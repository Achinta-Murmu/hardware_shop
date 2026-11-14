package com.galla.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A stretch of time: from `start` up to, but not including, `end`. Both in milliseconds. */
data class TimeRange(val start: Long, val end: Long)

/**
 * Times are stored as UTC milliseconds. "Today" and "this month" are worked out here,
 * in the phone's own timezone, so a shop's day ends at the shop's midnight.
 */
object Dates {
    private fun zone(): ZoneId = ZoneId.systemDefault()

    fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone()).toLocalDate()

    fun startOf(date: LocalDate): Long = date.atStartOfDay(zone()).toInstant().toEpochMilli()

    fun day(date: LocalDate): TimeRange = TimeRange(startOf(date), startOf(date.plusDays(1)))

    fun month(month: YearMonth): TimeRange = TimeRange(startOf(month.atDay(1)), startOf(month.plusMonths(1).atDay(1)))

    fun days(from: LocalDate, toInclusive: LocalDate): TimeRange = TimeRange(startOf(from), startOf(toInclusive.plusDays(1)))

    private val weekdayDayMonth = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val clock = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val weekdayShort = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)

    fun weekdayDayMonth(date: LocalDate): String = date.format(weekdayDayMonth)
    fun dayMonth(date: LocalDate): String = date.format(dayMonth)
    fun dayMonthYear(date: LocalDate): String = date.format(dayMonthYear)
    fun monthYear(month: YearMonth): String = month.format(monthYear)
    fun weekday(date: LocalDate): String = date.format(weekdayShort)

    fun time(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zone()).toLocalTime().format(clock)

    /** "Today", "Yesterday" or "12 Nov". */
    fun friendlyDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.year == today.year) dayMonth(date) else dayMonthYear(date)
    }

    /** "Today, 4:15 PM" or "12 Nov, 4:15 PM". */
    fun friendly(millis: Long, now: Long): String = "${friendlyDay(dateOf(millis), dateOf(now))}, ${time(millis)}"

    /** "14 Nov 2026, 4:15 PM" for receipts. */
    fun full(millis: Long): String = "${dayMonthYear(dateOf(millis))}, ${time(millis)}"
}
