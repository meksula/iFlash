# Marlin — broker platform

A broker simulator (PoC, fake money) on top of the iFlash matching engine. It adds everything the
engine has no concept of: users, account balance, a simulated payment gateway, favorites, positions
and trade history. Server-rendered with Thymeleaf + vanilla JS — no Node build, just Maven.

> Brand only; the Maven artifact stays `broker-platform`.

## Stack
Spring Boot 4.1 · Java 21 · Thymeleaf · Spring Data JPA · PostgreSQL · TradingView lightweight-charts
(vendored). Live data via API polling (WebSocket can replace it later).

## Prerequisites
1. **JDK 21**.
2. **PostgreSQL** running locally. Create an empty database:
   ```sql
   CREATE DATABASE marlin;
   ```
3. **The iFlash engine** (`iflash-platform`) running on port **10023** — that is the market data /
   trading API this app consumes. For trades to actually fill you also need liquidity in the book,
   so optionally run `trading-simulator` / `traders`.

## Configuration
Defaults live in `src/main/resources/application.yaml`; override via env vars:

| Setting | Env var | Default |
|---|---|---|
| DB URL | `MARLIN_DB_URL` | `jdbc:postgresql://localhost:5432/marlin` |
| DB user | `MARLIN_DB_USER` | `postgres` |
| DB password | `MARLIN_DB_PASSWORD` | `postgres` |
| Engine base URL | `IFLASH_API_BASE_URL` | `http://localhost:10023` |

Schema is created automatically (`spring.jpa.hibernate.ddl-auto=update`).

## Run
```bash
mvn -f broker-platform/pom.xml spring-boot:run
```
Open http://localhost:8090 and sign in with any email — the account is created on first login with a
$0.00 balance. Top up via Wallet, browse Instruments, favorite some, open one to chart it and place
orders.

## Tests
```bash
mvn -f broker-platform/pom.xml test
```
Settlement rules are unit-tested with Mockito (`TradingServiceTest`); the context-load test runs on
in-memory H2, so tests need neither PostgreSQL nor the engine.

## Known engine gaps (not implemented here on purpose)
- No per-user order identity / no order ids → resting LIMIT remainders can't be listed or cancelled.
- No cancel-order endpoint.
- No cost-quote/dry-run → the buy funds check is an estimate for MARKET orders (the engine fills
  immediately); settlement always uses the actual returned fills.
- Order types that need extra params (STOP price, GTD expiry, ICEBERG) can't carry them — the engine's
  request only accepts `direction, type, price, volume`.
