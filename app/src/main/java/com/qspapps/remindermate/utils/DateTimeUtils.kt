package com.qspapps.remindermate.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.getDefault())

    fun formatDateTime(dateTime: LocalDateTime, separator: String = "-"): String =
        "${formatDate(dateTime)}$separator${formatTime(dateTime)}"

    fun formatTime(localDateTime: LocalDateTime): String =
        localDateTime.format(timeFormatter)

    fun formatDate(localDateTime: LocalDateTime): String =
        localDateTime.format(dateFormatter)

    fun minsFromNow(minutes: Long): LocalDateTime =
        LocalDateTime.now().plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES)
}
