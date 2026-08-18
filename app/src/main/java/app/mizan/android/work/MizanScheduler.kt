package app.mizan.android.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.mizan.android.core.IndiaClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * WorkManager is the scheduler. It is not second-accurate and OEM battery managers make that worse,
 * so the requirement is "daily after NAVs publish" plus a morning catch-up -- not 21:00:00.000.
 */
object MizanScheduler {

    private val DAILY_WINDOW: LocalTime = LocalTime.of(21, 0)
    private val MORNING_WINDOW: LocalTime = LocalTime.of(7, 0)

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresStorageNotLow(true)
        .build()

    fun startBackfill(context: Context) {
        val request = OneTimeWorkRequestBuilder<BackfillWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackfillWorker.NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun reloadHistory(context: Context) {
        val request = OneTimeWorkRequestBuilder<BackfillWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(BackfillWorker.NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyPipelineWorker>(
            24, TimeUnit.HOURS,
            2, TimeUnit.HOURS,
        )
            .setConstraints(networkConstraints)
            .setInitialDelay(IndiaClock.millisUntil(DAILY_WINDOW), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyPipelineWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleMorningRetry(context: Context) {
        val request = OneTimeWorkRequestBuilder<RetryMorningWorker>()
            .setConstraints(networkConstraints)
            .setInitialDelay(IndiaClock.millisUntil(MORNING_WINDOW), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            RetryMorningWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ManualRefreshWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ManualRefreshWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun observeBackfillRunning(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(BackfillWorker.NAME)
            .map { infos -> infos.any { !it.state.isFinished } }

    fun observeRefreshRunning(context: Context): Flow<Boolean> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(ManualRefreshWorker.UNIQUE_NAME)
            .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } }
}
