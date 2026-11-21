# Project overview

## Purpose

iFlash is a stock exchange simulator built for personal use: to test investment strategies against a
simulated market and to learn software engineering, especially distributed systems and cloud-native
patterns.

It is **intentionally over-engineered**. Success is measured by exposure to real-world architectural
patterns (event-driven integration, CQRS, sagas, outbox, Kubernetes-native infrastructure, observability),
not by production fitness. Added complexity is the point, not a cost.

A second part of the system, the **broker platform**, simulates a broker that is a client of the
exchange: users, balances, positions and trade history on top of the exchange's trading API.

## Repository layout

Single Maven **monorepo** (`com.iflash:iflash`, Java 25). Git submodules were used earlier and dropped
to avoid juggling many repositories in a private project.

The repository currently holds two generations of the system side by side.

### Legacy core (working end to end)

| Module | Role | Port |
|---|---|---|
| `iflash-engine` | Plain Java library: matching engine (`SingleThreadMatchingEngine`), order book, MARKET and LIMIT order processors, quotation aggregation | n/a |
| `../../helpers/iflash-platform` | Spring Boot app that wraps `iflash-engine` and exposes the public REST API described in the root `../../README.md` (instruments, order book, quotations, place order) | 10023 |
| `../../helpers/trading-simulator` | Bots that trade against the platform to create liquidity and price movement (random noise, market maker, trend following, mean reversion, breakout and others) | n/a |
| `../../helpers/broker-platform` | "iFlash Broker Platform": broker simulator with users, wallet, simulated payment gateway, favourites, positions. Thymeleaf UI plus an Angular 21 SPA (`broker-platform-ui`, port 4200) over a JWT-secured JSON API. Consumes `iflash-platform` | 8090 |

### Target microservices (being extracted, mostly skeletons)

The monolithic logic is being split into single-responsibility services (issue #18). All are Spring Boot
4.1, WebFlux, container images built with **Jib** (`eclipse-temurin:25-jre` base).

| Service | Responsibility | State |
|---|---|---|
| `gateway-service` | Spring Cloud Gateway, single entry point, routes `/api/v1/**` to services by Kubernetes DNS name | Routing configured |
| `instrument-service` | Reference data: list of tradable instruments. Loads the initial catalogue from CSV, persists it (JPA, PostgreSQL) and publishes `FinancialInstrumentInitializedEvent` to the compacted Kafka topic `instrument.initialized` | Implemented |
| `rates-service` | Market data: current price and quotation history | Controller stub |
| `order-book-service` | Order book views | Skeleton |
| `trading-service` | Order placement (`/api/v1/trade/order`) | Skeleton |
| `matching-engine-service` | Order matching | Skeleton |
| `settlement-service` | Post-trade settlement (clearing and settlement simulation) | Skeleton |
| `broker-management-service` | Broker-facing management API (`/api/v1/broker`); the service being split up in #18 | Skeleton |

Gateway routes (`../../gateway-service/src/main/resources/application.yml`):

| Path | Service | Local port |
|---|---|---|
| `/api/v1/quotation/{ticker}/price`, `/api/v1/quotation/{ticker}/quotes` | rates-service | 8081 |
| `/api/v1/orderbook/{ticker}` | order-book-service | 8082 |
| `/api/v1/trade/order` | trading-service | 8083 |
| `/api/v1/instrument` | instrument-service | 8084 |
| `/api/v1/broker` | broker-management-service | 8085 |

Other directories: `..` (architecture diagram drafts, `iflash-arch.drawio`), `requests/` (IntelliJ
HTTP client requests), `skills/dev-SKILL.md` (the owner's Java coding conventions skill),
`compose.yaml` (local runtime: PostgreSQL plus all microservice images).

## Domain rules already settled

- Orders: `BID` (buy) and `ASK` (sell); types `MARKET` (immediate) and `LIMIT`.
- A LIMIT bid and ask match when `bid.price >= ask.price`, equal prices included. Execution price
  follows maker/taker rules: the resting (maker) order's price is used.
- Partial fills are in scope: one order can produce several transactions at different prices.
- Prices are `BigDecimal`, compared with `compareTo()`, never `equals()`.
- Displayed price: last traded price for tickers and portfolio valuation; best bid/ask for order
  execution; mid price as fallback when nothing has traded yet.

## Tech stack

- Java 25, Spring Boot 4.1.x, Spring WebFlux, Spring Cloud Gateway (`server.webflux` configuration)
- Apache Kafka 4.3.1 in KRaft mode (no ZooKeeper), Kafka UI; `spring-boot-starter-kafka`,
  `JacksonJsonSerializer`
- PostgreSQL (17 in `../../compose.yaml`), Redis planned
- Jib for images, Docker Compose locally, Kubernetes (minikube locally, EKS as target), Skaffold planned
- Testing: JUnit 5, Mockito, Testcontainers, Cucumber/Gherkin for acceptance tests
- Angular 21 for the broker platform SPA

## Working conventions

- English for code, commits and technical documents. Design discussions happen in Polish.
- Conventional Commits style for commit messages.
- Coding style is described in `../../skills/dev-SKILL.md`: rich (non-anemic) domain models, SOLID, design
  patterns without over-engineering at code level, modern Java features, no inline comments.
- Project-wide agent rules are in the root `../../CLAUDE.md`.
- The owner uses a Claude Code multi-agent pipeline (analyst → implementer → reviewer → tester). For now
  the owner writes the ticket, pushes commits and starts the next iteration after review. Agent
  definitions live in `.claude/`, which is git-ignored.

## Roadmap

Near term:
- Finish the split of the monolith into the services above.
- Kafka-based CQRS projections so read endpoints do not call other services synchronously at query
  time (e.g. `TradeExecuted` → `PriceChanged` → local read model in `instrument-service`; same pattern
  for portfolio P&L).
- `portfolio-service`; capture the price display rules above as an ADR first.
- Matching engine: price-time priority, partial fills, market maker bot.
- Local dev automation: root `skaffold.yaml` building all services with Jib; Cloud Code IntelliJ plugin.
- Full architecture diagram (C4).

Infrastructure to integrate as standalone services: Schema Registry, Prometheus, Grafana, Jaeger, Loki,
Keycloak, Vault, MinIO, ClickHouse, Elasticsearch.

Learning path for TLS: NLB TCP pass-through → Envoy terminating TLS, certificates from cert-manager and
Let's Encrypt delivered over SDS (SNI routing, rotation without restart, L4 vs L7).

Feature backlog for the simulator (not prioritised): order cancel/modify/status/history; STOP,
STOP-LIMIT, IOC, FOK, GTC orders; cash balance and fund locks; positions with average price and P&L;
trade tape; OHLC candles; Level 2 depth over WebSocket/SSE; trading hours and circuit breakers; admin
endpoints; API authentication, rate limiting and idempotent order creation; audit log. FIX protocol is
deferred.
