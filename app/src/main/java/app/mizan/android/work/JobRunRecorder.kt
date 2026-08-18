package app.mizan.android.work

import app.mizan.android.core.IndiaClock
import app.mizan.android.data.db.JOB_STATUS_FAILED
import app.mizan.android.data.db.JOB_STATUS_PARTIAL
import app.mizan.android.data.db.JOB_STATUS_RUNNING
import app.mizan.android.data.db.JOB_STATUS_SUCCESS
import app.mizan.android.data.db.JobRunEntity
import app.mizan.android.data.db.MizanDatabase
import app.mizan.android.data.pipeline.PipelineResult
import javax.inject.Inject
import javax.inject.Singleton

/** Every run leaves a trace so Home and Account can be honest about staleness. */
@Singleton
class JobRunRecorder @Inject constructor(private val database: MizanDatabase) {

    suspend fun start(jobName: String): Long = database.jobRunDao().insert(
        JobRunEntity(
            jobName = jobName,
            startedAt = IndiaClock.nowMillis(),
            status = JOB_STATUS_RUNNING,
        )
    )

    suspend fun finish(id: Long, result: PipelineResult) {
        val existing = database.jobRunDao().byId(id) ?: return
        val status = when {
            result.fundsFetched == 0 && result.hasErrors -> JOB_STATUS_FAILED
            result.hasErrors -> JOB_STATUS_PARTIAL
            else -> JOB_STATUS_SUCCESS
        }
        database.jobRunDao().update(
            existing.copy(
                finishedAt = IndiaClock.nowMillis(),
                status = status,
                error = result.errors.firstOrNull(),
                fundsFetched = result.fundsFetched,
                signalsWritten = result.signalsWritten,
                notifiesPosted = result.notifiesPosted,
            )
        )
    }

    suspend fun fail(id: Long, error: Throwable) {
        val existing = database.jobRunDao().byId(id) ?: return
        database.jobRunDao().update(
            existing.copy(
                finishedAt = IndiaClock.nowMillis(),
                status = JOB_STATUS_FAILED,
                error = error.message ?: error::class.simpleName,
            )
        )
    }
}
