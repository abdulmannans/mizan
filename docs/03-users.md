# 03 — Users and access

Mizan v1 is a **single-owner on-device app**. There is no gym login, no invite token, no super-admin, no “stock_access” flag.

## First launch

1. Disclaimer (must Acknowledge).
2. Optional display name.
3. Notification permission (Android 13+). Explain: “Daily after market hours, only for real dips (score 65+) and gold drops.”
4. Battery / exact-alarm: request **ignore battery optimizations** (recommended, not a hard block) so the daily job is not silently killed.
5. Seed catalog → start **first-run backfill** (foreground notification: “Loading NAV history…”). Home is usable with a progress banner.

No account server. Data lives in Room. Uninstall wipes history unless you add backup later (v2: optional Drive/JSON export).

## Settings (Account screen)

| Field | Default |
|---|---|
| Available lumpsum pool | ₹50,000 |
| What-if lumpsum | ₹10,000 |
| Notifications | on |
| Notify on fund 65+ | on |
| Notify on metal 65+ | on |
| Notify on gold ₹ drop | on |
| Daily job window | 21:00 IST (retry 07:00 IST if NAV missing) |

PIN/biometric lock is optional v1.1.

## Multi-device

Not in v1. Each phone is its own Mizan. v2: export/import database or a **Mizan-owned** sync API — still not the gym app.
