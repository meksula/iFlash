# 2026-11-26 — Broker platform dashboard redesign

## What changed

Angular SPA (`helpers/broker-platform/broker-platform-ui`):
- New look: black masthead, warm paper background, one signal orange, hairline rules instead of boxes,
  square corners, Helvetica with tabular figures. Done by changing the existing CSS tokens in
  `styles.scss`, so the other tabs inherit it.
- Navigation: Dashboard, Instruments, History (was Portfolio, now only trade history), News (empty
  placeholder). Wallet left the nav: it sits in the account menu (person icon, top right) with Log out.
- Masthead shows the account's total value and profit (green/red).
- Dashboard laid out per the wireframe: favourites table (sortable, Trade button opens an order
  dialog), candle chart with 1m/5m/15m/1h/1d intervals (lightweight-charts, aggregated client-side),
  and a compact portfolio table whose rows expand into individual buy lots. × liquidates the whole
  position or one lot at MARKET (browser confirm first).
- Prices as `33.34 USD` (`usd` pipe), dates as "2 hours ago" (`relativeTime` pipe). Names and icons
  are placeholders (`—`, generic banknote SVG) until instrument names arrive.

Backend (`helpers/broker-platform`):
- New `position_lot` table (`PositionLot`): one row per filled BID with average fill price. Sells
  release lots FIFO; closed lots are deleted. `PositionLotBackfill` rebuilds lots once from trade
  history for positions that have none.
- `POST /api/portfolio/{ticker}/liquidate` and `POST /api/portfolio/lots/{id}/liquidate`.
- `/api/portfolio` holdings now carry purchase price, change % and lots.
- `/api/auth/me` adds `totalValue` (cash + holdings at market) and `profit` (value − all top-ups).
- `/api/dashboard` favourites carry `dailyChangePercent`: current vs the last quote before 00:00 UTC,
  falling back to the oldest of the last 5000 quotes.
- `MarketQueryService` now asks rates-service for the latest quotes (`DESC`, reversed). Before, `ASC`
  with size 300 returned the oldest 300 quotes, so the chart did not show recent prices.

## Decisions (Karol, this session)
Lot table with backfill; profit = total value − deposits; stay on lightweight-charts with intervals;
daily change computed from quotes.

## Open / follow-ups
- Daily change and long intervals are approximate: rates-service has no time-range or OHLC query and
  the broker pulls 5000 quotes per favourite on every 5 s poll.
- `package-lock.json` is out of sync with `package.json` (`npm ci` fails); not touched.
- Login page and Instrument page keep some hardcoded dark-theme colours (login card shadow, chart).

## Follow-up (same day)
- Dashboard spans the full window width.
- Portfolio: header removed, new Value column (quantity × current price, also per lot), every column
  sortable (shared `TableSort` helper, also used by favourites). Lots stay under their position.
- Trade button is an orange exchange-arrows icon with a tooltip instead of a text label.
- Masthead shows Free cash next to Portfolio and Profit; the three figures now refresh on every
  dashboard poll.
- Node.js 22 LTS installed for the user in `~/.local` (no sudo); `npm start` runs the SPA on :4200.
- New branding from Karol's "iF" logo, redrawn as SVG in `broker-platform-ui/public`: `iflash-wordmark.svg`
  (white, masthead), `iflash-logo.svg` (full logo, login), `favicon.svg` (monogram);
  `apple-touch-icon.png` re-rendered from it. Old flame images (`ibp-logo.png`, `favicon.png`,
  `favicon.ico`) removed from the SPA; the Thymeleaf UI keeps its own copies in `static/img`.
- Account button in the masthead: no frame, round avatar plus menu icon, same hover/open states as the nav tabs.
- Portfolio rows only expand/collapse; a mini chart icon button before × switches the chart. Favourites order button replaced by B/S buttons (green/red) that open the order dialog preset to Buy or Sell.
- Favourites: green cart (buy) and red price tag (sell) icons. Order dialog: green Buy / red Sell toggle and submit, Market/Limit toggle instead of a dropdown, volume starts at 1. `OrderCatalog.ORDER_TYPES` trimmed to MARKET and LIMIT (other types not supported for now).
- Filled orders close the order dialog and show a green toast top-right for 2 s; errors (e.g. insufficient funds) stay in the dialog; unfilled orders show a warning in the dialog.
- Order dialog also closes on a click outside it (backdrop) and on Escape.
- Liquidation (×) asks in an in-app confirm dialog (Cancel / Yes) instead of the browser's confirm(). Toast moved to a shared `Toast` service (`core/toast.ts`) rendered in the shell; shown after filled orders and filled liquidations. Errors and unfilled sales stay as messages.
- Palette switched from warm paper to dark navy (tokens in `styles.scss`, new `--rule`, `--buy-fill`, `--sell-fill`); chart, dialogs, login logo and favicon follow. Revert = this file set in git if Karol prefers the light version.
