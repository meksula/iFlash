# ADR-0012. Persistence conventions

- Status: Accepted
- Date: 2026-11-25

## Context

Several services will persist time values, money and prices in PostgreSQL. Inconsistent types cause
time zone bugs, rounding differences and false negatives in comparisons. Services may run as several
replicas that start at the same time.

## Decision

- **Time:** `Instant` in Java, `TIMESTAMP WITH TIME ZONE` in PostgreSQL.
- **Money and prices:** `BigDecimal` mapped with `@Column(precision = 19, scale = 4)`, normalised with
  `RoundingMode.HALF_UP` before persisting.
- **Concurrency:** `@Version` provides optimistic locking for UPDATEs of existing rows. Duplicate
  INSERTs are prevented by unique constraints, not by `@Version`.
- **One-off data initialisation** (for example the instrument catalogue): gate it with a single-row
  state table that holds a checksum of the source file; lock that row with `PESSIMISTIC_WRITE` so that
  replicas starting at the same time serialise; and still write every record with an upsert
  (`ON CONFLICT DO UPDATE` / `DO NOTHING`) as a safety net.

## Consequences

- Prices compare and round the same way in every service (see also [ADR-0013](ADR-0013-matching-and-price-rules.md)).
- Initialisation is idempotent and safe with several replicas, and re-runs only when the source file
  changes.
- **Implementation gap:** `instrument-service` follows the type conventions
  (`FinancialInstrumentEntity`), but its CSV loader currently only skips tickers that are already
  stored; the checksum table, row lock and upsert are not implemented yet.
