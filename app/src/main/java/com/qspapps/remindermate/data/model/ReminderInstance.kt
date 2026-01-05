package com.qspapps.remindermate.data.model

import java.time.LocalDate
import java.time.LocalDateTime

data class ReminderInstance(
    val reminderId: Long,
    val title: String,
    val description: String?,
    val displayTime: LocalDateTime, // The time to show on the UI (could be original or snoozed)
    val originalTime: LocalDateTime, // The original scheduled time
    val isCompleted: Boolean,
    val isSnoozed: Boolean,
    val isRecurring: Boolean
) {
    companion object {
        fun getRemindersForDay(
            day: LocalDate,
            reminders: List<Reminder>,
            actions: List<ReminderAction>
        ): List<ReminderInstance> {
            val startOfDay = day.atStartOfDay()
            val endOfDay = day.plusDays(1).atStartOfDay()
            return getReminderInstances(reminders, actions, startOfDay, endOfDay)
        }

        /**
         * Filters and returns instances that are past their scheduled time and not completed.
         */
        fun getOverdueReminders(
            reminders: List<Reminder>,
            actions: List<ReminderAction>,
            currentTime: LocalDateTime
        ): List<ReminderInstance> {
            // We define a window from a far past date up to the current time
            val from = currentTime.minusYears(1) // Threshold for overdue reminders
            val to = currentTime

            // getReminderInstances returns all occurrences with displayTime in [from, to)
            val allPastInstances = getReminderInstances(reminders, actions, from, to)

            return allPastInstances.filter { !it.isCompleted }
        }

        private fun getReminderInstances(
            reminders: List<Reminder>,
            actions: List<ReminderAction>,
            from: LocalDateTime,
            to: LocalDateTime
        ): List<ReminderInstance> {
            val instances = mutableListOf<ReminderInstance>()
            val actionsMap = actions.groupBy { it.reminderId }

            for (reminder in reminders) {
                val reminderActions = actionsMap[reminder.id] ?: emptyList()
                val actionPerTime = reminderActions.associateBy { it.originalScheduledTime }

                // 1. Process occurrences whose original scheduled time is in the date range.
                val occurrences = reminder.getOccurrences(from.toLocalDate(), to.toLocalDate())
                val processedOriginalTimes = mutableSetOf<LocalDateTime>()

                for (originalTime in occurrences) {
                    processedOriginalTimes.add(originalTime)
                    val action = actionPerTime[originalTime]
                    if (action?.type == ActionType.DELETED) continue

                    val displayTime = if (action?.type == ActionType.SNOOZED) {
                        action.rescheduledTime ?: originalTime
                    } else {
                        originalTime
                    }

                    if (displayTime.isBetween(from, to)) {
                        instances.add(
                            createInstance(
                                reminder,
                                originalTime,
                                displayTime,
                                isCompleted = action?.type == ActionType.COMPLETED,
                                isSnoozed = action?.type == ActionType.SNOOZED
                            )
                        )
                    }
                }

                // 2. Handle cases where original time was outside the range, but it was snoozed into the range.
                val incomingSnoozes = reminderActions.filter {
                    it.type == ActionType.SNOOZED &&
                            it.rescheduledTime?.isBetween(from, to) == true &&
                            it.originalScheduledTime !in processedOriginalTimes
                }

                for (snooze in incomingSnoozes) {
                    instances.add(
                        createInstance(
                            reminder,
                            snooze.originalScheduledTime,
                            snooze.rescheduledTime!!,
                            isSnoozed = true
                        )
                    )
                }
            }
            return instances.distinctBy { it.reminderId to it.originalTime }.sortedBy { it.displayTime }
        }

        private fun createInstance(
            reminder: Reminder,
            originalTime: LocalDateTime,
            displayTime: LocalDateTime = originalTime,
            isCompleted: Boolean = false,
            isSnoozed: Boolean = false
        ): ReminderInstance {
            return ReminderInstance(
                reminderId = reminder.id,
                title = reminder.title,
                description = reminder.description,
                displayTime = displayTime,
                originalTime = originalTime,
                isCompleted = isCompleted,
                isSnoozed = isSnoozed,
                isRecurring = reminder.recurrence != null
            )
        }

        private fun LocalDateTime.isBetween(start: LocalDateTime, end: LocalDateTime): Boolean {
            // Check if time is in [start, end)
            return !this.isBefore(start) && this.isBefore(end)
        }
    }
}
