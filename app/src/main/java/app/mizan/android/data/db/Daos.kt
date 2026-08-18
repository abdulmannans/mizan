package app.mizan.android.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FundDao {
    @Upsert
    suspend fun upsert(funds: List<FundEntity>)

    @Query("SELECT * FROM funds WHERE active = 1 ORDER BY name")
    fun observeActive(): Flow<List<FundEntity>>

    @Query("SELECT * FROM funds WHERE active = 1 AND trackingEnabled = 1 ORDER BY name")
    suspend fun tracked(): List<FundEntity>

    @Query("SELECT * FROM funds WHERE schemeCode = :schemeCode")
    fun observeOne(schemeCode: Long): Flow<FundEntity?>

    @Query("SELECT * FROM funds WHERE schemeCode = :schemeCode")
    suspend fun byCode(schemeCode: Long): FundEntity?

    @Query("SELECT COUNT(*) FROM funds WHERE active = 1")
    suspend fun countActive(): Int

    @Query(
        """
        UPDATE funds SET lastNav = :nav, navAsOf = :asOf, growth1d = :g1d,
        growth1m = :g1m, growth1y = :g1y WHERE schemeCode = :schemeCode
        """
    )
    suspend fun updateQuote(
        schemeCode: Long,
        nav: Double,
        asOf: LocalDate,
        g1d: Double?,
        g1m: Double?,
        g1y: Double?,
    )
}

@Dao
interface FundPriceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(prices: List<FundPriceEntity>)

    @Query("SELECT * FROM fund_prices WHERE schemeCode = :schemeCode ORDER BY priceDate")
    suspend fun series(schemeCode: Long): List<FundPriceEntity>

    @Query("SELECT * FROM fund_prices WHERE schemeCode = :schemeCode ORDER BY priceDate")
    fun observeSeries(schemeCode: Long): Flow<List<FundPriceEntity>>

    @Query("SELECT MAX(priceDate) FROM fund_prices WHERE schemeCode = :schemeCode")
    suspend fun latestDate(schemeCode: Long): LocalDate?

    @Query("SELECT MAX(priceDate) FROM fund_prices")
    suspend fun latestDateAnyFund(): LocalDate?

    @Query("SELECT COUNT(*) FROM fund_prices")
    suspend fun count(): Int

    @Query("DELETE FROM fund_prices")
    suspend fun clear()
}

@Dao
interface SignalDao {
    @Upsert
    suspend fun upsert(signal: InvestmentSignalEntity)

    @Upsert
    suspend fun upsertAll(signals: List<InvestmentSignalEntity>)

    @Query("SELECT * FROM investment_signals WHERE schemeCode = :schemeCode ORDER BY signalDate DESC")
    fun observeForFund(schemeCode: Long): Flow<List<InvestmentSignalEntity>>

    @Query("SELECT * FROM investment_signals WHERE schemeCode = :schemeCode ORDER BY signalDate DESC LIMIT 1")
    suspend fun latestForFund(schemeCode: Long): InvestmentSignalEntity?

    @Query(
        """
        SELECT s.* FROM investment_signals AS s
        JOIN (
            SELECT schemeCode, MAX(signalDate) AS maxDate FROM investment_signals GROUP BY schemeCode
        ) AS latest ON s.schemeCode = latest.schemeCode AND s.signalDate = latest.maxDate
        """
    )
    fun observeLatestPerFund(): Flow<List<InvestmentSignalEntity>>

    @Query("SELECT * FROM investment_signals WHERE schemeCode = :schemeCode AND score >= :minScore ORDER BY signalDate")
    suspend fun attractiveForFund(schemeCode: Long, minScore: Int): List<InvestmentSignalEntity>

    @Query("SELECT * FROM investment_signals WHERE score >= :minScore ORDER BY signalDate")
    fun observeAttractive(minScore: Int): Flow<List<InvestmentSignalEntity>>

    @Query("SELECT COUNT(*) FROM investment_signals WHERE score >= :minScore AND signalDate >= :since")
    fun observeRecentAttractiveCount(minScore: Int, since: LocalDate): Flow<Int>

    @Query("DELETE FROM investment_signals")
    suspend fun clear()
}

