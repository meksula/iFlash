# ADR-0004. Timestamp convention in events

- Status: Proposed
- Date: 2026-11-25

## Context

`ZonedDateTime` is ruled out in events: the zone is presentation data and it makes serialised payloads
ambiguous across services. Two representations remain acceptable:

- epoch milliseconds as `long`, consistent with the public REST API (`quoteTimestamp`);
- `java.time.Instant`, serialised by Jackson as an ISO-8601 UTC string.

The code currently uses both:

- `QuotationChangedEvent` (`matching-engine-service` → `quotation.changed`) has `long timestamp`;
- `FinancialInstrumentInitializedEvent` (`instrument-service` → `instrument.initialized`) has
  `Instant timestamp`.

## Decision

Not decided yet. Events use either epoch millis or `Instant`, never `ZonedDateTime`. The owner chooses
one convention for all events; this ADR is then accepted with that choice and the non-conforming event
is migrated.

## Consequences

- Until the decision is made, consumers have to know per topic which representation is used.
- Changing the representation of an existing event is a breaking change for its consumers and needs an
  event version bump.
