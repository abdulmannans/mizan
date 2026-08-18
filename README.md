# Mizan

**SIP first. Extra cash on dips.**

A **standalone Android app**. It is not part of the gym site, not a companion screen, and not a client of that Laravel panel. Mizan owns its own database, runs its own **daily background jobs**, scores NAVs on the phone, and fires **Android notifications**.

It is a lumpsum dip assistant and SIP companion — not a broker, not a stock picker, not a fatwa.

## Brand

| | |
|---|---|
| Name | **Mizan** (الميزان — the Scale) |
| Tagline | SIP first. Extra cash on dips. |
| Package | `app.mizan.android` |
| Tone | Calm, precise, rupee amounts. Never “guaranteed return” or “buy now”. |

## Independence (non-negotiable)

- Own git repo / Android project. Zero imports from the gym codebase at runtime.
- Own Room database on device.
- Talks to **public market APIs** (MFAPI, Yahoo, metal/FX sources) — not to gym `/stock`.
- Daily “cron” = **WorkManager** (plus a retry). Notifications = **system NotificationManager** (local). No FCM server required for v1.
- A private web panel that inspired the behaviour is **reference only**. Do not couple releases.

## Read in this order

1. [docs/01-product.md](docs/01-product.md)
2. [docs/02-principles.md](docs/02-principles.md)
3. [docs/03-users.md](docs/03-users.md)
4. [docs/04-screens.md](docs/04-screens.md)
5. [docs/05-features.md](docs/05-features.md)
6. [docs/06-score.md](docs/06-score.md)
7. [docs/07-data.md](docs/07-data.md) — on-device store + market HTTP
8. [docs/08-notifications.md](docs/08-notifications.md)
9. [docs/09-compliance.md](docs/09-compliance.md)
10. [docs/10-mvp.md](docs/10-mvp.md)
11. [docs/11-daily-jobs.md](docs/11-daily-jobs.md) — the background cron

## One-line product

Watchlisted Direct Growth ethical funds + Mumbai gold/silver, scored **on device every day**. Monthly SIP is the default. Extra ₹ only at dip score **65+**. Hypothetical “if you had invested” math. Local Android alerts. No orders.
