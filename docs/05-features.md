# 05 — Features (functional)

IDs are stable. Implement on device.

## Catalog

| ID | Requirement |
|---|---|
| F-CAT-01 | Show bundled Direct Growth ethical funds. |
| F-CAT-02 | Display scheme code, name, symbol, ISIN, AMC, mandate, catalog as-of, Shariah verification status + notes. |
| F-CAT-03 | Show last NAV, NAV as-of, growth 1D / 1M / 1Y from Room. |
| F-CAT-04 | NAV history for charts, filled by backfill + daily job. |
| F-CAT-05 | New schemes ship in an app update. |

## Watchlist and SIP

| ID | Requirement |
|---|---|
| F-WL-01 | Add/remove a fund in Room. |
| F-WL-02 | Optional monthly SIP amount (₹, min 100) and debit day 1–28. |
| F-WL-03 | Estimated allotment date = debit + 2 business days (skip weekends); max lag 3. |
| F-WL-04 | Allotment NAV is NAV on allotment date, not necessarily latest chart NAV. |
| F-WL-05 | Never recommend pausing SIP. |

## Dip score (on device)

| ID | Requirement |
|---|---|
| F-SC-01 | Daily score 0–100 with components and reasons ([06-score.md](06-score.md)). |
| F-SC-02 | Do not score unchanged NAV vs prior session. |
| F-SC-03 | Attractive 65, very 80, exceptional 90; 50–64 neutral; 30–49 weak. |
| F-SC-04 | Suggested ₹ = band mid-% × lumpsum pool (default 50,000). |
| F-SC-05 | Notify only if score ≥ 65, fund is watchlisted, cooldown passes. |
| F-SC-06 | Engine runs in `DailyPipelineWorker`, not only when the UI is open. |

## Jobs

| ID | Requirement |
|---|---|
| F-JOB-01 | Periodic daily pipeline ~21:00 IST + 07:00 retry if NAV missing. |
| F-JOB-02 | First-run (and Reload history) backfill ~5y; no historical notification spam. |
| F-JOB-03 | Pull-to-refresh / Run update now triggers the same pipeline. |
| F-JOB-04 | Persist `JobRun` and show status on Home + Account. |

## Missed opportunities

| ID | Requirement |
|---|---|
| F-MO-01 | Filter = current watchlist funds, all-time attractive days (including before starring). |
| F-MO-02 | Attractive = score ≥ 65. Score 64 and below never appear as missed deploys. |
| F-MO-03 | Cluster by fund: gap > 7 days starts a new cluster; pick highest score (tie → later date). |
| F-MO-04 | Units = suggested ₹ / NAV then; value today = units × last NAV. Skip if NAV or suggested ₹ invalid. |
| F-MO-05 | Fixed what-if: invest ₹10,000 (configurable) at NAV then → value today + P&L. |
| F-MO-06 | Totals for both suggested and what-if scenarios. |
| F-MO-07 | Today block: current score per watchlisted fund + attractive_now + suggested ₹. |

## Metals

| ID | Requirement |
|---|---|
| F-MT-01 | Gold and silver, Mumbai-style. Gold ₹/10g, silver ₹/kg. |
| F-MT-02 | Same dip-score engine; benchmark component uses fallback. |
| F-MT-03 | Gold drop: ≥ ₹10,000 below 60-day peak; 7-day cooldown. Independent of score. Silver off. |
| F-MT-04 | Disclose: excludes making charges and GST. |

## Notifications

| ID | Requirement |
|---|---|
| F-NT-01 | Local Android notifications from the daily job (no email, no FCM in v1). |
| F-NT-02 | Body: score, level, suggested ₹. Tap opens fund/metal. |
| F-NT-03 | Cooldown 7 days at same or lower level; allow if level increases. |
| F-NT-04 | Metal 65+ and gold ₹ drop, same cooldown idea. |
| F-NT-05 | Fund alerts: **watchlist only**. |

## Non-features (v1)

- Orders / broker login
- Cloud login, invites, gym users
- 50–64 in alerts or missed
- Ranking funds
- Admin catalog editor
