# Architecture Decision Records

Significant architecture decisions of iFlash. How ADRs are written and maintained is described in
[ADR-0001](ADR-0001-record-architecture-decisions.md); new records start from [template.md](template.md).

ADRs take precedence over `docs/context/architecture-decisions.md`.

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](ADR-0001-record-architecture-decisions.md) | Record architecture decisions as ADRs in the repository | Accepted |
| [ADR-0002](ADR-0002-monorepo-with-root-aggregator-pom.md) | Monorepo with a root aggregator POM | Accepted |
| [ADR-0003](ADR-0003-kafka-as-the-only-message-broker.md) | Kafka as the only message broker | Accepted |
| [ADR-0004](ADR-0004-event-timestamp-convention.md) | Timestamp convention in events | Proposed |
| [ADR-0005](ADR-0005-kafka-topic-conventions.md) | Kafka topic conventions: keys, retention and replay | Accepted |
| [ADR-0006](ADR-0006-compacted-topics-for-reference-data.md) | Compacted topics for reference data | Accepted |
| [ADR-0007](ADR-0007-service-naming-and-boundaries.md) | Service naming and boundaries | Accepted |
| [ADR-0008](ADR-0008-cqrs-projections-instead-of-synchronous-reads.md) | CQRS projections instead of synchronous calls at read time | Accepted |
| [ADR-0009](ADR-0009-matching-engine-service-owns-the-engine.md) | matching-engine-service owns the engine and the order book | Accepted |
| [ADR-0010](ADR-0010-kubernetes-native-service-discovery.md) | Kubernetes-native service discovery and gateway routing | Accepted |
| [ADR-0011](ADR-0011-tls-termination.md) | TLS termination | Accepted |
| [ADR-0012](ADR-0012-persistence-conventions.md) | Persistence conventions | Accepted |
| [ADR-0013](ADR-0013-matching-and-price-rules.md) | Order matching and price display rules | Accepted |
| [ADR-0014](ADR-0014-container-images-with-jib.md) | Container images with Jib, local Kubernetes loop with Skaffold | Accepted |
