# 0006. Compacted topics for reference data

- Status: Accepted
- Date: 2026-11-25

## Context

Reference data such as the instrument catalogue changes rarely, but every consumer needs the complete
current set, including consumers that start long after the data was published. With time-based
retention the data would either expire or the consumer would have to replay every historical mutation.

## Decision

Reference data is published to compacted topics (`cleanup.policy=compact`), keyed by the business
identifier of the entity (for instruments: the ticker). A consumer reading from `earliest` gets the
latest value per key, which is the current catalogue. `min.compaction.lag.ms=0`, because older versions
of a key have no value.

## Consequences

- New services get the current catalogue without calling `instrument-service` synchronously.
- Deleting an entity requires publishing a tombstone (null value) for its key.
- **Implementation gap:** `instrument.initialized` is already compacted, but `instrument-service`
  publishes the whole catalogue as a single `FinancialInstrumentInitializedEvent` under a random UUID
  key (`routingKey`). Every start therefore adds a new key and compaction never removes old catalogues.
  To follow this decision the topic should carry one message per instrument keyed by ticker.
