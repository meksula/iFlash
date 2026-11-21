# Architecture decisions

Decisions reached in design sessions, with the reasoning. Each entry is a candidate for a formal ADR in
`docs/adr/`.

## Messaging

**Kafka is the only message broker.** Choosing a broker per event (RabbitMQ here, Kafka there) multiplies
operational complexity. Kafka's retained log also lets services that do not exist yet consume historical
events later, which a queue model does not.

**Message keys carry the ordering domain.** The key (for example the ticker) decides the partition and
therefore the ordering guarantee. This is not the same thing as a RabbitMQ routing key.

**Retention is explicit.** Topics are declared as `NewTopic` beans with explicit retention, and consumers
use `auto.offset.reset: earliest` so a late service can rebuild its state.

**Reference data goes to compacted topics.** Instrument definitions use `cleanup.policy=compact`, keyed
by ticker, so a new consumer reading from `earliest` gets the current catalogue (latest value per key)
rather than every historical mutation. `min.compaction.lag.ms=0` because there is no reason to keep old
versions of a key around.

**Event timestamps.** `ZonedDateTime` is not used in events. `TradeExecutedEvent` carries epoch millis
(`long`), consistent with the REST API. Note that `FinancialInstrumentInitializedEvent` uses `Instant`;
`Instant` is acceptable, `ZonedDateTime` is not. Pick one convention and record it in an ADR.

**Spring Boot 4 specifics.** Use `spring-boot-starter-kafka` (not bare `spring-kafka`); `KafkaProperties`
moved to `org.springframework.boot.kafka.autoconfigure`; `JsonSerializer` is deprecated in favour of
`JacksonJsonSerializer`.

## Service boundaries

**Naming: `-service` suffix, not `-api`.** A service owns a business capability, not only an HTTP layer.

**`instrument-service` is separate from `rates-service`.** Reference data (instruments) and market data
(prices) change at very different rates and belong to different bounded contexts.

**CQRS projections instead of synchronous calls at read time.** A service that needs another context's
data for its queries keeps a local read model fed by events (e.g. `TradeExecuted` → `PriceChanged` →
read model in `instrument-service`), rather than calling the owning service during the request.

**Standalone services vs overlays.** Independently deployed containers (Kafka, Keycloak, Prometheus…)
are "standalone"; libraries and patterns that run inside existing services (resilience, tracing
instrumentation, outbox) are "overlays". Infrastructure planning keeps these lists separate.

## Gateway and networking

**Kubernetes-native service discovery, no Eureka.** Gateway routes use plain DNS names such as
`http://rates-service:8081`, resolved by CoreDNS. The same configuration works in minikube and EKS.
Preference in general: Kubernetes-native mechanisms over Spring-specific abstractions where they match
what production uses.

**Gateway configuration** uses the `spring.cloud.gateway.server.webflux` structure; URIs need the
`http://` scheme; multiple paths per predicate and URI variables (`{ticker}`) are allowed; predicates
(matching) are combined with AND, filters do the modification.

**TLS termination.** In production, an ALB terminating TLS with plain HTTP inside the VPC is the
standard and acceptable default. For learning, the chosen path is NLB TCP pass-through → Envoy
terminates TLS, with cert-manager and Let's Encrypt over SDS. AWS App Mesh is ruled out (being retired).

## Persistence

- Time columns: `Instant` in Java, `TIMESTAMP WITH TIME ZONE` in PostgreSQL.
- Money and prices: `BigDecimal`, `@Column(precision = 19, scale = 4)`, normalised with
  `RoundingMode.HALF_UP` before persisting.
- `@Version` gives optimistic locking on UPDATE of an existing row. It does not protect against
  duplicate INSERTs; that is the job of unique constraints.
- One-off data initialisation (e.g. the instrument catalogue): gate it with a single-row state table
  holding a checksum of the source file, lock that row with `PESSIMISTIC_WRITE` so concurrent replicas
  starting at the same time serialise, and still write each record with an upsert
  (`ON CONFLICT DO UPDATE/NOTHING`) as a safety net.

## Matching and pricing

- LIMIT orders match when `bid.price >= ask.price`.
- `BigDecimal.compareTo()` for price comparison; `equals()` is scale-sensitive.
- Price shown to users: last traded price; execution uses best bid/ask; mid price as fallback.

## Build, deployment, documentation

- **Monorepo** with a root aggregator POM, instead of one repository per service.
- **Jib** builds images without a Dockerfile or Docker daemon; it also integrates with Skaffold's `jib`
  builder.
- **Skaffold** (planned) for the local Kubernetes loop, one root `skaffold.yaml`.
- **Docs as code.** GitHub Wiki was rejected (no PR review, no versioning with the code, poor fit for
  ADRs). Cross-cutting documentation (C4 diagrams, Kafka topic and naming conventions, ADRs) lives in
  `..`, optionally published with MkDocs Material or Docusaurus on GitHub Pages.
- **Agent knowledge exchange** through `` (see its README).

## Settlement domain background

Trading → clearing (CCP: novation, netting) → settlement (CSD: delivery versus payment, T+1/T+2).
Relevant message standards: FIX for trading, ISO 15022 (SWIFT MT540–MT548) and its successor ISO 20022
for settlement instructions. ISO 8583 is a card payments standard and does not apply. A simplified
clearing module with netting and MT54x-style instructions is a possible learning exercise for
`settlement-service`.
