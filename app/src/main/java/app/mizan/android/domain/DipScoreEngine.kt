package app.mizan.android.domain

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

data class ScoreComponents(
    val drawdown52w: Int,
    val momentum30d: Int,
    val correction90d: Int,
    val trend1y: Int,
    val volatility: Int,
    val vsBenchmark: Int,
) {
    fun toMap(): Map<String, Int> = mapOf(
        "drawdown_52w" to drawdown52w,
        "momentum_30d" to momentum30d,
        "correction_90d" to correction90d,
        "trend_1y" to trend1y,
        "volatility" to volatility,
        "vs_benchmark" to vsBenchmark,
    )

    /** Weight labels for the fund detail breakdown. */
    fun labelled(): List<Triple<String, Int, Int>> = listOf(
        Triple("52-week drawdown", drawdown52w, DipScoreEngine.WEIGHT_DRAWDOWN_52W),
        Triple("30-day momentum", momentum30d, DipScoreEngine.WEIGHT_MOMENTUM_30D),
        Triple("90-day correction", correction90d, DipScoreEngine.WEIGHT_CORRECTION_90D),
        Triple("1-year trend", trend1y, DipScoreEngine.WEIGHT_TREND_1Y),
        Triple("Volatility", volatility, DipScoreEngine.WEIGHT_VOLATILITY),
        Triple("vs Nifty 50", vsBenchmark, DipScoreEngine.WEIGHT_VS_BENCHMARK),
    )

    companion object {
        fun fromMap(values: Map<String, Double>): ScoreComponents = ScoreComponents(
            drawdown52w = values["drawdown_52w"].toScore(),
            momentum30d = values["momentum_30d"].toScore(),
            correction90d = values["correction_90d"].toScore(),
            trend1y = values["trend_1y"].toScore(),
            volatility = values["volatility"].toScore(),
            vsBenchmark = values["vs_benchmark"].toScore(),
        )

        private fun Double?.toScore(): Int = this?.roundToInt() ?: 0
    }
}

data class DipScore(
    val date: LocalDate,
    val score: Int,
    val level: Level,
    val value: Double,
    val components: ScoreComponents,
    val reasons: List<String>,
)

/**
 * The dip meter. Weights sum to 100 and every component rewards weakness, because the question
 * is "how much of a dip is this" rather than "is this a good fund".
 *
 * The curves, the partial credit given when an indicator is calm or missing, and the per-component
 * rounding all mirror the reference panel so a fund scores the same in both places.
 */
object DipScoreEngine {

    const val WEIGHT_DRAWDOWN_52W = 30
    const val WEIGHT_MOMENTUM_30D = 20
    const val WEIGHT_CORRECTION_90D = 15
    const val WEIGHT_TREND_1Y = 15
    const val WEIGHT_VOLATILITY = 10
    const val WEIGHT_VS_BENCHMARK = 10

    private const val FULL_DRAWDOWN_52W = 0.25
    private const val FULL_DROP_30D = 0.15
    private const val FULL_DROP_90D = 0.20
    private const val STRONG_YEAR = 0.20
    private const val FULL_WEAK_YEAR = 0.25
    private const val FULL_VOLATILITY = 0.03
    private const val FULL_RECENT_DROP = 0.15
    private const val FULL_UNDERPERFORMANCE = 0.10

    /** A gap this small reads as noise rather than a reason to act. */
    private const val MEANINGFUL_UNDERPERFORMANCE = 0.03
    private const val MEANINGFUL_DRAWDOWN = 0.10

