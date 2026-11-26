# 2026-11-24: Split iflash-platform monolith into microservices

## Decisions (made by the owner in this session)

- `iflash-engine` stays untouched and keeps order book and quotations in one in-memory object, so
  **matching-engine-service owns the engine** (single instance, in-memory, as before).
- The order book is not split out: `GET /api/v1/orderbook/{ticker}` is served by
  matching-engine-service. `order-book-service` stays an empty skeleton.
- trading-service → matching-engine-service: **synchronous HTTP** (`POST /internal/api/v1/order`),
  because the public response must contain the executed transactions.
- matching-engine-service consumes only `instrument.initialized` from Kafka (engine initialisation).
- **matching-engine-service publishes quotations** (`quotation.changed`, key = ticker, value
  `{ticker, timestamp (epoch millis), price}`); rates-service keeps its own read model from it.
- **API change:** `GET /api/v1/instrument` returns `[{ticker, initialPrice}]` (reference data from the
  CSV/DB) instead of `{ticker, currentPrice}`. Clients now read current prices from
  `GET /api/v1/quotation/{ticker}/price`.

## Outcome

| Service | Port | What it does now |
|---|---|---|
| gateway-service | 8080 | Routes; default profile uses DNS names with ports, `local` profile uses `localhost` |
| rates-service | 8081 | `/api/v1/quotation/{ticker}/price`, `/quotes` from an in-memory read model fed by `instrument.initialized` + `quotation.changed` |
| trading-service | 8083 | `/api/v1/trade/order`, forwards to matching-engine-service, passes its error status and body through (406 `{message}`) |
| instrument-service | 8084 | `/api/v1/instrument` from PostgreSQL |
| matching-engine-service | 8086 | Hosts `iflash-engine`; `/api/v1/orderbook/{ticker}`, `/internal/api/v1/order` |

- Quotation publishing: the engine computes quotations in `CompletableFuture.runAsync` (common
  ForkJoinPool). A decorator (`QuotationPublishingAggregator`) wraps the engine's aggregator and
  publishes after each post-transaction quotation. Sending from the common-pool thread failed in the
  fat jar (`ProducerConfig` static init loads classes through the thread context class loader, which
  is the app class loader there), so publishing goes through a single-thread executor with the
  correct context class loader (also keeps event order).
- Both Kafka consumers seek to the beginning on partition assignment, because their state is in
  memory and must be rebuilt on every start. rates-service initialisation does not depend on the
  order in which the two topics are replayed.
- instrument-service: datasource `localhost:5432/iflash` (`iflash`/`iflash`),
  `ddl-auto: update`, and the CSV loader skips tickers already stored (a restart failed on the unique
  constraint before).
- broker-platform and trading-simulator point to the gateway (`http://localhost:8080`); their
  instrument lists are enriched with the current price from rates-service.
- Testcontainers (Kafka) in context tests of matching-engine-service and rates-service.
- Verified: `mvn -B package` passes for all modules; all endpoints checked through the gateway,
  trading-simulator traded for 25 s without errors, rates-service rebuilt its state after a restart.

## Local infrastructure

```bash
docker run -d --name iflash-postgres --restart unless-stopped -e POSTGRES_DB=iflash -e POSTGRES_USER=iflash -e POSTGRES_PASSWORD=iflash -p 5432:5432 -v iflash-postgres-data:/var/lib/postgresql/data postgres:17
```

```bash
docker run -d --name iflash-kafka --restart unless-stopped -p 9092:9092 \
  -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
  -e KAFKA_LOG_DIRS=/var/lib/kafka/data -v iflash-kafka-data:/var/lib/kafka/data apache/kafka:4.1.0
```

## New simulator traders

- `OrderBookImbalanceTrader`: compares the volume of the top 10 BID and ASK levels; buys when the bid
  side clearly dominates (imbalance > 0.4) and sells its position when the ask side does. The only
  bot that reads the order book.
- `MomentumRotationTrader`: ranks all tickers by return over the last 30 quotes, holds the top 3 with
  a positive return and sells whatever drops out of the top.
- `PairsTrader` (KO/PEP, XOM/CVX): z-score of the price ratio over 60 quotes. Above +2 or below -2
  it moves into the cheaper leg; when |z| < 0.5 it closes both. Long only, because the bots track
  their own holdings.
- `InvestmentFundTrader` ("Fund-BigFish", 50 000 000 USD): wakes up every 5 minutes. The first
  wake-up records each ticker's price as its fair value. Later wake-ups sell holdings at least 5%
  above fair value and buy up to 5 tickers at least 5% below it, in blocks worth up to 1 000 000 USD
  and at most 500 shares. Its blocks are bigger than the available liquidity, so the engine turns
  the unfilled part into a resting LIMIT order that the fund's account does not track.
- Shared `TradingAccount` keeps cash and holdings and books the volume actually filled (from the
  `transactions` list). `TechnicalTrader` books the requested volume instead; left as is.
- All three ran against the live stack; the pairs logic was also checked with a fake `ApiToolkit`.

## Open points

- `docs/context/project-overview.md` is outdated: order book route (now matching-engine-service,
  8086), instrument API (`initialPrice`), `/api/v1/instrument` no longer returns current prices.
- `helpers/iflash-platform` was removed (directory and root `pom.xml` module); broker-platform
  README and "engine unreachable" messages now point to gateway-service on 8080.
  `project-overview.md` still describes it.
- `compose.yaml` was not updated (no Kafka, Postgres on 5433 with a different password, services
  default to `localhost` for Kafka and the database).
- `instrument.initialized` is still keyed by a random UUID (compaction does not deduplicate); every
  instrument-service start emits a new event. Consumers are idempotent, so this is harmless for now.
- Schema management for instrument-service is `ddl-auto: update` until Flyway (or similar) is chosen.
- matching-engine-service state is in memory: after its restart the engine starts from initial
  prices while rates-service keeps the replayed history.

## Follow-up 2026-11-25

- **Order book priority (iflash-engine).** Both `PriorityBlockingQueue`s used `Order`'s natural
  ascending price order, so the head of the bid queue was the *lowest* bid. Every sell hit the worst
  bid, higher bids never matched (crossed book), and the market drifted down (52 of 55 tickers fell
  in 20 minutes of play). Queues now use `Order.ASK_PRIORITY` (price ascending) and
  `Order.BID_PRIORITY` (price descending), with a registration sequence as the time tie-breaker
  (price-time priority). `Order`'s natural order (used by snapshots) is unchanged.
- **Lost orders (iflash-engine).** `LimitOrderProcessor` polled the next resting order even after the
  requested volume was filled, and that order was dropped. It now polls only while volume is left.
- Tests: three new tests in `SimpleOrderBookTest`. Two expectations in `LimitOrderProcessorTest`
  encoded the old random heap order and are updated to price-time results.
- **rates-service:** the replayed initial quote got a "now" timestamp and went to the front of the
  history, so the broker chart got unordered data and lightweight-charts rendered nothing. The
  initial quote now uses the `instrument.initialized` event timestamp and every quote is inserted in
  time order.
- Ports: broker-management-service 8085 (it took 8080 by default), settlement-service 8087.
- Local DB `iflash_broker_platform` (owner `iflash`) created for broker-platform's `local` profile.

Known, not fixed: `MarketOrderProcessor` compares a resting order's volume with the requested volume
instead of the remaining volume, so a market order can be over-filled; `SimpleOrderBook.getTopOrders`
iterates the queue unsorted and takes `subList(depth, size)` (the tail); broker-platform asks for both
book sides with `orderBy=ASC`, so its UI lists bids from the lowest.
