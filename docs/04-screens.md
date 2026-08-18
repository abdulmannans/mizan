# 04 — Android screens

Bottom nav (4): **Home**, **Funds**, **Metals**, **Missed**. Overflow: Watchlist, Account.

Always show a compact disclaimer chip on Home; full text on first launch (must tap Acknowledge).

## 4.1 First launch

- Wordmark **Mizan**, tagline.
- Full disclaimer → Acknowledge.
- Notification permission. If denied, continue with in-app badges only.
- Start history backfill (non-blocking banner on Home).

No email/password. No invite.

## 4.2 Home

Widgets (top to bottom):

1. Disclaimer one-liner + “Read”.
2. **Tracked funds** count, **watchlist** count, **signals last 7 days**, **gold** price (silver in subtitle).
3. **Missed deploys** count + hypothetical P&L (suggested-₹ scenario) + **If ₹10k each → value today**. Tap → Missed.
4. Watchlist **Today** strip: fund name, score, level badge, attractive-now yes/no, suggested ₹. Empty: “Add funds from Funds.”
5. Next SIP (if any): fund, debit day, estimated allotment NAV vs chart NAV.
6. **Last update:** time of last successful daily job. Error chip if the last run failed.

Pull to refresh runs `ManualRefreshWorker`.

## 4.3 Funds (list)

Filter: active + tracking-enabled (member never sees scoring-disabled internals except a “not scored” badge).

Row: name, scheme code, AMC, last NAV, 1D/1M/1Y %, latest score + level, Shariah verification badge (unreviewed / verified compliant / non-compliant / rejected).

Actions: star to add/remove watchlist.

Search by name / scheme / AMC.

## 4.4 Fund detail

- Full disclaimer.
- Identity + mandate + verification notes.
- NAV + growth.
- Latest dip score, level, reasons, NAV at score.
- Charts: NAV history, score history (line).
- **Watchlist / SIP**: amount (₹), debit day 1–28. Helper: units usually allot **2–3 business days after debit**, not on debit date. Show estimated allotment NAV vs latest chart NAV.
- Signals history (paginated): date, score, level, NAV, reasons.

No admin toggles (`tracking_enabled`, etc.) for members.

## 4.5 Watchlist

Same as web “My watchlist”: name, scheme, verification, NAV, growth, SIP ₹, SIP day, added date. Configure SIP. Remove.

Empty: “Add funds from the Funds list.”

## 4.6 Missed opportunities

Header stats:

- Missed deploys, would-have-invested (suggested), value today, P&L % (suggested scenario).
- Second row: If ₹10,000 each → invested, value today, P&L.

**Today** table: watchlisted funds, current score, attractive now, suggested ₹.

**Missed dips** table: date (best score in 7-day cluster), fund, score, level, NAV then, NAV now, suggested ₹, value today, If ₹10k (value + P&L), return %.

Sort missed by date desc. Empty watchlist copy as above. Empty missed: “No attractive scores (65+) on your watchlist yet.”

Metals are **not** on this page (not user-watchlisted).

## 4.7 Metals

List gold + silver: last price, unit, 1D/1M/1Y, latest score.

Detail: price chart, score chart, drop-from-high card (peak, drop ₹, threshold). Signals history.

## 4.8 Account

Display name, lumpsum pool, what-if ₹, notification toggles (funds / metals / gold drop), last job status, **Run update now**, **Reload history**, optional battery-optimization help. No logout.

## 4.9 Notifications inbox (v1.1)

List of dip and gold-drop alerts already posted locally. Tap → fund/metal.

## Visual

- Primary: calm green/emerald (web uses emerald). Dark mode required.
- Level colours: exceptional/very_attractive = success; attractive = warning; neutral = gray; weak/unattractive = danger.
- Currency: `₹` with `en_IN` grouping. Scores as integers 0–100.
