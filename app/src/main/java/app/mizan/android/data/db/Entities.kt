package app.mizan.android.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

const val SHARIAH_UNREVIEWED = "unreviewed"
const val SHARIAH_VERIFIED = "verified_compliant"
const val SHARIAH_NON_COMPLIANT = "non_compliant"
const val SHARIAH_REJECTED = "rejected"

const val METAL_GOLD = "gold"
const val METAL_SILVER = "silver"

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey val schemeCode: Long,
    val name: String,
    val shortName: String,
    val amc: String,
    val isin: String?,
    val mandate: String,
    val catalogAsOf: String,
    val shariahStatus: String,
    val shariahNotes: String?,
    val active: Boolean,
    val trackingEnabled: Boolean,
    val lastNav: Double? = null,
    val navAsOf: LocalDate? = null,
    val growth1d: Double? = null,
    val growth1m: Double? = null,
    val growth1y: Double? = null,
)

@Entity(
    tableName = "fund_prices",
    primaryKeys = ["schemeCode", "priceDate"],
    indices = [Index("schemeCode")],
)
data class FundPriceEntity(
    val schemeCode: Long,
    val priceDate: LocalDate,
    val nav: Double,
)

@Entity(
    tableName = "investment_signals",
    primaryKeys = ["schemeCode", "signalDate"],
    indices = [Index("schemeCode"), Index("score")],
)
data class InvestmentSignalEntity(
    val schemeCode: Long,
    val signalDate: LocalDate,
    val score: Int,
    val level: String,
    val nav: Double,
    val componentsJson: String,
    val reasonsJson: String,
)

@Entity(tableName = "watchlist")
data class WatchlistItemEntity(
    @PrimaryKey val schemeCode: Long,
    val addedAt: Long,
)

@Entity(tableName = "metals")
data class MetalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val unit: String,
    val dropThresholdRupees: Double,
    val lastPrice: Double? = null,
    val priceAsOf: LocalDate? = null,
    val growth1d: Double? = null,
    val growth1m: Double? = null,
    val growth1y: Double? = null,
)

@Entity(
    tableName = "metal_prices",
    primaryKeys = ["metalId", "priceDate"],
    indices = [Index("metalId")],
)
data class MetalPriceEntity(
    val metalId: String,
    val priceDate: LocalDate,
    val price: Double,
)

@Entity(
    tableName = "metal_signals",
    primaryKeys = ["metalId", "signalDate"],
    indices = [Index("metalId")],
)
data class MetalSignalEntity(
    val metalId: String,
    val signalDate: LocalDate,
    val score: Int,
    val level: String,
    val price: Double,
    val componentsJson: String,
    val reasonsJson: String,
)

@Entity(tableName = "benchmark_prices")
data class BenchmarkPriceEntity(
    @PrimaryKey val priceDate: LocalDate,
    val close: Double,
)

const val NOTIFY_FUND_DIP = "fund_dip"
const val NOTIFY_METAL_DIP = "metal_dip"
const val NOTIFY_GOLD_DROP = "gold_drop"

@Entity(tableName = "notification_log", indices = [Index("type"), Index("targetId")])
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val targetId: String,
    val title: String,
    val body: String,
    val sentAt: Long,
    val sentOn: LocalDate,
    val score: Int?,
    val level: String?,
)

const val JOB_STATUS_RUNNING = "running"
const val JOB_STATUS_SUCCESS = "success"
const val JOB_STATUS_PARTIAL = "partial"
const val JOB_STATUS_FAILED = "failed"

@Entity(tableName = "job_runs")
data class JobRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobName: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val status: String,
    val error: String? = null,
    val fundsFetched: Int = 0,
    val signalsWritten: Int = 0,
    val notifiesPosted: Int = 0,
)
