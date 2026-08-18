package app.mizan.android.domain

import java.time.LocalDate

data class MissedDeploy(
    val targetId: String,
    val name: String,
    val date: LocalDate,
    val score: Int,
    val level: Level,
    val navThen: Double,
    val navNow: Double,
    val suggestedRupees: Double,
    val units: Double,
    val valueToday: Double,
    val whatIfInvested: Double,
    val whatIfValueToday: Double,
) {
    /** Return is the NAV change, so it is identical for both scenarios. */
    val returnPercent: Double get() = (navNow / navThen - 1.0) * 100.0
    val suggestedPnl: Double get() = valueToday - suggestedRupees
    val whatIfPnl: Double get() = whatIfValueToday - whatIfInvested
}

data class MissedTotals(
    val deploys: Int,
    val suggestedInvested: Double,
    val suggestedValueToday: Double,
    val whatIfInvested: Double,
    val whatIfValueToday: Double,
) {
    val suggestedPnl: Double get() = suggestedValueToday - suggestedInvested
    val suggestedPnlPercent: Double
        get() = if (suggestedInvested > 0) suggestedPnl / suggestedInvested * 100.0 else 0.0
    val whatIfPnl: Double get() = whatIfValueToday - whatIfInvested
    val whatIfPnlPercent: Double
        get() = if (whatIfInvested > 0) whatIfPnl / whatIfInvested * 100.0 else 0.0
}

/**
 * Hypothetical mark-to-market of the attractive days a watchlisted fund printed. Nothing here
 * is a recorded trade.
 */
object MissedOpportunity {

    fun build(
        targetId: String,
        name: String,
        attractiveDays: List<AttractiveDay>,
        navNow: Double?,
        availableLumpsum: Double,
        whatIfAmount: Double,
    ): List<MissedDeploy> {
        if (navNow == null || navNow <= 0.0) return emptyList()
        return DipClustering.cluster(attractiveDays).mapNotNull { day ->
            val navThen = day.value
            val suggested = AllocationBands.suggestedRupees(day.score, availableLumpsum)
            if (navThen <= 0.0 || suggested <= 0.0) return@mapNotNull null
            val units = suggested / navThen
            MissedDeploy(
                targetId = targetId,
                name = name,
                date = day.date,
                score = day.score,
                level = day.level,
                navThen = navThen,
                navNow = navNow,
                suggestedRupees = suggested,
                units = units,
                valueToday = units * navNow,
                whatIfInvested = whatIfAmount,
                whatIfValueToday = whatIfAmount / navThen * navNow,
            )
        }
    }

    fun totals(deploys: List<MissedDeploy>): MissedTotals = MissedTotals(
        deploys = deploys.size,
        suggestedInvested = deploys.sumOf { it.suggestedRupees },
        suggestedValueToday = deploys.sumOf { it.valueToday },
        whatIfInvested = deploys.sumOf { it.whatIfInvested },
        whatIfValueToday = deploys.sumOf { it.whatIfValueToday },
    )

    fun sortedByDateDesc(deploys: List<MissedDeploy>): List<MissedDeploy> =
        deploys.sortedByDescending { it.date }

    fun attractiveOnly(days: List<AttractiveDay>): List<AttractiveDay> =
        days.filter { it.score >= Level.ATTRACTIVE_SCORE }

    fun latestDate(deploys: List<MissedDeploy>): LocalDate? = deploys.maxOfOrNull { it.date }
}
