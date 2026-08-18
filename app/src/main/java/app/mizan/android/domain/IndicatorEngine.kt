package app.mizan.android.domain

import java.time.LocalDate
import kotlin.math.sqrt

data class PricePoint(val date: LocalDate, val value: Double)

data class Indicators(
    val asOf: LocalDate,
    val current: Double,
    val previous: Double?,
    val high52w: Double,
    /** Fraction below the rolling 52-week high, 0..1. */
    val drawdown52w: Double,
    val return30d: Double?,
    val return90d: Double?,
    val return1y: Double?,
    /** Standard deviation of daily returns over the last 60 observations, null when too short. */
    val volatility: Double?,
    /** Fraction below the 90-day high, 0..1. */
    val recentDrawdown: Double,
)

/**
 * Turns a price/NAV series into the inputs the dip score needs. Windows are anchored to the last
 * observation and measured in calendar days, so holidays in the Indian session do not shift them.
 */
object IndicatorEngine {

    private const val VOLATILITY_WINDOW = 60

    fun compute(series: List<PricePoint>, asOf: LocalDate? = null): Indicators? {
        if (series.isEmpty()) return null
        val sorted = series.sortedBy { it.date }
        val window = if (asOf == null) sorted else sorted.filter { !it.date.isAfter(asOf) }
        if (window.isEmpty()) return null

        val last = window.last()
        val current = last.value
        if (current <= 0.0) return null

        val previous = window.getOrNull(window.size - 2)?.value

        val high52w = highSince(window, last.date.minusYears(1)) ?: current
        val drawdown52w = if (high52w > 0) ((high52w - current) / high52w).coerceIn(0.0, 1.0) else 0.0

        val recentHigh = highSince(window, last.date.minusDays(90)) ?: current
        val recentDrawdown =
            if (recentHigh > 0) ((recentHigh - current) / recentHigh).coerceIn(0.0, 1.0) else 0.0

        return Indicators(
            asOf = last.date,
            current = current,
            previous = previous,
            high52w = high52w,
            drawdown52w = drawdown52w,
            return30d = returnSince(window, last.date.minusDays(30)),
            return90d = returnSince(window, last.date.minusDays(90)),
            return1y = returnSince(window, last.date.minusYears(1)),
            volatility = volatility(window),
            recentDrawdown = recentDrawdown,
        )
    }

    /** Return from the last observation on or before [target] to the end of [window]. */
    fun returnSince(window: List<PricePoint>, target: LocalDate): Double? {
        if (window.isEmpty()) return null
        val past = window.lastOrNull { !it.date.isAfter(target) } ?: return null
        if (past.value <= 0.0) return null
        return window.last().value / past.value - 1.0
    }

    private fun highSince(window: List<PricePoint>, since: LocalDate): Double? =
        window.filter { !it.date.isBefore(since) }.maxOfOrNull { it.value }

    /** Sample standard deviation of daily simple returns, not annualised. */
    private fun volatility(window: List<PricePoint>): Double? {
        val tail = window.takeLast(VOLATILITY_WINDOW)
        if (tail.size < 5) return null

        val returns = mutableListOf<Double>()
        var previous: Double? = null
        for (point in tail) {
            val prior = previous
            if (prior != null && prior > 0.0) returns += point.value / prior - 1.0
            previous = point.value
        }
        if (returns.size < 2) return null

        val mean = returns.average()
        return sqrt(returns.sumOf { (it - mean) * (it - mean) } / (returns.size - 1))
    }
}
