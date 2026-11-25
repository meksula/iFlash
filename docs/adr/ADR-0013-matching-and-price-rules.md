# ADR-0013. Order matching and price display rules

- Status: Accepted
- Date: 2026-11-25

## Context

Several services and clients (engine, rates, broker platform, the planned `portfolio-service`) need to
agree on when orders match, at what price, and which price is shown to users.

## Decision

- Orders are `BID` (buy) or `ASK` (sell), of type `MARKET` (immediate) or `LIMIT`.
- A LIMIT bid and ask match when `bid.price >= ask.price`, equal prices included. The execution price
  follows maker/taker rules: the resting (maker) order's price is used.
- Partial fills are allowed: one order can produce several transactions at different prices.
- Prices are compared with `BigDecimal.compareTo()`, never `equals()`, which is scale-sensitive.
- **Displayed price:** the last traded price is used for tickers and portfolio valuation; best bid/ask
  is used for order execution; the mid price is a fallback when nothing has traded yet.

## Consequences

- `portfolio-service` and the broker platform value positions with the last traded price from
  `rates-service`.
- Price-time priority in the order book is on the roadmap and will refine, not replace, these rules.
