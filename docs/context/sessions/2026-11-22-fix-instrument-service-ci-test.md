# 2026-11-22: Fix instrument-service test in CI

## Outcome

- The first GitHub Actions build (`Java CI with Maven`, `mvn -B package`) failed in `instrument-service`:
  `contextLoads` started the full application with JPA but no datasource configured.
- `InstrumentServiceApplicationTests` now imports `TestcontainersConfiguration` with PostgreSQL
  (`postgres:17-alpine`, `@ServiceConnection`) and Kafka (`apache/kafka:4.1.0`). Schema is created by
  `spring.jpa.hibernate.ddl-auto=create-drop`, set on the test only.
- Kafka is wired through `spring.kafka.bootstrap-servers` (`DynamicPropertyRegistrar`), not
  `@ServiceConnection`, because `KafkaConfig` builds its own `ProducerFactory` from `KafkaProperties`,
  which ignores service connections.
- `helpers/iflash-platform` pins `testcontainers.version` to 1.21.4: 1.21.3 from Spring Boot 3.5.6
  cannot talk to Docker 29.
- Full `mvn -B package` passes locally for all modules.

## Open points

- `instrument-service` has no schema management for real environments: no `spring.datasource`, no
  migrations, no `ddl-auto`. Needs a decision (e.g. Flyway).
- `KafkaConfig` ignores `KafkaConnectionDetails`, so Docker Compose and Testcontainers service
  connections do not reach the producer.
