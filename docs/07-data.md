# 07 — On-device data and market HTTP

No Mizan REST server in v1. Room is the source of truth. The phone calls **public market endpoints** from the daily job and from pull-to-refresh.

## Room entities

Same shapes as the product model:

- `Fund` — bundled catalog row + `lastNav`, `navAsOf`, growth fields
- `FundPrice` — `(fundId, priceDate)` unique, `nav`
- `InvestmentSignal` — `(fundId, signalDate)` unique, score, level, component scores, nav, reasons JSON
- `WatchlistItem` — `fundId`, `sipAmount`, `sipDayOfMonth`
- `Metal`, `MetalPrice`, `MetalSignal`
- `BenchmarkPrice` — Nifty 50 close by date
- `NotificationLog` — type, fund/metal id, sentAt, score/level (for cooldown)
- `JobRun` — startedAt, finishedAt, status, error, counts (funds fetched, signals written, notifies posted)

## Market sources (same idea as the reference implementation)

| Job | Source | Notes |
|---|---|---|
| Fund NAV (latest + history) | `https://api.mfapi.in/mf/{schemeCode}` | Direct Growth scheme codes only |
| Nifty 50 | Yahoo chart `^NSEI` | Fallback: MFAPI scheme `119598` (HDFC Nifty 50 Direct Growth) |
| Gold/silver latest | GoodReturns Mumbai HTML, then IBJA, then COMEX×FX | Gold ₹/10g, silver ₹/kg |
| Metal history | COMEX × FX, scaled to India level | See reference `config/stock.php` metals block |
| FX | Frankfurter `api.frankfurter.app` | USDINR |

User-Agent required for Yahoo. Timeouts 20s. Retry 3× with backoff. Never block the UI thread.

Honor robots/ToS; if a source breaks, fail that step, keep last good Room data, show “last updated …” on Home.

## Client modules (Kotlin)

| Module | Responsibility |
|---|---|
| `MfapiClient` | Parse NAV history JSON |
| `YahooChartClient` | Nifty closes |
| `MetalClient` | India gold/silver |
| `IndicatorEngine` | drawdown, returns, vol |
| `DipScoreEngine` | port of opportunity scoring ([06-score.md](06-score.md)) |
| `SipAllotment` | debit + 2 business days |
| `MissedOpportunity` | cluster 7d, mark-to-market, ₹10k what-if |
| `DailyPipelineWorker` | [11-daily-jobs.md](11-daily-jobs.md) |
| `MizanNotifier` | local notifications |

## Offline

App must open fully offline after first successful backfill. Stale if last successful `JobRun` > 36h: banner “Prices may be old. Pull to refresh.”

## What we are not building in v1

- Laravel / gym `/api/mizan`
- FCM device registry
- Email
- Multi-user auth
