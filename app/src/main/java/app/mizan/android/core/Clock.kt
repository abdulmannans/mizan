package app.mizan.android.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Every price date, signal date and job window in Mizan is an Indian session date.
 */
object IndiaClock {

    val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    fun today(): LocalDate = LocalDate.now(ZONE)

    fun now(): LocalDateTime = LocalDateTime.now(ZONE)

    fun nowMillis(): Long = System.currentTimeMillis()

    fun toLocalDateTime(epochMillis: Long): LocalDateTime =
        Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDateTime()

    fun toLocalDate(epochMillis: Long): LocalDate = toLocalDateTime(epochMillis).toLocalDate()

    /** Milliseconds until the next occurrence of [time] in IST. */
    fun millisUntil(time: LocalTime): Long {
        val now = ZonedDateTime.now(ZONE)
        var next = now.with(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return next.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
    }
}