@Dao
interface WatchlistDao {
    @Upsert
    suspend fun upsert(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist WHERE schemeCode = :schemeCode")
    suspend fun remove(schemeCode: Long)

    @Query("SELECT * FROM watchlist")
    fun observeAll(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist")
    suspend fun all(): List<WatchlistItemEntity>

    @Query("SELECT * FROM watchlist WHERE schemeCode = :schemeCode")
    fun observeOne(schemeCode: Long): Flow<WatchlistItemEntity?>
}

@Dao
interface MetalDao {
    @Upsert
    suspend fun upsertMetals(metals: List<MetalEntity>)

    @Query("SELECT * FROM metals ORDER BY id")
    fun observeAll(): Flow<List<MetalEntity>>

    @Query("SELECT * FROM metals ORDER BY id")
    suspend fun all(): List<MetalEntity>

    @Query("SELECT * FROM metals WHERE id = :id")
    fun observeOne(id: String): Flow<MetalEntity?>

    @Query("SELECT * FROM metals WHERE id = :id")
    suspend fun byId(id: String): MetalEntity?

    @Query(
        """
        UPDATE metals SET lastPrice = :price, priceAsOf = :asOf, growth1d = :g1d,
        growth1m = :g1m, growth1y = :g1y WHERE id = :id
        """
    )
    suspend fun updateQuote(
        id: String,
        price: Double,
        asOf: LocalDate,
        g1d: Double?,
        g1m: Double?,
        g1y: Double?,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrices(prices: List<MetalPriceEntity>)

    @Query("SELECT * FROM metal_prices WHERE metalId = :metalId ORDER BY priceDate")
    suspend fun series(metalId: String): List<MetalPriceEntity>

    @Query("SELECT * FROM metal_prices WHERE metalId = :metalId ORDER BY priceDate")
    fun observeSeries(metalId: String): Flow<List<MetalPriceEntity>>

    @Upsert
    suspend fun upsertSignal(signal: MetalSignalEntity)

    @Query("SELECT * FROM metal_signals WHERE metalId = :metalId ORDER BY signalDate DESC")
    fun observeSignals(metalId: String): Flow<List<MetalSignalEntity>>

    @Query("SELECT * FROM metal_signals WHERE metalId = :metalId ORDER BY signalDate DESC LIMIT 1")
    suspend fun latestSignal(metalId: String): MetalSignalEntity?

    @Query(
        """
        SELECT s.* FROM metal_signals AS s
        JOIN (
            SELECT metalId, MAX(signalDate) AS maxDate FROM metal_signals GROUP BY metalId
        ) AS latest ON s.metalId = latest.metalId AND s.signalDate = latest.maxDate
        """
    )
    fun observeLatestPerMetal(): Flow<List<MetalSignalEntity>>

    @Query("DELETE FROM metal_prices")
    suspend fun clearPrices()

    @Query("DELETE FROM metal_signals")
    suspend fun clearSignals()
}

@Dao
interface BenchmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(prices: List<BenchmarkPriceEntity>)

    @Query("SELECT * FROM benchmark_prices ORDER BY priceDate")
    suspend fun series(): List<BenchmarkPriceEntity>

    @Query("SELECT MAX(priceDate) FROM benchmark_prices")
    suspend fun latestDate(): LocalDate?

    @Query("DELETE FROM benchmark_prices")
    suspend fun clear()
}

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(log: NotificationLogEntity)

    @Query(
        """
        SELECT * FROM notification_log WHERE type = :type AND targetId = :targetId
        ORDER BY sentAt DESC LIMIT 1
        """
    )
    suspend fun latest(type: String, targetId: String): NotificationLogEntity?

    @Query("SELECT * FROM notification_log ORDER BY sentAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_log WHERE sentOn >= :since")
    suspend fun countSince(since: LocalDate): Int
}

@Dao
interface JobRunDao {
    @Insert
    suspend fun insert(run: JobRunEntity): Long

    @Update
    suspend fun update(run: JobRunEntity)

    @Query("SELECT * FROM job_runs WHERE id = :id")
    suspend fun byId(id: Long): JobRunEntity?

    @Query("SELECT * FROM job_runs ORDER BY startedAt DESC LIMIT 1")
    fun observeLast(): Flow<JobRunEntity?>

    @Query("SELECT * FROM job_runs WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    suspend fun lastWithStatus(status: String): JobRunEntity?

    @Query("SELECT * FROM job_runs ORDER BY startedAt DESC LIMIT 20")
    fun observeRecent(): Flow<List<JobRunEntity>>
}
