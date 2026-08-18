package app.mizan.android.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LevelAndBandTest {

    @Test
    fun `levels follow the documented cut-offs`() {
        assertEquals(Level.UNATTRACTIVE, Level.of(29))
        assertEquals(Level.WEAK, Level.of(30))
        assertEquals(Level.NEUTRAL, Level.of(50))
        assertEquals(Level.NEUTRAL, Level.of(64))
        assertEquals(Level.ATTRACTIVE, Level.of(65))
        assertEquals(Level.VERY_ATTRACTIVE, Level.of(80))
        assertEquals(Level.EXCEPTIONAL, Level.of(90))
    }

    @Test
    fun `score 64 is never attractive`() {
        assertFalse(Level.of(64).isAttractive)
        assertTrue(Level.of(65).isAttractive)
    }

    @Test
    fun `suggested rupees use band mid percent of the pool`() {
        assertEquals(12_500.0, AllocationBands.suggestedRupees(70, 50_000.0), 0.001)
        assertEquals(2_500.0, AllocationBands.suggestedRupees(60, 50_000.0), 0.001)
        assertEquals(0.0, AllocationBands.suggestedRupees(40, 50_000.0), 0.001)
        assertEquals(20_000.0, AllocationBands.suggestedRupees(85, 50_000.0), 0.001)
        assertEquals(31_500.0, AllocationBands.suggestedRupees(95, 50_000.0), 0.001)
    }
}

class DipScoreEngineTest {

    private fun series(values: List<Double>, endDate: LocalDate = LocalDate.of(2026, 8, 18)) =
        values.mapIndexed { index, value ->
            PricePoint(endDate.minusDays((values.size - 1 - index).toLong()), value)
        }

    @Test
    fun `a deep crash scores attractive or better`() {
        val values = buildList {
            repeat(300) { add(100.0) }
            repeat(120) { index -> add(100.0 - index * 0.45) }
        }
        val indicators = IndicatorEngine.compute(series(values))!!
        val result = DipScoreEngine.score(indicators, benchmark90d = 0.02, subject = "Fund")
        assertTrue("score was ${result.score}", result.score >= Level.ATTRACTIVE_SCORE)
        assertTrue(result.reasons.any { it.contains("below its rolling 52-week high") })
    }

    @Test
    fun `a steady climb is not a dip`() {
        val values = (0 until 420).map { 100.0 * (1.0 + it * 0.0009) }
        val indicators = IndicatorEngine.compute(series(values))!!
        val result = DipScoreEngine.score(indicators, benchmark90d = -0.05, subject = "Fund")
        assertTrue("score was ${result.score}", result.score < Level.ATTRACTIVE_SCORE)
    }

    @Test
    fun `missing benchmark awards the fallback share of that weight`() {
        val values = (0 until 400).map { 100.0 }
        val indicators = IndicatorEngine.compute(series(values))!!
        val withFallback = DipScoreEngine.score(indicators, benchmark90d = null, subject = "Gold")
        assertEquals(4, withFallback.components.vsBenchmark)
    }

    @Test
    fun `a calm market still earns the partial credit the reference panel gives`() {
        val values = (0 until 400).map { 100.0 }
        val indicators = IndicatorEngine.compute(series(values))!!
        val result = DipScoreEngine.score(indicators, benchmark90d = 0.0, subject = "Fund")
        assertEquals(4, result.components.momentum30d)
        assertEquals(2, result.components.correction90d)
        assertEquals(7, result.components.trend1y)
        assertEquals(3, result.components.vsBenchmark)
    }

    /** Indicator values captured from Tata Ethical on 2026-08-17, which the panel scored 26. */
    @Test
    fun `matches the reference panel on a live fund`() {
        val indicators = Indicators(
            asOf = LocalDate.of(2026, 8, 17),
            current = 428.6055,
            previous = 427.0,
            high52w = 454.7603,
            drawdown52w = 0.057513375727828414,
            return30d = 0.023454623646465222,
            return90d = 0.030289299407459058,
            return1y = 0.004000019676836164,
            volatility = 0.007401703873675372,
            recentDrawdown = 0.010858492730157154,
        )
        val result = DipScoreEngine.score(
            indicators = indicators,
            benchmark90d = 0.030558081502315648,
            subject = "Tata Ethical",
        )
        assertEquals(7, result.components.drawdown52w)
        assertEquals(4, result.components.momentum30d)
        assertEquals(2, result.components.correction90d)
        assertEquals(7, result.components.trend1y)
        assertEquals(2, result.components.volatility)
        assertEquals(4, result.components.vsBenchmark)
        assertEquals(26, result.score)
    }

    @Test
    fun `component weights sum to one hundred`() {
        val total = DipScoreEngine.WEIGHT_DRAWDOWN_52W +
            DipScoreEngine.WEIGHT_MOMENTUM_30D +
            DipScoreEngine.WEIGHT_CORRECTION_90D +
            DipScoreEngine.WEIGHT_TREND_1Y +
            DipScoreEngine.WEIGHT_VOLATILITY +
            DipScoreEngine.WEIGHT_VS_BENCHMARK
        assertEquals(100, total)
    }
}
