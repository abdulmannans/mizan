package app.mizan.android.work

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import app.mizan.android.core.IndiaClock
import app.mizan.android.data.catalog.CatalogSeeder
import app.mizan.android.data.pipeline.MarketPipeline
import app.mizan.android.notify.MizanNotifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * First run and "Reload history". Runs as a foreground service because a five-year backfill is too
 * long to survive as a plain background job, and Missed opportunities is empty without it.
 */
@HiltWorker
class BackfillWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val seeder: CatalogSeeder,
    private val pipeline: MarketPipeline,
    private val recorder: JobRunRecorder,
    private val notifier: MizanNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(
        "Mizan is loading history (this can take a few minutes)."
    )

    override suspend fun doWork(): Result {
        val runId = recorder.start(NAME)
        return try {
            setForeground(getForegroundInfo())
            seeder.seed()
            val result = pipeline.backfill { progress ->
                runCatching { setForeground(foregroundInfo(progress)) }
            }
            recorder.finish(runId, result)
            MizanScheduler.scheduleDaily(applicationContext)
            Result.success()
        } catch (error: Exception) {
            recorder.fail(runId, error)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private fun foregroundInfo(text: String): ForegroundInfo = ForegroundInfo(
        MizanNotifier.ID_BACKFILL_FOREGROUND,
        notifier.jobProgressNotification(text),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
    )

    companion object {
        const val NAME = "BackfillWorker"
        private const val MAX_ATTEMPTS = 3
    }
}

/** The daily "cron", targeting the window after NAVs publish. */
@HiltWorker
class DailyPipelineWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: MarketPipeline,
    private val recorder: JobRunRecorder,
    private val notifier: MizanNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val runId = recorder.start(NAME)
        return try {
            val result = pipeline.daily()
            recorder.finish(runId, result)

            // No fresh NAV date means the AMCs have not published yet: try again in the morning
            // rather than burning retries tonight.
            if (!result.fundNavAdvanced) {
                MizanScheduler.scheduleMorningRetry(applicationContext)
            }
            if (result.fundsFetched == 0 && result.hasErrors) notifier.notifyJobFailure()
            Result.success()
        } catch (error: Exception) {
            recorder.fail(runId, error)
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val NAME = "DailyPipelineWorker"
        const val UNIQUE_NAME = "mizan.daily.pipeline"
        private const val MAX_ATTEMPTS = 3
    }
}

/** Morning catch-up, only worth running while yesterday's NAV is still missing. */
@HiltWorker
class RetryMorningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: MarketPipeline,
    private val recorder: JobRunRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val latest = pipeline.latestFundNavDate()
        val yesterday = IndiaClock.today().minusDays(1)
        if (latest != null && !latest.isBefore(yesterday)) return Result.success()

        val runId = recorder.start(NAME)
        return try {
            recorder.finish(runId, pipeline.daily())
            Result.success()
        } catch (error: Exception) {
            recorder.fail(runId, error)
            Result.failure()
        }
    }

    companion object {
        const val NAME = "RetryMorningWorker"
        const val UNIQUE_NAME = "mizan.morning.retry"
    }
}

/** Pull to refresh and "Run update now" share the daily pipeline. */
@HiltWorker
class ManualRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: MarketPipeline,
    private val recorder: JobRunRecorder,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val runId = recorder.start(NAME)
        return try {
            recorder.finish(runId, pipeline.daily())
            Result.success()
        } catch (error: Exception) {
            recorder.fail(runId, error)
            Result.failure()
        }
    }

    companion object {
        const val NAME = "ManualRefreshWorker"
        const val UNIQUE_NAME = "mizan.manual.refresh"
    }
}
