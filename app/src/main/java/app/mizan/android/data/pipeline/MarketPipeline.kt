package app.mizan.android.data.pipeline

import app.mizan.android.core.IndiaClock
import app.mizan.android.data.db.BenchmarkPriceEntity
import app.mizan.android.data.db.FundEntity
import app.mizan.android.data.db.FundPriceEntity
import app.mizan.android.data.db.InvestmentSignalEntity
import app.mizan.android.data.db.METAL_GOLD
import app.mizan.android.data.db.MetalEntity
import app.mizan.android.data.db.MetalPriceEntity
import app.mizan.android.data.db.MetalSignalEntity
import app.mizan.android.data.db.MizanDatabase
import app.mizan.android.data.db.NOTIFY_FUND_DIP
import app.mizan.android.data.db.NOTIFY_GOLD_DROP
import app.mizan.android.data.db.NOTIFY_METAL_DIP
import app.mizan.android.data.db.NotificationLogEntity
import app.mizan.android.data.remote.MetalClient
import app.mizan.android.data.remote.MfapiClient
import app.mizan.android.data.remote.YahooChartClient
import app.mizan.android.data.settings.SettingsRepository
import app.mizan.android.domain.AllocationBands
import app.mizan.android.domain.DipScore
import app.mizan.android.domain.DipScoreEngine
import app.mizan.android.domain.IndicatorEngine
import app.mizan.android.domain.LastNotification
import app.mizan.android.domain.Level
import app.mizan.android.domain.NotifyRules
import app.mizan.android.domain.PricePoint
import app.mizan.android.notify.MizanNotifier
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class PipelineResult(
    val fundsFetched: Int = 0,
    val signalsWritten: Int = 0,
    val notifiesPosted: Int = 0,
    val fundNavAdvanced: Boolean = false,
    val errors: List<String> = emptyList(),
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

/**
 * The on-device replacement for a server cron: fetch, score, notify, in that order. Every step is
 * allowed to fail on its own without losing the last good Room data.
 */
@Singleton
class MarketPipeline @Inject constructor(
    private val database: MizanDatabase,
    private val mfapi: MfapiClient,
    private val yahoo: YahooChartClient,
    private val metals: MetalClient,
    private val settings: SettingsRepository,
    private val notifier: MizanNotifier,
    private val json: Json,
) {

    /** ~5 years is enough history for 52-week windows plus a meaningful missed-dip record. */
    private val historyStart: LocalDate get() = IndiaClock.today().minusYears(5)

    suspend fun backfill(onProgress: suspend (String) -> Unit): PipelineResult {
        val errors = mutableListOf<String>()
        var fundsFetched = 0
        var signalsWritten = 0
        var navAdvanced = false

        onProgress("Loading Nifty 50 history")
        val benchmark = fetchBenchmark(range = "5y", errors)

        val funds = database.fundDao().tracked()
        val fundSeries = mutableMapOf<Long, List<PricePoint>>()

        for (fund in funds) {
            onProgress("Loading ${fund.shortName}")
            val series = runCatching { mfapi.history(fund.schemeCode) }
                .onFailure { errors += "NAV history for ${fund.shortName}: ${it.message}" }
                .getOrDefault(emptyList())
                .filter { it.date >= historyStart }
            if (series.isEmpty()) continue

            persistFundPrices(fund.schemeCode, series)
            updateFundQuote(fund, series)
            fundSeries[fund.schemeCode] = series
            fundsFetched++
            navAdvanced = true
            yield()
        }

        for (fund in funds) {
            val series = fundSeries[fund.schemeCode] ?: continue
            onProgress("Scoring ${fund.shortName}")
            signalsWritten += backfillFundSignals(fund, series, benchmark)
            yield()
        }

        onProgress("Loading gold and silver history")
        signalsWritten += backfillMetals(errors, onProgress)

        // A five-year backfill must never dump five years of notifications.
        val notifies = notifyLatest(errors)

        settings.setBackfillCompleted(true)

        return PipelineResult(
            fundsFetched = fundsFetched,
            signalsWritten = signalsWritten,
            notifiesPosted = notifies,
            fundNavAdvanced = navAdvanced,
            errors = errors,
        )
    }

    suspend fun daily(): PipelineResult {
        val errors = mutableListOf<String>()
        var fundsFetched = 0
        var signalsWritten = 0
        var navAdvanced = false

        val funds = database.fundDao().tracked()
        val benchmark = fetchBenchmark(range = "1y", errors)
            .ifEmpty { database.benchmarkDao().series().map { PricePoint(it.priceDate, it.close) } }

        for (fund in funds) {
            val storedLatest = database.fundPriceDao().latestDate(fund.schemeCode)
            val fetched = runCatching { mfapi.history(fund.schemeCode) }
                .onFailure { errors += "NAV for ${fund.shortName}: ${it.message}" }
                .getOrDefault(emptyList())
            if (fetched.isEmpty()) continue

            fundsFetched++
            persistFundPrices(fund.schemeCode, fetched.filter { it.date >= historyStart })
            updateFundQuote(fund, fetched)

            val newLatest = fetched.last().date
            if (storedLatest == null || newLatest.isAfter(storedLatest)) navAdvanced = true

            val series = database.fundPriceDao().series(fund.schemeCode)
                .map { PricePoint(it.priceDate, it.nav) }
            if (scoreFundOn(fund, series, benchmark, series.last().date)) signalsWritten++
            yield()
        }

        signalsWritten += refreshMetals(errors)
        val notifies = notifyLatest(errors)

        return PipelineResult(
            fundsFetched = fundsFetched,
            signalsWritten = signalsWritten,
            notifiesPosted = notifies,
            fundNavAdvanced = navAdvanced,
            errors = errors,
        )
    }

    suspend fun latestFundNavDate(): LocalDate? = database.fundPriceDao().latestDateAnyFund()

    // --- funds -------------------------------------------------------------------------------

    private suspend fun persistFundPrices(schemeCode: Long, series: List<PricePoint>) {
        if (series.isEmpty()) return
        database.fundPriceDao().upsertAll(
            series.map { FundPriceEntity(schemeCode, it.date, it.value) }
        )
    }

    private suspend fun updateFundQuote(fund: FundEntity, series: List<PricePoint>) {
        val indicators = IndicatorEngine.compute(series) ?: return
        val growth1d = indicators.previous?.let { (indicators.current / it - 1.0) * 100.0 }
        database.fundDao().updateQuote(
            schemeCode = fund.schemeCode,
            nav = indicators.current,
            asOf = indicators.asOf,
            g1d = growth1d,
            g1m = indicators.return30d?.times(100.0),
            g1y = indicators.return1y?.times(100.0),
        )
    }

    /**
     * An unchanged NAV means a holiday or a stale publish, so it gets no score at all.
     */
    private suspend fun scoreFundOn(
        fund: FundEntity,
        series: List<PricePoint>,
        benchmark: List<PricePoint>,
        date: LocalDate,
    ): Boolean {
        val window = series.filter { !it.date.isAfter(date) }
        if (window.size < 2) return false
        if (window.last().value == window[window.size - 2].value) return false

        val indicators = IndicatorEngine.compute(window) ?: return false
        val score = DipScoreEngine.score(
            indicators = indicators,
            benchmark90d = benchmarkReturn90d(benchmark, date),
            subject = fund.shortName,
        )
        database.signalDao().upsert(score.toFundSignal(fund.schemeCode))
        return true
    }

    private suspend fun backfillFundSignals(
        fund: FundEntity,
        series: List<PricePoint>,
        benchmark: List<PricePoint>,
    ): Int {
        if (series.size < 2) return 0
        val scoreFrom = series.first().date.plusYears(1)
        val signals = mutableListOf<InvestmentSignalEntity>()
        var written = 0

        for (index in 1 until series.size) {
            val point = series[index]
            if (point.date < scoreFrom) continue
            if (point.value == series[index - 1].value) continue

            val indicators = IndicatorEngine.compute(series.subList(0, index + 1)) ?: continue
            val score = DipScoreEngine.score(
                indicators = indicators,
                benchmark90d = benchmarkReturn90d(benchmark, point.date),
                subject = fund.shortName,
            )
            signals += score.toFundSignal(fund.schemeCode)
            written++

            if (signals.size >= BATCH_SIZE) {
                database.signalDao().upsertAll(signals.toList())
                signals.clear()
                yield()
            }
        }
        if (signals.isNotEmpty()) database.signalDao().upsertAll(signals)
        return written
    }

    private fun DipScore.toFundSignal(schemeCode: Long) = InvestmentSignalEntity(
        schemeCode = schemeCode,
        signalDate = date,
        score = score,
        level = level.key,
        nav = value,
        componentsJson = json.encodeToString(components.toMap()),
        reasonsJson = json.encodeToString(reasons),
    )

    // --- benchmark ---------------------------------------------------------------------------

    private suspend fun fetchBenchmark(range: String, errors: MutableList<String>): List<PricePoint> {
        val fromYahoo = runCatching { yahoo.closes(YahooChartClient.NIFTY_50, range) }
            .getOrDefault(emptyList())
        val series = if (fromYahoo.isNotEmpty()) {
            fromYahoo
        } else {
            runCatching { mfapi.history(BENCHMARK_FALLBACK_SCHEME) }
                .onFailure { errors += "Benchmark: ${it.message}" }
                .getOrDefault(emptyList())
        }
        if (series.isEmpty()) return emptyList()

        database.benchmarkDao().upsertAll(
            series.filter { it.date >= historyStart }
                .map { BenchmarkPriceEntity(it.date, it.value) }
        )
        return database.benchmarkDao().series().map { PricePoint(it.priceDate, it.close) }
    }

    private fun benchmarkReturn90d(benchmark: List<PricePoint>, asOf: LocalDate): Double? {
        if (benchmark.isEmpty()) return null
        val window = benchmark.filter { !it.date.isAfter(asOf) }
        if (window.size < 2) return null
        // Anchored to the signal date, not to the benchmark's last close, so a lagging index feed
        // does not silently shorten the comparison window.
        return IndicatorEngine.returnSince(window, asOf.minusDays(90))
    }

    // --- metals ------------------------------------------------------------------------------

    private suspend fun refreshMetals(errors: MutableList<String>): Int {
        var written = 0
        for (metal in database.metalDao().all()) {
            val latest = runCatching {
                if (metal.id == METAL_GOLD) metals.goldLatest() else metals.silverLatest()
            }.onFailure { errors += "${metal.name} price: ${it.message}" }.getOrNull() ?: continue

            database.metalDao().upsertPrices(
                listOf(MetalPriceEntity(metal.id, latest.date, latest.value))
            )
            val series = database.metalDao().series(metal.id).map { PricePoint(it.priceDate, it.price) }
            updateMetalQuote(metal, series)
            if (scoreMetalOn(metal, series, series.last().date)) written++
        }
        return written
    }

    private suspend fun backfillMetals(
        errors: MutableList<String>,
        onProgress: suspend (String) -> Unit,
    ): Int {
        var written = 0
        for (metal in database.metalDao().all()) {
            onProgress("Loading ${metal.name} history")
            val spot = runCatching {
                if (metal.id == METAL_GOLD) metals.goldLatest() else metals.silverLatest()
            }.getOrNull()

            val history = runCatching { metals.history(metal.id, spot?.value) }
                .onFailure { errors += "${metal.name} history: ${it.message}" }
                .getOrDefault(emptyList())
                .filter { it.date >= historyStart }

            val combined = (history + listOfNotNull(spot))
                .groupBy { it.date }
                .map { (date, points) -> PricePoint(date, points.last().value) }
                .sortedBy { it.date }
            if (combined.isEmpty()) continue

            database.metalDao().upsertPrices(
                combined.map { MetalPriceEntity(metal.id, it.date, it.value) }
            )
            updateMetalQuote(metal, combined)

            val scoreFrom = combined.first().date.plusYears(1)
            for (index in 1 until combined.size) {
                val point = combined[index]
                if (point.date < scoreFrom) continue
                if (point.value == combined[index - 1].value) continue
                if (scoreMetal(metal, combined.subList(0, index + 1))) written++
                if (index % BATCH_SIZE == 0) yield()
            }
        }
        return written
    }

    private suspend fun updateMetalQuote(metal: MetalEntity, series: List<PricePoint>) {
        val indicators = IndicatorEngine.compute(series) ?: return
        database.metalDao().updateQuote(
            id = metal.id,
            price = indicators.current,
            asOf = indicators.asOf,
            g1d = indicators.previous?.let { (indicators.current / it - 1.0) * 100.0 },
            g1m = indicators.return30d?.times(100.0),
            g1y = indicators.return1y?.times(100.0),
        )
    }

    private suspend fun scoreMetalOn(
        metal: MetalEntity,
        series: List<PricePoint>,
        date: LocalDate,
    ): Boolean {
        val window = series.filter { !it.date.isAfter(date) }
        if (window.size < 2) return false
        if (window.last().value == window[window.size - 2].value) return false
        return scoreMetal(metal, window)
    }

    private suspend fun scoreMetal(metal: MetalEntity, window: List<PricePoint>): Boolean {
        val indicators = IndicatorEngine.compute(window) ?: return false
        // Metals have no fund benchmark, so the vs-Nifty weight falls back inside the engine.
        val score = DipScoreEngine.score(indicators, benchmark90d = null, subject = metal.name)
        database.metalDao().upsertSignal(
            MetalSignalEntity(
                metalId = metal.id,
                signalDate = score.date,
                score = score.score,
                level = score.level.key,
                price = score.value,
                componentsJson = json.encodeToString(score.components.toMap()),
                reasonsJson = json.encodeToString(score.reasons),
            )
        )
        return true
    }

    // --- notifications ----------------------------------------------------------------------

    private suspend fun notifyLatest(errors: MutableList<String>): Int {
        val prefs = settings.current()
        if (!prefs.notificationsEnabled || !notifier.canPost()) return 0

        var posted = 0
        val today = IndiaClock.today()

        if (prefs.notifyFundDips) {
            for (item in database.watchlistDao().all()) {
                val fund = database.fundDao().byCode(item.schemeCode) ?: continue
                val signal = database.signalDao().latestForFund(item.schemeCode) ?: continue
                if (signal.score < Level.ATTRACTIVE_SCORE) continue
                if (signal.signalDate < today.minusDays(STALE_SIGNAL_DAYS)) continue

                val last = database.notificationLogDao()
                    .latest(NOTIFY_FUND_DIP, item.schemeCode.toString())
                    ?.let { LastNotification(it.sentOn, Level.fromKey(it.level ?: "")) }
                if (!NotifyRules.shouldNotifyDip(signal.score, signal.signalDate, last)) continue

                val level = Level.fromKey(signal.level)
                val (title, body) = notifier.notifyDip(
                    route = "fund/${fund.schemeCode}",
                    notificationId = MizanNotifier.fundNotificationId(fund.schemeCode),
                    subjectName = fund.shortName,
                    score = signal.score,
                    level = level,
                    suggestedRupees = AllocationBands.suggestedRupees(
                        signal.score,
                        prefs.availableLumpsum,
                    ),
                )
                logNotification(
                    NOTIFY_FUND_DIP,
                    fund.schemeCode.toString(),
                    title,
                    body,
                    signal.signalDate,
                    signal.score,
                    level.key,
                )
                posted++
            }
        }

        if (prefs.notifyMetalDips) {
            for (metal in database.metalDao().all()) {
                val signal = database.metalDao().latestSignal(metal.id) ?: continue
                if (signal.score < Level.ATTRACTIVE_SCORE) continue
                if (signal.signalDate < today.minusDays(STALE_SIGNAL_DAYS)) continue

                val last = database.notificationLogDao().latest(NOTIFY_METAL_DIP, metal.id)
                    ?.let { LastNotification(it.sentOn, Level.fromKey(it.level ?: "")) }
                if (!NotifyRules.shouldNotifyDip(signal.score, signal.signalDate, last)) continue

                val level = Level.fromKey(signal.level)
                val (title, body) = notifier.notifyDip(
                    route = "metal/${metal.id}",
                    notificationId = MizanNotifier.metalNotificationId(metal.id),
                    subjectName = metal.name,
                    score = signal.score,
                    level = level,
                    suggestedRupees = AllocationBands.suggestedRupees(
                        signal.score,
                        prefs.availableLumpsum,
                    ),
                )
                logNotification(
                    NOTIFY_METAL_DIP,
                    metal.id,
                    title,
                    body,
                    signal.signalDate,
                    signal.score,
                    level.key,
                )
                posted++
            }
        }

        if (prefs.notifyGoldDrop) {
            runCatching { posted += notifyGoldDrop(today) }
                .onFailure { errors += "Gold drop check: ${it.message}" }
        }

        notifier.postGroupSummary(posted)
        return posted
    }

    /** Independent of the 0-100 score: a plain rupee fall off the recent peak. */
    private suspend fun notifyGoldDrop(today: LocalDate): Int {
        val gold = database.metalDao().byId(METAL_GOLD) ?: return 0
        if (gold.dropThresholdRupees <= 0.0) return 0

        val series = database.metalDao().series(METAL_GOLD)
        val window = series.filter { it.priceDate >= today.minusDays(NotifyRules.GOLD_PEAK_WINDOW_DAYS) }
        val current = window.lastOrNull() ?: return 0
        val peak = window.maxByOrNull { it.price } ?: return 0
        val drop = peak.price - current.price
        if (drop < gold.dropThresholdRupees) return 0

        val last = database.notificationLogDao().latest(NOTIFY_GOLD_DROP, METAL_GOLD)
            ?.let { LastNotification(it.sentOn, Level.UNATTRACTIVE) }
        if (!NotifyRules.shouldNotifyDrop(drop, current.priceDate, last)) return 0

        val (title, body) = notifier.notifyGoldDrop(drop, peak.price, peak.priceDate)
        logNotification(NOTIFY_GOLD_DROP, METAL_GOLD, title, body, current.priceDate, null, null)
        return 1
    }

    private suspend fun logNotification(
        type: String,
        targetId: String,
        title: String,
        body: String,
        on: LocalDate,
        score: Int?,
        level: String?,
    ) {
        database.notificationLogDao().insert(
            NotificationLogEntity(
                type = type,
                targetId = targetId,
                title = title,
                body = body,
                sentAt = IndiaClock.nowMillis(),
                sentOn = on,
                score = score,
                level = level,
            )
        )
    }

    private companion object {
        const val BENCHMARK_FALLBACK_SCHEME = 119598L
        const val BATCH_SIZE = 200
        const val STALE_SIGNAL_DAYS = 4L
    }
}
