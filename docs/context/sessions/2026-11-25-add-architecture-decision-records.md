# 2026-11-25: Add architecture decision records

## What was done

Turned the decisions collected in `docs/context/architecture-decisions.md` into formal ADRs in
`docs/adr/` (Nygard format, index in `docs/adr/README.md`, template in `docs/adr/template.md`).
Each ADR was checked against the current code; where the code does not follow the decision yet, the
ADR says so under Consequences.

| ADR | Topic | Status |
|---|---|---|
| ADR-0001 | ADRs in the repository, docs as code (Wiki rejected) | Accepted |
| ADR-0002 | Monorepo with root aggregator POM | Accepted |
| ADR-0003 | Kafka as the only broker, Spring Boot 4 specifics | Accepted |
| ADR-0004 | Event timestamp convention (epoch millis vs `Instant`) | Proposed, owner to decide |
| ADR-0005 | Topic keys, explicit retention, replay from earliest | Accepted |
| ADR-0006 | Compacted topics for reference data | Accepted |
| ADR-0007 | `-service` naming, instrument vs rates, standalone vs overlays | Accepted |
| ADR-0008 | CQRS projections instead of synchronous reads | Accepted |
| ADR-0009 | matching-engine-service owns the engine, sync HTTP from trading | Accepted |
| ADR-0010 | Kubernetes-native discovery, gateway routing | Accepted |
| ADR-0011 | TLS termination (NLB pass-through + Envoy) | Accepted |
| ADR-0012 | Persistence conventions, one-off initialisation | Accepted |
| ADR-0013 | Matching and price display rules | Accepted |
| ADR-0014 | Jib images, Skaffold planned | Accepted |

## Gaps between decisions and code

- `instrument.initialized` is compacted but receives the whole catalogue as one message under a random
  UUID key, so compaction never removes old catalogues (ADR-0006).
- Events mix `long` epoch millis (`QuotationChangedEvent`) and `Instant`
  (`FinancialInstrumentInitializedEvent`) (ADR-0004).
- The instrument CSV loader skips stored tickers instead of using a checksum state table, row lock and
  upsert (ADR-0012).

## Outdated context

- `docs/context/AGENTS-README.md` still says ADRs will live in `docs/adr/` "once introduced"; they now
  exist. `docs/context/` was not edited.
