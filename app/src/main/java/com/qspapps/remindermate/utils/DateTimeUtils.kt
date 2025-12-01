package com.qspapps.remindermate.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateTimeUtils {
    fun formatTime(localDateTime: LocalDateTime): String {
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        return localDateTime.format(formatter)
    }

    fun isDue(localDateTime: LocalDateTime): Boolean {
        return localDateTime.isBefore(LocalDateTime.now())
    }

    fun minsFromNow(minutes: Long): LocalDateTime {
        return LocalDateTime.now().plusMinutes(minutes).truncatedTo(ChronoUnit.MINUTES)
    }
}
