package app.mizan.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DipClusteringTest {

    private fun day(day: Int, score: Int) = AttractiveDay(
        date = LocalDate.of(2026, 3, day),
        score = score,
        level = Level.of(score),
        value = 100.0 - score,
    )

    @Test
    fun `three attractive days two days apart collapse into one deploy`() {
        val clusters = DipClustering.cluster(listOf(day(2, 68), day(4, 74), day(6, 71)))
        assertEquals(1, clusters.size)
        assertEquals(74, clusters.first().score)
        assertEquals(LocalDate.of(2026, 3, 4), clusters.first().date)
    }

    @Test
    fun `a gap of eight days starts a second cluster`() {
        val clusters = DipClustering.cluster(listOf(day(2, 68), day(10, 66)))
        assertEquals(2, clusters.size)
        assertEquals(listOf(68, 66), clusters.map { it.score })
    }

    @Test
    fun `a gap of exactly seven days stays one cluster`() {
        val clusters = DipClustering.cluster(listOf(day(2, 68), day(9, 66)))
        assertEquals(1, clusters.size)
    }

    @Test
    fun `ties keep the later date`() {
        val clusters = DipClustering.cluster(listOf(day(2, 70), day(5, 70)))
        assertEquals(1, clusters.size)
        assertEquals(LocalDate.of(2026, 3, 5), clusters.first().date)
    }
}

class NotifyRulesTest {

    private val today = LocalDate.of(2026, 8, 18)

    @Test
    fun `neutral never notifies`() {
        assertFalse(NotifyRules.shouldNotifyDip(64, today, null))
        assertTrue(NotifyRules.shouldNotifyDip(65, today, null))
    }

    @Test
    fun `same level inside the cooldown stays quiet`() {
        val last = LastNotification(today.minusDays(3), Level.ATTRACTIVE)
        assertFalse(NotifyRules.shouldNotifyDip(70, today, last))
    }

    @Test
    fun `a level step up breaks the cooldown`() {
        val last = LastNotification(today.minusDays(3), Level.ATTRACTIVE)
        assertTrue(NotifyRules.shouldNotifyDip(82, today, last))
    }

    @Test
    fun `after the cooldown the same level notifies again`() {
        val last = LastNotification(today.minusDays(8), Level.ATTRACTIVE)
        assertTrue(NotifyRules.shouldNotifyDip(70, today, last))
    }

    @Test
    fun `gold drop needs the rupee threshold and its own cooldown`() {
        assertFalse(NotifyRules.shouldNotifyDrop(9_000.0, today, null))
        assertTrue(NotifyRules.shouldNotifyDrop(10_500.0, today, null))
        assertFalse(
            NotifyRules.shouldNotifyDrop(
                12_000.0,
                today,
                LastNotification(today.minusDays(2), Level.ATTRACTIVE),
            )
        )
    }
}

class MissedOpportunityTest {

    private fun day(day: Int, score: Int, nav: Double) = AttractiveDay(
        date = LocalDate.of(2026, 3, day),
        score = score,
        level = Level.of(score),
        value = nav,
    )

    @Test
    fun `mark to market uses suggested rupees and the fixed what-if`() {
        val deploys = MissedOpportunity.build(
            targetId = "119172",
            name = "Tata Ethical",
            attractiveDays = listOf(day(2, 70, 100.0)),
            navNow = 120.0,
            availableLumpsum = 50_000.0,
            whatIfAmount = 10_000.0,
        )
        assertEquals(1, deploys.size)
        val deploy = deploys.first()
        assertEquals(12_500.0, deploy.suggestedRupees, 0.001)
        assertEquals(15_000.0, deploy.valueToday, 0.001)
        assertEquals(12_000.0, deploy.whatIfValueToday, 0.001)
        assertEquals(20.0, deploy.returnPercent, 0.001)
    }

    @Test
    fun `a neutral day produces no deploy even if it slips through`() {
        val deploys = MissedOpportunity.build(
            targetId = "119172",
            name = "Tata Ethical",
            attractiveDays = MissedOpportunity.attractiveOnly(listOf(day(2, 64, 100.0))),
            navNow = 120.0,
            availableLumpsum = 50_000.0,
            whatIfAmount = 10_000.0,
        )
        assertTrue(deploys.isEmpty())
    }

    @Test
    fun `missing current nav yields nothing rather than a wrong number`() {
        val deploys = MissedOpportunity.build(
            targetId = "119172",
            name = "Tata Ethical",
            attractiveDays = listOf(day(2, 70, 100.0)),
            navNow = null,
            availableLumpsum = 50_000.0,
            whatIfAmount = 10_000.0,
        )
        assertTrue(deploys.isEmpty())
    }
}
