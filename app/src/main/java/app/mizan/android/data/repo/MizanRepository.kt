package app.mizan.android.data.repo

import app.mizan.android.core.IndiaClock
import app.mizan.android.data.db.FundEntity
import app.mizan.android.data.db.InvestmentSignalEntity
import app.mizan.android.data.db.JOB_STATUS_SUCCESS
import app.mizan.android.data.db.JobRunEntity
import app.mizan.android.data.db.METAL_GOLD
import app.mizan.android.data.db.MetalEntity
import app.mizan.android.data.db.MetalSignalEntity
import app.mizan.android.data.db.MizanDatabase
import app.mizan.android.data.db.WatchlistItemEntity
import app.mizan.android.data.settings.MizanSettings
import app.mizan.android.data.settings.SettingsRepository
import app.mizan.android.domain.AllocationBands
import app.mizan.android.domain.AttractiveDay
import app.mizan.android.domain.Level
import app.mizan.android.domain.MissedDeploy
import app.mizan.android.domain.MissedOpportunity
import app.mizan.android.domain.MissedTotals
import app.mizan.android.domain.NotifyRules
import app.mizan.android.domain.PricePoint
import app.mizan.android.domain.ScoreComponents
import app.mizan.android.domain.SipAllotment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

data class FundRow(
    val fund: FundEntity,
    val latestSignal: InvestmentSignalEntity?,
    val watchlisted: Boolean,
    val sipAmount: Double?,
    val sipDayOfMonth: Int?,
) {
    val score: Int? get() = latestSignal?.score
    val level: Level? get() = latestSignal?.let { Level.fromKey(it.level) }
    val attractiveNow: Boolean get() = (score ?: 0) >= Level.ATTRACTIVE_SCORE
    val isScored: Boolean get() = latestSignal != null

    fun suggestedRupees(pool: Double): Double? =
        score?.let { AllocationBands.suggestedRupees(it, pool) }
}

data class MetalRow(
    val metal: MetalEntity,
    val latestSignal: MetalSignalEntity?,
) {
    val score: Int? get() = latestSignal?.score
    val level: Level? get() = latestSignal?.let { Level.fromKey(it.level) }
}

data class NextSip(
    val fundName: String,
    val schemeCode: Long,
    val amount: Double,
    val debitDay: Int,
    val debitDate: LocalDate,
    val allotmentDate: LocalDate,
    val estimatedAllotmentNav: Double?,
    val latestNav: Double?,
)

data class LastJob(
    val name: String,
    val at: LocalDateTime?,
    val status: String,
    val error: String?,
    val fundsFetched: Int,
    val signalsWritten: Int,
    val notifiesPosted: Int,
    val durationSeconds: Long?,
) {
    val succeeded: Boolean get() = status == JOB_STATUS_SUCCESS
}

data class HomeState(
    val trackedFunds: Int = 0,
    val watchlistCount: Int = 0,
    val signalsLast7Days: Int = 0,
    val gold: MetalRow? = null,
    val silver: MetalRow? = null,
    val watchlistToday: List<FundRow> = emptyList(),
    val missedTotals: MissedTotals? = null,
    val nextSip: NextSip? = null,
    val lastJob: LastJob? = null,
    val stale: Boolean = false,
    val settings: MizanSettings = MizanSettings(),
)

data class MissedState(
    val today: List<FundRow> = emptyList(),
    val deploys: List<MissedDeploy> = emptyList(),
    val totals: MissedTotals = MissedTotals(0, 0.0, 0.0, 0.0, 0.0),
    val watchlistEmpty: Boolean = true,
    val settings: MizanSettings = MizanSettings(),
)

data class SignalView(
    val date: LocalDate,
    val score: Int,
    val level: Level,
    val value: Double,
    val components: ScoreComponents,
    val reasons: List<String>,
)

data class FundDetailState(
    val fund: FundEntity? = null,
    val navSeries: List<PricePoint> = emptyList(),
    val signals: List<SignalView> = emptyList(),
    val watchlisted: Boolean = false,
    val sipAmount: Double? = null,
    val sipDayOfMonth: Int? = null,
    val nextSip: NextSip? = null,
    val settings: MizanSettings = MizanSettings(),
) {
    val latest: SignalView? get() = signals.firstOrNull()
}

data class MetalDetailState(
    val metal: MetalEntity? = null,
    val priceSeries: List<PricePoint> = emptyList(),
    val signals: List<SignalView> = emptyList(),
    val peak: Double? = null,
    val peakDate: LocalDate? = null,
    val dropRupees: Double? = null,
    val thresholdRupees: Double = 0.0,
)

/**
 * Read side for the UI. Everything comes from Room, so the app opens fully offline once the first
 * backfill has landed.
 */
