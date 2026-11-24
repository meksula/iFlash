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
