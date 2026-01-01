package com.qspapps.remindermate.workers

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object WorkerStatusUtils {

    fun getWorkerStatusMessages(history: Map<String, Long>): List<String> {
        val currentTime = System.currentTimeMillis()
        val statusItems = mutableListOf<String>()

        val intervals = mapOf(
            CleanupWorker.WORK_NAME to TimeUnit.DAYS.toMillis(CleanupWorker.REPEAT_INTERVAL_DAYS),
            OverdueWorker.WORK_NAME to TimeUnit.HOURS.toMillis(OverdueWorker.REPEAT_INTERVAL_HOURS)
        )

        intervals.forEach { (name, intervalMs) ->
            val lastRun = history[name]
            if (lastRun == null) {
                statusItems.add("$name: Never run")
            } else if (currentTime - lastRun > intervalMs) {
                val dateString = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastRun),
                    ZoneId.systemDefault()
                ).format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))
                statusItems.add("$name: Last run $dateString (Interval exceeded)")
            }
        }
        return statusItems
    }
}
