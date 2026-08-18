package app.mizan.android.domain

import java.time.LocalDate

data class LastNotification(val date: LocalDate, val level: Level)

/**
 * Five funds in one crash must not flood the shade, and a soft market must not ping at all.
 */
object NotifyRules {

    const val COOLDOWN_DAYS = 7L

    /** Gold alert threshold below the rolling 60-day peak, in rupees per 10g. */
    const val GOLD_DROP_RUPEES = 10_000.0
    const val GOLD_PEAK_WINDOW_DAYS = 60L

    fun shouldNotifyDip(score: Int, on: LocalDate, last: LastNotification?): Boolean {
        if (score < Level.ATTRACTIVE_SCORE) return false
        val level = Level.of(score)
        if (last == null) return true
        val withinCooldown = on.toEpochDay() - last.date.toEpochDay() <= COOLDOWN_DAYS
        if (!withinCooldown) return true
        return level.rank > last.level.rank
    }

    fun shouldNotifyDrop(dropRupees: Double, on: LocalDate, last: LastNotification?): Boolean {
        if (dropRupees < GOLD_DROP_RUPEES) return false
        if (last == null) return true
        return on.toEpochDay() - last.date.toEpochDay() > COOLDOWN_DAYS
    }
}
