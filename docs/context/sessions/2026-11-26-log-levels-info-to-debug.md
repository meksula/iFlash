# 2026-11-26: Move per-order INFO logs to DEBUG

## What was done

Implemented issue #28. INFO was used for per-order details and debug dumps, including inside the
matching loop, which made logs noisy and may distort Gatling results. Only the log level changed;
message texts are the same.

- `iflash-engine`: all 11 `log.info` calls are now `log.debug` (`MatchingEngineFactory`,
  `SimpleOrderBook`, `MarketOrderProcessor`, `OrderUtils`, `Order`). The engine library no longer
  logs at INFO; the hosting service decides what to log.
- matching-engine-service `OrderController` and trading-service `TradeController`: the per-request
  "Order registration ..." logs are now DEBUG.
- Unchanged: service lifecycle INFO logs (`MatchingEngineConfiguration`, both
  `InstrumentInitializedListener`s, `FinancialInstrumentDataLoader`), WARN/ERROR logs,
  `helpers/trading-simulator`, logback and application config.

## Verification

- `grep -rn "log.info" iflash-engine/src/main` returns nothing.
- `mvn -B -pl iflash-engine,matching-engine-service,trading-service -am verify`: BUILD SUCCESS, no
  tests skipped.