@Singleton
class MizanRepository @Inject constructor(
    private val database: MizanDatabase,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
) {

    val settings: Flow<MizanSettings> = settingsRepository.settings

    fun observeFundRows(): Flow<List<FundRow>> = combine(
        database.fundDao().observeActive(),
        database.signalDao().observeLatestPerFund(),
        database.watchlistDao().observeAll(),
    ) { funds, signals, watchlist ->
        val signalByFund = signals.associateBy { it.schemeCode }
        val watchByFund = watchlist.associateBy { it.schemeCode }
        funds.map { fund ->
            val watch = watchByFund[fund.schemeCode]
            FundRow(
                fund = fund,
                latestSignal = signalByFund[fund.schemeCode],
                watchlisted = watch != null,
                sipAmount = watch?.sipAmount,
                sipDayOfMonth = watch?.sipDayOfMonth,
            )
        }
    }

    fun observeMetalRows(): Flow<List<MetalRow>> = combine(
        database.metalDao().observeAll(),
        database.metalDao().observeLatestPerMetal(),
    ) { metals, signals ->
        val byId = signals.associateBy { it.metalId }
        metals.map { MetalRow(it, byId[it.id]) }
    }

    fun observeHome(): Flow<HomeState> = combine(
        observeFundRows(),
        observeMetalRows(),
        database.signalDao().observeRecentAttractiveCount(
            Level.ATTRACTIVE_SCORE,
            IndiaClock.today().minusDays(7),
        ),
        database.jobRunDao().observeLast(),
        settingsRepository.settings,
    ) { funds, metals, recentSignals, lastJob, prefs ->
        val watchlisted = funds.filter { it.watchlisted }
        HomeState(
            trackedFunds = funds.count { it.fund.trackingEnabled },
            watchlistCount = watchlisted.size,
            signalsLast7Days = recentSignals,
            gold = metals.firstOrNull { it.metal.id == METAL_GOLD },
            silver = metals.firstOrNull { it.metal.id != METAL_GOLD },
            watchlistToday = watchlisted,
            nextSip = nextSip(watchlisted),
            lastJob = lastJob?.toLastJob(),
            stale = isStale(lastJob),
            settings = prefs,
        )
    }

    fun observeMissed(): Flow<MissedState> = combine(
        observeFundRows(),
        database.signalDao().observeAttractive(Level.ATTRACTIVE_SCORE),
        settingsRepository.settings,
    ) { funds, attractive, prefs ->
        val watchlisted = funds.filter { it.watchlisted }
        val byFund = attractive.groupBy { it.schemeCode }

        val deploys = watchlisted.flatMap { row ->
            val days = byFund[row.fund.schemeCode].orEmpty().map { signal ->
                AttractiveDay(
                    date = signal.signalDate,
                    score = signal.score,
                    level = Level.fromKey(signal.level),
                    value = signal.nav,
                )
            }
            MissedOpportunity.build(
                targetId = row.fund.schemeCode.toString(),
                name = row.fund.shortName,
                attractiveDays = MissedOpportunity.attractiveOnly(days),
                navNow = row.fund.lastNav,
                availableLumpsum = prefs.availableLumpsum,
                whatIfAmount = prefs.whatIfAmount,
            )
        }

        MissedState(
            today = watchlisted,
            deploys = MissedOpportunity.sortedByDateDesc(deploys),
            totals = MissedOpportunity.totals(deploys),
            watchlistEmpty = watchlisted.isEmpty(),
            settings = prefs,
        )
    }

    fun observeHomeWithMissed(): Flow<HomeState> = combine(
        observeHome(),
        observeMissed(),
    ) { home, missed -> home.copy(missedTotals = missed.totals) }

    fun observeFundDetail(schemeCode: Long): Flow<FundDetailState> = combine(
        database.fundDao().observeOne(schemeCode),
        database.fundPriceDao().observeSeries(schemeCode),
        database.signalDao().observeForFund(schemeCode),
        database.watchlistDao().observeOne(schemeCode),
        settingsRepository.settings,
    ) { fund, prices, signals, watch, prefs ->
        val series = prices.map { PricePoint(it.priceDate, it.nav) }
        FundDetailState(
            fund = fund,
            navSeries = series,
            signals = signals.map { it.toView() },
            watchlisted = watch != null,
            sipAmount = watch?.sipAmount,
            sipDayOfMonth = watch?.sipDayOfMonth,
            nextSip = fund?.let { nextSipFor(it, watch, series) },
            settings = prefs,
        )
    }

    fun observeMetalDetail(metalId: String): Flow<MetalDetailState> = combine(
        database.metalDao().observeOne(metalId),
        database.metalDao().observeSeries(metalId),
        database.metalDao().observeSignals(metalId),
    ) { metal, prices, signals ->
        val series = prices.map { PricePoint(it.priceDate, it.price) }
        val window = series.filter {
            it.date >= IndiaClock.today().minusDays(NotifyRules.GOLD_PEAK_WINDOW_DAYS)
        }
        val peak = window.maxByOrNull { it.value }
        MetalDetailState(
            metal = metal,
            priceSeries = series,
            signals = signals.map { it.toView() },
            peak = peak?.value,
            peakDate = peak?.date,
            dropRupees = peak?.let { p -> series.lastOrNull()?.let { p.value - it.value } },
            thresholdRupees = metal?.dropThresholdRupees ?: 0.0,
        )
    }

    fun observeRecentJobs(): Flow<List<LastJob>> =
        database.jobRunDao().observeRecent().map { runs -> runs.map { it.toLastJob() } }

    suspend fun addToWatchlist(schemeCode: Long) {
        val existing = database.watchlistDao().all().firstOrNull { it.schemeCode == schemeCode }
        if (existing != null) return
        database.watchlistDao().upsert(
            WatchlistItemEntity(schemeCode = schemeCode, addedAt = IndiaClock.nowMillis())
        )
    }

    suspend fun removeFromWatchlist(schemeCode: Long) = database.watchlistDao().remove(schemeCode)

    suspend fun toggleWatchlist(schemeCode: Long, watchlisted: Boolean) {
        if (watchlisted) removeFromWatchlist(schemeCode) else addToWatchlist(schemeCode)
    }

    suspend fun saveSip(schemeCode: Long, amount: Double?, dayOfMonth: Int?) {
        val existing = database.watchlistDao().all().firstOrNull { it.schemeCode == schemeCode }
        database.watchlistDao().upsert(
            WatchlistItemEntity(
                schemeCode = schemeCode,
                sipAmount = amount,
                sipDayOfMonth = dayOfMonth,
                addedAt = existing?.addedAt ?: IndiaClock.nowMillis(),
            )
        )
    }

    private fun nextSip(rows: List<FundRow>): NextSip? = rows
        .filter { it.sipAmount != null && it.sipDayOfMonth != null }
        .mapNotNull { row ->
            val debitDate = SipAllotment.nextDebitDate(row.sipDayOfMonth!!, IndiaClock.today())
            NextSip(
                fundName = row.fund.shortName,
                schemeCode = row.fund.schemeCode,
                amount = row.sipAmount!!,
                debitDay = row.sipDayOfMonth,
                debitDate = debitDate,
                allotmentDate = SipAllotment.estimatedAllotmentDate(debitDate),
                estimatedAllotmentNav = null,
                latestNav = row.fund.lastNav,
            )
        }
        .minByOrNull { it.debitDate }

    private fun nextSipFor(
        fund: FundEntity,
        watch: WatchlistItemEntity?,
        series: List<PricePoint>,
    ): NextSip? {
        val amount = watch?.sipAmount ?: return null
        val day = watch.sipDayOfMonth ?: return null
        val debitDate = SipAllotment.nextDebitDate(day, IndiaClock.today())
        val allotmentDate = SipAllotment.estimatedAllotmentDate(debitDate)
        // Allotment NAV is the NAV on the allotment date, which for a future debit is unknown --
        // the best available proxy is the last published NAV.
        val allotmentNav = series.lastOrNull { !it.date.isAfter(allotmentDate) }?.value
        return NextSip(
            fundName = fund.shortName,
            schemeCode = fund.schemeCode,
            amount = amount,
            debitDay = day,
            debitDate = debitDate,
            allotmentDate = allotmentDate,
            estimatedAllotmentNav = allotmentNav,
            latestNav = fund.lastNav,
        )
    }

    private fun isStale(lastJob: JobRunEntity?): Boolean {
        val finished = lastJob?.finishedAt ?: return true
        if (lastJob.status != JOB_STATUS_SUCCESS) return true
        val hours = (IndiaClock.nowMillis() - finished) / 3_600_000L
        return hours > STALE_AFTER_HOURS
    }

    private fun JobRunEntity.toLastJob() = LastJob(
        name = jobName,
        at = finishedAt?.let { IndiaClock.toLocalDateTime(it) }
            ?: IndiaClock.toLocalDateTime(startedAt),
        status = status,
        error = error,
        fundsFetched = fundsFetched,
        signalsWritten = signalsWritten,
        notifiesPosted = notifiesPosted,
        durationSeconds = finishedAt?.let { (it - startedAt) / 1000 },
    )

    private fun InvestmentSignalEntity.toView() = SignalView(
        date = signalDate,
        score = score,
        level = Level.fromKey(level),
        value = nav,
        components = decodeComponents(componentsJson),
        reasons = decodeReasons(reasonsJson),
    )

    private fun MetalSignalEntity.toView() = SignalView(
        date = signalDate,
        score = score,
        level = Level.fromKey(level),
        value = price,
        components = decodeComponents(componentsJson),
        reasons = decodeReasons(reasonsJson),
    )

    private fun decodeComponents(raw: String): ScoreComponents =
        runCatching { ScoreComponents.fromMap(json.decodeFromString<Map<String, Double>>(raw)) }
            .getOrDefault(ScoreComponents(0, 0, 0, 0, 0, 0))

    private fun decodeReasons(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())

    private companion object {
        const val STALE_AFTER_HOURS = 36L
    }
}