    /**
     * @param benchmark90d benchmark return over the same ~90-day window, or null to fall back to a
     *   share of the benchmark weight (metals have no fund benchmark).
     */
    fun score(
        indicators: Indicators,
        benchmark90d: Double?,
        subject: String,
    ): DipScore {
        val relative = if (benchmark90d == null || indicators.return90d == null) {
            null
        } else {
            indicators.return90d - benchmark90d
        }

        val components = ScoreComponents(
            drawdown52w = scoreDrawdown(indicators.drawdown52w),
            momentum30d = scoreMomentum(indicators.return30d),
            correction90d = scoreCorrection(indicators.return90d),
            trend1y = scoreTrend(indicators.return1y),
            volatility = scoreVolatility(indicators.volatility, indicators.recentDrawdown),
            vsBenchmark = scoreBenchmark(relative),
        )

        val total = with(components) {
            drawdown52w + momentum30d + correction90d + trend1y + volatility + vsBenchmark
        }.coerceIn(0, 100)

        return DipScore(
            date = indicators.asOf,
            score = total,
            level = Level.of(total),
            value = indicators.current,
            components = components,
            reasons = reasons(indicators, relative, subject),
        )
    }

    /** Drawdown is already a positive fraction below the 52-week high. */
    private fun scoreDrawdown(drawdown52w: Double): Int =
        (WEIGHT_DRAWDOWN_52W * min(1.0, drawdown52w / FULL_DRAWDOWN_52W)).roundToInt()

    private fun scoreMomentum(return30d: Double?): Int {
        val share = when {
            return30d == null -> 0.30
            return30d >= 0.0 -> 0.20
            else -> 0.40 + 0.60 * min(1.0, abs(return30d) / FULL_DROP_30D)
        }
        return (WEIGHT_MOMENTUM_30D * share).roundToInt()
    }

    private fun scoreCorrection(return90d: Double?): Int {
        val share = when {
            return90d == null -> return 0
            return90d >= 0.0 -> 0.15
            else -> min(1.0, abs(return90d) / FULL_DROP_90D)
        }
        return (WEIGHT_CORRECTION_90D * share).roundToInt()
    }

    private fun scoreTrend(return1y: Double?): Int {
        val share = when {
            return1y == null -> 0.30
            return1y > STRONG_YEAR -> 0.20
            return1y >= 0.0 -> 0.45
            else -> 0.50 + 0.50 * min(1.0, abs(return1y) / FULL_WEAK_YEAR)
        }
        return (WEIGHT_TREND_1Y * share).roundToInt()
    }

    private fun scoreVolatility(volatility: Double?, recentDrawdown: Double): Int {
        val volPart = volatility?.let { min(1.0, it / FULL_VOLATILITY) } ?: 0.30
        val dropPart = min(1.0, recentDrawdown / FULL_RECENT_DROP)
        return (WEIGHT_VOLATILITY * ((volPart + dropPart) / 2.0)).roundToInt()
    }

    private fun scoreBenchmark(relative: Double?): Int {
        val share = when {
            relative == null -> 0.40
            relative >= 0.0 -> 0.25
            else -> 0.40 + 0.60 * min(1.0, abs(relative) / FULL_UNDERPERFORMANCE)
        }
        return (WEIGHT_VS_BENCHMARK * share).roundToInt()
    }

    private fun reasons(indicators: Indicators, relative: Double?, subject: String): List<String> {
        val reasons = mutableListOf<String>()
        if (indicators.drawdown52w >= MEANINGFUL_DRAWDOWN) {
            reasons += "$subject is ${percent(indicators.drawdown52w)} below its rolling 52-week high"
        }
        indicators.return90d?.let {
            if (it < 0) reasons += "90-day return is negative (${percent(abs(it))} down)"
        }
        indicators.return30d?.let {
            if (it < 0) reasons += "30-day return is negative (${percent(abs(it))} down)"
        }
        if (relative != null && relative < -MEANINGFUL_UNDERPERFORMANCE) {
            reasons += "$subject has underperformed the benchmark by ${percent(abs(relative))} over ~90 days"
        }
        if (reasons.isEmpty()) {
            reasons += "Score reflects configured indicator weights; conditions are not strongly attractive"
        }
        return reasons
    }

    private fun percent(fraction: Double): String = "%.1f%%".format(fraction * 100)
}
