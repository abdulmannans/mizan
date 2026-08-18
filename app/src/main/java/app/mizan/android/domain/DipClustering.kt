package app.mizan.android.domain

import java.time.LocalDate

data class AttractiveDay(
    val date: LocalDate,
    val score: Int,
    val level: Level,
    val value: Double,
)

/**
 * A crash is one event, not one event per session. Attractive days within the cooldown of each
 * other collapse into the single highest-score day.
 */
object DipClustering {

    const val COOLDOWN_DAYS = 7L

    fun cluster(days: List<AttractiveDay>): List<AttractiveDay> {
        if (days.isEmpty()) return emptyList()
        val ordered = days.sortedBy { it.date }
        val result = mutableListOf<AttractiveDay>()

        var best = ordered.first()
        var lastInCluster = ordered.first().date

        for (day in ordered.drop(1)) {
            val gap = day.date.toEpochDay() - lastInCluster.toEpochDay()
            if (gap > COOLDOWN_DAYS) {
                result += best
                best = day
            } else if (day.score > best.score || (day.score == best.score && day.date > best.date)) {
                best = day
            }
            lastInCluster = day.date
        }
        result += best
        return result
    }
}
