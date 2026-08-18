package app.mizan.android.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Units usually allot two business days after the debit, so the allotment NAV is rarely the
 * latest NAV on the chart.
 */
object SipAllotment {

    private const val BUSINESS_DAYS_LAG = 2
    private const val MAX_CALENDAR_LAG = 3

    fun estimatedAllotmentDate(debitDate: LocalDate): LocalDate {
        var date = debitDate
        var businessDays = 0
        var calendarLag = 0
        while (businessDays < BUSINESS_DAYS_LAG && calendarLag < MAX_CALENDAR_LAG) {
            date = date.plusDays(1)
            calendarLag++
            if (isBusinessDay(date)) businessDays++
        }
        return date
    }

    /** Next debit for a monthly SIP on [dayOfMonth] (1..28), counting today as still due. */
    fun nextDebitDate(dayOfMonth: Int, today: LocalDate): LocalDate {
        val day = dayOfMonth.coerceIn(1, 28)
        val thisMonth = today.withDayOfMonth(day)
        return if (!thisMonth.isBefore(today)) thisMonth else today.plusMonths(1).withDayOfMonth(day)
    }

    private fun isBusinessDay(date: LocalDate): Boolean =
        date.dayOfWeek != DayOfWeek.SATURDAY && date.dayOfWeek != DayOfWeek.SUNDAY
}
