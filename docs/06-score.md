# 06 — Dip opportunity score

Computed **on the phone** in `DipScoreEngine` (Kotlin port of the reference logic). The daily job writes `InvestmentSignal` rows into Room. The UI never calls a scoring server.

## Inputs (per fund, as of date)

From NAV series:

- Current NAV
- 52-week drawdown
- 30-day return
- 90-day return
- 1-year return
- Volatility
- Recent drawdown

Benchmark: Nifty 50 (Yahoo `^NSEI`, fallback MFAPI scheme 119598). Relative ~90-day underperformance vs benchmark.

Skip scoring when NAV equals the prior session (holiday / unchanged).

## Weights (sum 100)

| Component | Weight | Direction (higher score) |
|---|---|---|
| 52w drawdown | 30 | Deeper drop from 52w high |
| 30d momentum | 20 | More **negative** 30d return |
| 90d correction | 15 | More **negative** 90d return |
| 1y trend | 15 | Weak/negative year scores up; strong >20% year scores down |
| Volatility | 10 | Higher vol + recent drop |
| vs Nifty | 10 | Fund lagging benchmark |

This is intentional: the meter is **“how much of a dip”**, not quality, not “best AMC”.

Metals: same weights; vs-Nifty uses a 40% fallback of that weight when there is no fund.

## Levels

```
≥ 90 exceptional
≥ 80 very_attractive
≥ 65 attractive          ← alerts + missed extra lumpsum
≥ 50 neutral             ← visible, not alerted
≥ 30 weak
else unattractive
```

## Allocation bands → suggested %

Mid of min/max, rounded:

| Score ≤ | min% | max% | suggested% |
|---|---|---|---|
| 49 | 0 | 0 | 0 |
| 64 | 0 | 10 | 5 |
| 79 | 20 | 30 | 25 |
| 89 | 30 | 50 | 40 |
| 100 | 50 | 75 | 63 |

Suggested ₹ = `available_lumpsum × suggested% / 100`.  
Example: score 70, pool ₹50,000 → **₹12,500**.

## Reasons (examples)

- “Fund is X% below its rolling 52-week high”
- “90-day return is negative”
- “30-day return is negative”
- “Fund has underperformed benchmark by X% over ~90 days”
- Fallback: “Score reflects configured indicator weights; conditions are not strongly attractive”

## Clustering (missed + notify)

Cooldown **7 days**.

Missed: walk attractive days in date order; if gap from last day **in the current cluster** > 7, close cluster and start new. Emit the day with max score (tie: later date).

Notify: score must be ≥ 65; if another signal for that fund exists in the previous 7 days, only notify if **level rank** increased  
`unattractive < weak < neutral < attractive < very_attractive < exceptional`.

## What-if ₹10,000

`value_today = 10000 / nav_then × nav_now`  
Return % is the same as NAV change (independent of amount).

Forward-return research screens are **not** MVP.
