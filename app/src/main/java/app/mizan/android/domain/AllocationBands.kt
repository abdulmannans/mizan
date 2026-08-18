package app.mizan.android.domain

import kotlin.math.roundToLong

/**
 * Extra-lumpsum sizing bands. Suggested percent is the mid of the band, and the rupee figure
 * is hypothetical sizing for a ring-fenced pool -- not a directive to buy.
 */
object AllocationBands {

    data class Band(val minPercent: Int, val maxPercent: Int, val suggestedPercent: Int)

    fun band(score: Int): Band = when {
        score <= 49 -> Band(0, 0, 0)
        score <= 64 -> Band(0, 10, 5)
        score <= 79 -> Band(20, 30, 25)
        score <= 89 -> Band(30, 50, 40)
        else -> Band(50, 75, 63)
    }

    fun suggestedPercent(score: Int): Int = band(score).suggestedPercent

    /** Suggested rupees = band mid-percent x available lumpsum pool. */
    fun suggestedRupees(score: Int, availableLumpsum: Double): Double =
        (availableLumpsum * suggestedPercent(score) / 100.0).roundToLong().toDouble()
}
