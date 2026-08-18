# 10 — MVP

## Ship

1. Onboarding + notification permission + disclaimer
2. Bundled fund catalog + Room
3. **BackfillWorker** then **DailyPipelineWorker** (21:00 IST + morning retry)
4. Home, Funds, Fund detail (NAV + score charts), Watchlist + SIP allotment
5. Missed opportunities (clusters, suggested ₹, ₹10k what-if) computed locally
6. Metals + gold drop card
7. **Local Android notifications** (65+ watchlist, metal 65+, gold ₹ drop)
8. Account: pool, what-if, notify toggles, last job, Run update now
9. Pull to refresh, dark mode, `en_IN` rupees

No gym API. No login. No email. No FCM.

## Out of MVP

- Attractive threshold below 65; 50–64 in alerts/missed
- Ranking funds
- Orders / mandates
- Cloud sync / multi-device
- In-app catalog editor (ship new funds via app update)
- Sleeve-level single dip for all ethical funds

## Stack

- Kotlin, Jetpack Compose, Material 3, min SDK 26
- Room + Hilt
- WorkManager + Foreground Service for backfill
- OkHttp/Retrofit
- Vico or similar for charts
- `NotificationCompat` only (no Firebase in v1)

## QA

- App works with gym site **uninstalled / unreachable**
- Score 64 never notifies and never appears as a missed deploy
- Three attractive days 2 days apart → one missed row
- Gap of 8 days → two clusters
- Non-watchlisted fund: scored, **no** notification, **not** in missed
- Unchanged NAV day: no new score, no notify
- Backfill does not dump years of notifications
- Airplane mode after backfill: UI still opens
- Kill app; wait for next job window (or Run update now) → notification still appears
- Disclaimer cannot be skipped

## Name lock

**Mizan**. Fallback Play id: `app.mizan.dip`.
