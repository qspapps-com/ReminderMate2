package com.qspapps.remindermate.domain

import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import com.qspapps.remindermate.data.model.ReminderInstance
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

/**
 * How far back [overdueReminders] looks. Occurrences older than this stop being reported as
 * overdue, which keeps the sequence walk bounded for long-running recurring reminders.
 */
val OVERDUE_LOOKBACK: Period = Period.ofYears(1)

/** Every occurrence that should be shown on [day], in display order. */
fun remindersForDay(
    day: LocalDate,
    reminders: List<Reminder>,
    actions: List<ReminderAction>
): List<ReminderInstance> = reminderInstances(
    reminders = reminders,
    actions = actions,
    from = day.atStartOfDay(),
    to = day.plusDays(1).atStartOfDay()
)

/** Occurrences that are past their display time and still pending. */
fun overdueReminders(
    reminders: List<Reminder>,
    actions: List<ReminderAction>,
    currentTime: LocalDateTime
): List<ReminderInstance> = reminderInstances(
    reminders = reminders,
    actions = actions,
    from = currentTime.minus(OVERDUE_LOOKBACK),
    to = currentTime
).filterNot { it.isCompleted }

/** Instances whose display time falls in `[from, to)`, sorted by display time. */
private fun reminderInstances(
    reminders: List<Reminder>,
    actions: List<ReminderAction>,
    from: LocalDateTime,
    to: LocalDateTime
): List<ReminderInstance> {
    val actionsByReminder = actions.groupBy { it.reminderId }
    return reminders
        .flatMap { reminder ->
            reminder.instancesIn(from, to, actionsByReminder[reminder.id].orEmpty())
        }
        .distinctBy { it.reminderId to it.originalTime }
        .sortedBy { it.displayTime }
}

private fun Reminder.instancesIn(
    from: LocalDateTime,
    to: LocalDateTime,
    actions: List<ReminderAction>
): List<ReminderInstance> {
    val actionByTime = actions.associateBy { it.originalScheduledTime }

    // 1. Occurrences whose original scheduled time falls in the window. A snooze can push one
    //    of these out of the window, in which case it is not shown here.
    val scheduled = getOccurrences(from, to).mapNotNull { originalTime ->
        val action = actionByTime[originalTime]
        if (action?.type == ActionType.DELETED) return@mapNotNull null

        val displayTime = when (action?.type) {
            ActionType.SNOOZED -> action.rescheduledTime ?: originalTime
            else -> originalTime
        }
        instanceAt(
            originalTime = originalTime,
            displayTime = displayTime,
            isCompleted = action?.type == ActionType.COMPLETED,
            isSnoozed = action?.type == ActionType.SNOOZED
        ).takeIf { displayTime.isBetween(from, to) }
    }

    // 2. Occurrences scheduled outside the window but snoozed into it.
    val scheduledTimes = scheduled.mapTo(mutableSetOf()) { it.originalTime }
    val snoozedIn = actions
        .filter { action ->
            action.type == ActionType.SNOOZED &&
                action.rescheduledTime?.isBetween(from, to) == true &&
                action.originalScheduledTime !in scheduledTimes
        }
        .map { action ->
            instanceAt(
                originalTime = action.originalScheduledTime,
                displayTime = action.rescheduledTime!!,
                isSnoozed = true
            )
        }

    return scheduled + snoozedIn
}

private fun Reminder.instanceAt(
    originalTime: LocalDateTime,
    displayTime: LocalDateTime = originalTime,
    isCompleted: Boolean = false,
    isSnoozed: Boolean = false
) = ReminderInstance(
    reminderId = id,
    title = title,
    description = description,
    displayTime = displayTime,
    originalTime = originalTime,
    isCompleted = isCompleted,
    isSnoozed = isSnoozed,
    isRecurring = recurrence != null
)

/** True when this is in `[start, end)`. */
private fun LocalDateTime.isBetween(start: LocalDateTime, end: LocalDateTime) =
    !isBefore(start) && isBefore(end)
