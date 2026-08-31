package com.qspapps.remindermate.utils

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Emits the current time immediately and then on every minute boundary, so anything derived from
 * "now" (overdue styling, overdue lists) turns over as the clock does rather than up to a minute
 * late. Cold, so collecting it under `WhileSubscribed` stops the timer when the UI goes away.
 */
fun minuteTicker(): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now)
        val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        delay(Duration.between(now, nextMinute).toMillis())
    }
}
