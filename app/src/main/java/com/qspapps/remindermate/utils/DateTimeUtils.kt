package com.qspapps.remindermate.utils

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object DateTimeUtils {
    // Formatters are immutable and expensive to build, but they bake in a locale, so they are
    // cached per locale rather than once: the process survives locale changes.
    private val timeFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()
    private val dateFormatters = ConcurrentHashMap<Locale, DateTimeFormatter>()

    private fun timeFormatter(): DateTimeFormatter =
        timeFormatters.computeIfAbsent(Locale.getDefault()) { locale ->
            // Localized rather than a fixed "hh:mm a" so the device's 24-hour setting is honoured.
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        }

    private fun dateFormatter(): DateTimeFormatter =
        dateFormatters.computeIfAbsent(Locale.getDefault()) { locale ->
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        }

    fun formatDateTime(dateTime: LocalDateTime, separator: String = "-"): String =
        "${formatDate(dateTime)}$separator${formatTime(dateTime)}"

    fun formatTime(localDateTime: LocalDateTime): String = localDateTime.format(timeFormatter())

    fun formatTime(localTime: LocalTime): String = localTime.format(timeFormatter())

    fun formatDate(localDateTime: LocalDateTime): String = localDateTime.format(dateFormatter())

    fun minsFromNow(minutes: Long): LocalDateTime =
        LocalDateTime.now().plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES)
}

/**
 * The soonest of these times on [reference]'s date that is after both [reference] and
 * [mustBeAfter], or null when none of them qualify.
 */
fun List<LocalTime>.nextAfter(
    reference: LocalDateTime,
    mustBeAfter: LocalDateTime = reference
): LocalDateTime? = asSequence()
    .map(reference::with)
    .filter { it.isAfter(reference) && it.isAfter(mustBeAfter) }
    .minOrNull()
