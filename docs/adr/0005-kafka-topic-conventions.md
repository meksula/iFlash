# 0005. Kafka topic conventions: keys, retention and replay

- Status: Accepted
- Date: 2026-11-25

## Context

With Kafka ([0003](0003-kafka-as-the-only-message-broker.md)) several choices have to be made the same
way in every service: what goes into the message key, how long data is kept, and where a new or
restarted consumer starts reading. Some services (`rates-service`, `matching-engine-service`) keep
their state only in memory and must rebuild it from Kafka on every start.

## Decision

**Message keys carry the ordering domain.** The key decides the partition and therefore which events
are ordered relative to each other. For market data the key is the ticker (`quotation.changed`). A
Kafka key is not a RabbitMQ routing key; it is not used to select consumers.

**Retention is explicit.** Every topic is declared in the owning (producing) service as a `NewTopic`
bean built with `TopicBuilder`, with its retention or cleanup policy stated in code. Current topics:

| Topic | Producer | Key | Policy |
|---|---|---|---|
| `instrument.initialized` | `instrument-service` | see [0006](0006-compacted-topics-for-reference-data.md) | `cleanup.policy=compact`, `min.compaction.lag.ms=0` |
| `quotation.changed` | `matching-engine-service` | ticker | `retention.ms` = 7 days |

**Consumers can rebuild their state.** Consumers use `auto.offset.reset: earliest`, so a service that
joins later reads the retained history. Services whose state is held only in memory additionally seek
to the beginning on partition assignment (`ConsumerSeekAware.onPartitionsAssigned` →
`seekToBeginning`), so the state is rebuilt after every restart.

## Consequences

- Topic configuration is reviewed together with the producing code and is reproducible in any
  environment.
- In-memory read models are cheap to build but their startup time grows with the retained volume;
  retention therefore bounds both history and startup time.
- Ordering is only guaranteed per key. Consumers must not rely on order across tickers.
- Topics are currently created with one partition and replication factor 1, which is enough locally
  but has to be revisited for a multi-broker cluster.
