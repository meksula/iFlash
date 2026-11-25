# 0007. Service naming and boundaries

- Status: Accepted
- Date: 2026-11-25

## Context

The `iflash-platform` monolith is being split into microservices (issue #18). Service names and
boundaries have to reflect business capabilities, and some data that looks related (an instrument and
its price) behaves very differently.

## Decision

- Services are named with the `-service` suffix, not `-api`: a service owns a business capability,
  not only an HTTP layer.
- `instrument-service` (reference data: the list of tradable instruments) is separate from
  `rates-service` (market data: current price and quotation history). They change at very different
  rates and belong to different bounded contexts. `GET /api/v1/instrument` returns reference data
  (`ticker`, `initialPrice`); current prices come from `GET /api/v1/quotation/{ticker}/price`.
- Infrastructure is planned in two separate lists: **standalone** services that are deployed as their
  own containers (Kafka, Keycloak, Prometheus, ...) and **overlays** that run inside existing services
  (resilience, tracing instrumentation, outbox).

## Consequences

- Clients that need an instrument with its current price combine two calls or keep a projection
  ([0008](0008-cqrs-projections-instead-of-synchronous-reads.md)).
- High-frequency price updates do not touch the reference data store.
