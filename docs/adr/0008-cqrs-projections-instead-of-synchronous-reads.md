# 0008. CQRS projections instead of synchronous calls at read time

- Status: Accepted
- Date: 2026-11-25

## Context

Queries of one service often need data owned by another bounded context (an instrument list with
prices, portfolio valuation with current prices). Calling the owning service during the request couples
availability and latency of both services and multiplies load on the owner.

## Decision

A service that needs another context's data for its queries keeps a local read model fed by events
from Kafka, rather than calling the owning service while handling the request.

Current example: `rates-service` builds its in-memory quotation store from `instrument.initialized`
and `quotation.changed`. Planned examples: `TradeExecuted` → `PriceChanged` → read model in
`instrument-service`, and the same pattern for portfolio P&L.

## Consequences

- Read endpoints stay available when the owning service is down, and their latency does not depend on
  it.
- Read models are eventually consistent; a client may briefly see stale data.
- Every projection needs a way to be rebuilt (see replay rules in [0005](0005-kafka-topic-conventions.md)).
- Commands that need an immediate answer from the owner may still use synchronous calls
  ([0009](0009-matching-engine-service-owns-the-engine.md)).
