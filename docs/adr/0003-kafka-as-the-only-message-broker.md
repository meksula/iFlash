# 0003. Kafka as the only message broker

- Status: Accepted
- Date: 2026-11-25

## Context

Services integrate through events (instrument catalogue, quotations, and in the future trades,
settlement and portfolio events). One option is to pick a broker per use case, for example RabbitMQ
for work queues and Kafka for streams.

## Decision

We use Apache Kafka (KRaft mode, no ZooKeeper) as the only message broker.

- Services use `spring-boot-starter-kafka`, not bare `spring-kafka`.
- On Spring Boot 4, `KafkaProperties` is imported from `org.springframework.boot.kafka.autoconfigure`,
  and `JacksonJsonSerializer` is used instead of the deprecated `JsonSerializer`.

## Consequences

- One broker to run, monitor and learn instead of several.
- Kafka's retained log lets services that do not exist yet consume historical events later, which a
  queue model does not offer. This is what makes the read-model rebuilds in [0005](0005-kafka-topic-conventions.md)
  and the projections in [0008](0008-cqrs-projections-instead-of-synchronous-reads.md) possible.
- Patterns that a queue broker gives out of the box (per-message routing, delayed delivery, per-message
  acknowledgement) have to be modelled with topics, keys and consumer groups.
