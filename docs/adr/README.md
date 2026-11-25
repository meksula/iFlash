# Architecture Decision Records

Significant architecture decisions of iFlash. How ADRs are written and maintained is described in
[0001](0001-record-architecture-decisions.md); new records start from [template.md](template.md).

ADRs take precedence over `docs/context/architecture-decisions.md`.

| ADR | Title | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions as ADRs in the repository | Accepted |
| [0002](0002-monorepo-with-root-aggregator-pom.md) | Monorepo with a root aggregator POM | Accepted |
| [0003](0003-kafka-as-the-only-message-broker.md) | Kafka as the only message broker | Accepted |
| [0004](0004-event-timestamp-convention.md) | Timestamp convention in events | Proposed |
| [0005](0005-kafka-topic-conventions.md) | Kafka topic conventions: keys, retention and replay | Accepted |
| [0006](0006-compacted-topics-for-reference-data.md) | Compacted topics for reference data | Accepted |
| [0007](0007-service-naming-and-boundaries.md) | Service naming and boundaries | Accepted |
| [0008](0008-cqrs-projections-instead-of-synchronous-reads.md) | CQRS projections instead of synchronous calls at read time | Accepted |
| [0009](0009-matching-engine-service-owns-the-engine.md) | matching-engine-service owns the engine and the order book | Accepted |
| [0010](0010-kubernetes-native-service-discovery.md) | Kubernetes-native service discovery and gateway routing | Accepted |
| [0011](0011-tls-termination.md) | TLS termination | Accepted |
| [0012](0012-persistence-conventions.md) | Persistence conventions | Accepted |
| [0013](0013-matching-and-price-rules.md) | Order matching and price display rules | Accepted |
| [0014](0014-container-images-with-jib.md) | Container images with Jib, local Kubernetes loop with Skaffold | Accepted |
