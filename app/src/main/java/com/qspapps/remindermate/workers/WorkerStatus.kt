package com.qspapps.remindermate.workers

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** A background worker that has not run as often as its schedule promises. */
sealed interface WorkerStatus {
    val workName: String

    data class NeverRun(override val workName: String) : WorkerStatus
    data class Overdue(override val workName: String, val lastRun: LocalDateTime) : WorkerStatus
}

private val expectedIntervalsMs = mapOf(
    CleanupWorker.WORK_NAME to TimeUnit.DAYS.toMillis(CleanupWorker.REPEAT_INTERVAL_DAYS),
    OverdueWorker.WORK_NAME to TimeUnit.HOURS.toMillis(OverdueWorker.REPEAT_INTERVAL_HOURS)
)

/**
 * Workers that look unhealthy given [history] (worker name to last run, epoch millis).
 * An empty result means everything is running on schedule.
 */
fun workerStatuses(
    history: Map<String, Long>,
    now: Long = System.currentTimeMillis()
): List<WorkerStatus> = expectedIntervalsMs.mapNotNull { (workName, intervalMs) ->
    val lastRun = history[workName]
    when {
        lastRun == null -> WorkerStatus.NeverRun(workName)
        now - lastRun > intervalMs -> WorkerStatus.Overdue(
            workName,
            LocalDateTime.ofInstant(Instant.ofEpochMilli(lastRun), ZoneId.systemDefault())
        )
        else -> null
    }
}
