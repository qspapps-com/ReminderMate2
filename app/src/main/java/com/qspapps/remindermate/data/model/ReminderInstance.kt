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
    val isRecurring: Boolean // New field
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
            val from = currentTime.minusYears(1) // Or any reasonable threshold for "old" reminders
            val to = currentTime

            // Reuse the existing getReminderInstances logic to get all occurrences up to now
            val allPastInstances = getReminderInstances(reminders, actions, from, to)

            return allPastInstances.filter { instance ->
                !instance.isCompleted && instance.displayTime.isBefore(currentTime)
            }
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
                val occurrences = reminder.getOccurrences(from.toLocalDate(), to.toLocalDate())

                for (originalTime in occurrences) {
                    if (originalTime.isAfter(to) || originalTime.isBefore(from)) continue

                    val action = reminderActions.find { it.originalScheduledTime == originalTime }

                    when (action?.type) {
                        ActionType.COMPLETED -> {
                            instances.add(createInstance(reminder, originalTime, isCompleted = true))
                        }
                        ActionType.SNOOZED -> {
                            val newTime = action.rescheduledTime!!
                            if (newTime.isBetween(from, to)) {
                                instances.add(createInstance(reminder, originalTime, newTime, isSnoozed = true))
                            }
                        }
                        ActionType.DELETED -> {
                            // Do not add to the list
                        }
                        null -> {
                            instances.add(createInstance(reminder, originalTime))
                        }
                    }
                }

                val incomingSnoozes = reminderActions.filter {
                    it.type == ActionType.SNOOZED &&
                            it.rescheduledTime?.isBetween(from, to) == true &&
                            !it.originalScheduledTime.isBetween(from, to)
                }

                for (snooze in incomingSnoozes) {
                    instances.add(createInstance(reminder, snooze.originalScheduledTime, snooze.rescheduledTime!!, isSnoozed = true))
                }
            }
            return instances.sortedBy { it.displayTime }
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
                isRecurring = reminder.recurrence != null // Set the new field
            )
        }

        private fun LocalDateTime.isBetween(start: LocalDateTime, end: LocalDateTime): Boolean {
            return !this.isBefore(start) && this.isBefore(end)
        }
    }
}
