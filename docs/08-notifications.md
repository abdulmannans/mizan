# 08 — Android notifications

v1 uses **local notifications** posted by the daily job on the same device. No email. No Firebase required.

Permission: `POST_NOTIFICATIONS` (API 33+). If denied, still score and show badges in-app; Home explains how to enable.

## Channels

Create at first launch:

| Channel id | Name | Importance |
|---|---|---|
| `mizan.dips` | Dip opportunities | HIGH |
| `mizan.gold` | Gold drop | HIGH |
| `mizan.jobs` | Background updates | LOW (foreground service while backfill/pipeline runs) |

## Fund dip (65+)

**When:** job wrote today’s signal, `score >= 65`, cooldown passes.

**Title:** `Dip opportunity — {fund name}`  
**Body:** `Score {n}/100 · {LEVEL} · suggested ₹{amount}`  
Tap → Fund detail.

Cooldown **7 days** unless **level increases** (attractive → very_attractive → exceptional). Persist in `NotificationLog`.

Notify **watchlisted funds only**.

## Metal dip (65+)

Same rules. Title: `Dip opportunity — Gold` / `Silver`.

## Gold INR drop (independent of score)

**When:** `peak − current ≥ ₹10,000` in the last 60 days, and no `gold_drop` log in 7 days.

**Title:** `Gold off recent high`  
**Body:** `Down ₹{drop} from ₹{peak} ({peak_date})`

Silver drop off unless threshold set > 0.

## Grouping

`setGroup("mizan.dips")` so five funds in one crash do not flood. Summary: `{n} dip alerts`.

## Do not notify

- Neutral / weak / unattractive
- Unchanged NAV vs prior session (no new score)
- Same 7-day window at the same or lower level
- User toggled that category off
- Job failure (use the LOW job channel only if you must: “Couldn’t update prices”)

## Quiet hours (optional v1.1)

Default none. Pipeline already runs ~21:00 IST.
