package app.mizan.android.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        FundEntity::class,
        FundPriceEntity::class,
        InvestmentSignalEntity::class,
        WatchlistItemEntity::class,
        MetalEntity::class,
        MetalPriceEntity::class,
        MetalSignalEntity::class,
        BenchmarkPriceEntity::class,
        NotificationLogEntity::class,
        JobRunEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MizanDatabase : RoomDatabase() {
    abstract fun fundDao(): FundDao
    abstract fun fundPriceDao(): FundPriceDao
    abstract fun signalDao(): SignalDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun metalDao(): MetalDao
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun jobRunDao(): JobRunDao

    companion object {
        const val NAME = "mizan.db"
    }
}
