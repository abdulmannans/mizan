package app.mizan.android.core

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val INDIA: Locale = Locale.Builder().setLanguage("en").setRegion("IN").build()

object Formatters {

    private val rupees: NumberFormat = NumberFormat.getCurrencyInstance(INDIA).apply {
        maximumFractionDigits = 0
    }

    private val rupeesPrecise: NumberFormat = NumberFormat.getCurrencyInstance(INDIA).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    private val plainNumber: NumberFormat = NumberFormat.getNumberInstance(INDIA)

    private val dayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", INDIA)
    private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", INDIA)
    private val timestamp: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, h:mm a", INDIA)

    fun money(amount: Double?): String = if (amount == null) "--" else rupees.format(amount)

    fun moneySigned(amount: Double?): String {
        if (amount == null) return "--"
        val sign = if (amount > 0) "+" else ""
        return sign + rupees.format(amount)
    }

    fun nav(value: Double?): String = if (value == null) "--" else rupeesPrecise.format(value)

    fun units(value: Double?): String = if (value == null) "--" else "%.3f".format(value)

    fun percent(value: Double?, decimals: Int = 2): String =
        if (value == null) "--" else "%.${decimals}f%%".format(value)

    fun percentSigned(value: Double?, decimals: Int = 2): String {
        if (value == null) return "--"
        val sign = if (value > 0) "+" else ""
        return "$sign%.${decimals}f%%".format(value)
    }

    fun date(value: LocalDate?): String = value?.format(dayMonthYear) ?: "--"

    fun shortDate(value: LocalDate?): String = value?.format(dayMonth) ?: "--"

    fun dateTime(value: LocalDateTime?): String = value?.format(timestamp) ?: "--"

    fun count(value: Int): String = plainNumber.format(value)
}
