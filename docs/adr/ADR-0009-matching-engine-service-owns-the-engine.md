# ADR-0009. matching-engine-service owns the engine and the order book

- Status: Accepted
- Date: 2026-11-25

## Context

`iflash-engine` is a plain Java library that keeps the order book and quotation aggregation in one
in-memory object (`SingleThreadMatchingEngine`). The target architecture lists a separate
`order-book-service` and a `trading-service`. The public order placement response must contain the
executed transactions.

## Decision

- `iflash-engine` stays unchanged. `matching-engine-service` hosts it as a single in-memory instance
  and is the only owner of order books and matching.
- `GET /api/v1/orderbook/{ticker}` is served by `matching-engine-service`. `order-book-service`
  remains an empty skeleton.
- `trading-service` calls `matching-engine-service` synchronously over HTTP
  (`POST /internal/api/v1/order`) and passes the engine's result or error (406 `{message}`) through,
  because the response must contain the transactions.
- `matching-engine-service` initialises tickers from `instrument.initialized` and publishes every
  post-transaction quotation to `quotation.changed`. Publishing runs on a dedicated single-thread
  executor, which keeps event order and gives Kafka the correct context class loader.

## Consequences

- Matching stays as fast and simple as in the monolith; no distributed coordination inside a trade.
- `matching-engine-service` is a single instance and a single point of failure; its state is lost on
  restart (open orders are not persisted).
- The synchronous call couples order placement availability to `matching-engine-service`.
- Scaling out (for example partitioning by ticker) or extracting the order book needs a new ADR.
