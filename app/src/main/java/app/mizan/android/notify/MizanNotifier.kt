package app.mizan.android.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.mizan.android.MainActivity
import app.mizan.android.R
import app.mizan.android.core.Compliance
import app.mizan.android.core.Formatters
import app.mizan.android.domain.Level
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local notifications only. There is no FCM and no server: the daily job on this device decides
 * what is worth a buzz.
 */
@Singleton
class MizanNotifier @Inject constructor(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        val dips = NotificationChannel(
            CHANNEL_DIPS,
            "Dip opportunities",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Watchlisted funds and metals at a dip score of 65 or more." }

        val gold = NotificationChannel(
            CHANNEL_GOLD,
            "Gold drop",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "Gold falling a set rupee amount below its recent high." }

        val jobs = NotificationChannel(
            CHANNEL_JOBS,
            "Background updates",
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "History loading and daily price updates." }

        manager.createNotificationChannels(listOf(dips, gold, jobs))
    }

    fun canPost(): Boolean = manager.areNotificationsEnabled()

    fun notifyDip(
        route: String,
        notificationId: Int,
        subjectName: String,
        score: Int,
        level: Level,
        suggestedRupees: Double,
    ): Pair<String, String> {
        val title = "Dip opportunity — $subjectName"
        val body = "Score $score/100 · ${level.label.uppercase()} · suggested ${Formatters.money(suggestedRupees)}"
        post(
            channelId = CHANNEL_DIPS,
            notificationId = notificationId,
            title = title,
            body = body,
            longText = "$body\n\n${Compliance.DISCLAIMER}",
            route = route,
        )
        return title to body
    }

    fun notifyGoldDrop(
        dropRupees: Double,
        peak: Double,
        peakDate: LocalDate,
    ): Pair<String, String> {
        val title = "Gold off recent high"
        val body = "Down ${Formatters.money(dropRupees)} from ${Formatters.money(peak)} (${Formatters.date(peakDate)})"
        post(
            channelId = CHANNEL_GOLD,
            notificationId = ID_GOLD_DROP,
            title = title,
            body = body,
            longText = "$body\n\n${Compliance.METAL_QUOTES}\n\n${Compliance.DISCLAIMER}",
            route = "metal/gold",
        )
        return title to body
    }

    fun postGroupSummary(count: Int) {
        if (count < 2) return
        val summary = NotificationCompat.Builder(context, CHANNEL_DIPS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$count dip alerts")
            .setContentText(Compliance.DISCLAIMER_SHORT)
            .setGroup(GROUP_DIPS)
            .setGroupSummary(true)
            .setContentIntent(routeIntent("missed"))
            .setAutoCancel(true)
            .build()
        safeNotify(ID_GROUP_SUMMARY, summary)
    }

    fun jobProgressNotification(text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_JOBS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mizan is loading history")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun notifyJobFailure() {
        val notification = NotificationCompat.Builder(context, CHANNEL_JOBS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Couldn't update prices")
            .setContentText("Mizan will try again. Prices on Home may be old.")
            .setAutoCancel(true)
            .setContentIntent(routeIntent("home"))
            .build()
        safeNotify(ID_JOB_FAILURE, notification)
    }

    private fun post(
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        longText: String,
        route: String,
    ) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(longText))
            .setGroup(GROUP_DIPS)
            .setAutoCancel(true)
            .setContentIntent(routeIntent(route))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        safeNotify(notificationId, notification)
    }

    private fun routeIntent(route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun safeNotify(id: Int, notification: Notification) {
        if (!canPost()) return
        runCatching { manager.notify(id, notification) }
    }

    companion object {
        const val CHANNEL_DIPS = "mizan.dips"
        const val CHANNEL_GOLD = "mizan.gold"
        const val CHANNEL_JOBS = "mizan.jobs"

        const val GROUP_DIPS = "mizan.dips"
        const val EXTRA_ROUTE = "mizan_route"

        const val ID_BACKFILL_FOREGROUND = 1001
        const val ID_GROUP_SUMMARY = 1002
        const val ID_GOLD_DROP = 1003
        const val ID_JOB_FAILURE = 1004

        fun fundNotificationId(schemeCode: Long): Int = (2_000_000 + schemeCode).toInt()
        fun metalNotificationId(metalId: String): Int = 3_000_000 + metalId.hashCode() % 1000
    }
}
