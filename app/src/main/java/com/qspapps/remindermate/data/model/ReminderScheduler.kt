package com.qspapps.remindermate.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ReminderScheduler {

    /**
     * Main function: Get all displayable instances for a specific day.
     */
    fun getRemindersForDay(
        day: LocalDate,
        reminders: List<Reminder>,
        actions: List<ReminderAction> // Load actions for these reminders from DB
    ): List<ReminderInstance> {

        val instances = mutableListOf<ReminderInstance>()

        // Group actions by reminderId for faster lookup
        val actionsMap = actions.groupBy { it.reminderId }

        for (reminder in reminders) {
            val reminderActions = actionsMap[reminder.id] ?: emptyList()

            // 1. Generate all theoretical occurrences for this day based on rules
            val theoreticalTimes = calculateOccurrencesForDay(reminder, day, reminderActions)

            // 2. Apply state (Snooze/Complete) logic
            for (origTime in theoreticalTimes) {
                // Find if we have an action for this specific occurrence time
                val action = reminderActions.find { it.originalScheduledTime == origTime }

                if (action != null) {
                    when (action.type) {
                        ActionType.COMPLETED -> {
                            // If you want to show completed items crossed out:
                            instances.add(createInstance(reminder, origTime, origTime, isCompleted = true, isSnoozed = false))
                            // If you don't want to show completed items at all, do nothing here.
                        }
                        ActionType.SNOOZED -> {
                            // Only show the snoozed instance if the NEW time falls on the requested day
                            val newTime = action.resheduledTime!!
                            if (newTime.toLocalDate() == day) {
                                instances.add(createInstance(reminder, newTime, origTime, isCompleted = false, isSnoozed = true))
                            }
                        }
                    }
                } else {
                    // No action taken, show as normal active reminder
                    instances.add(createInstance(reminder, origTime, origTime, isCompleted = false, isSnoozed = false))
                }
            }

            // 3. Handle snoozes from PAST days that landed on THIS day
            // (e.g., Yesterday 10PM snoozed for 4 hours -> Today 2AM)
            val incomingSnoozes = reminderActions.filter {
                it.type == ActionType.SNOOZED &&
                        it.resheduledTime?.toLocalDate() == day &&
                        it.originalScheduledTime.toLocalDate() != day // Origin was not today
            }

            for (snooze in incomingSnoozes) {
                instances.add(createInstance(reminder, snooze.resheduledTime!!, snooze.originalScheduledTime, isCompleted = false, isSnoozed = true))
            }
        }

        return instances.sortedBy { it.displayTime }
    }

    private fun createInstance(r: Reminder, display: LocalDateTime, origin: LocalDateTime, isCompleted: Boolean, isSnoozed: Boolean) =
        ReminderInstance(r.id, r.title, r.description, display, origin, isCompleted, isSnoozed)

    /**
     * Logic to determine if/when a reminder occurs on a given day.
     */
    private fun calculateOccurrencesForDay(reminder: Reminder, targetDay: LocalDate, allActions: List<ReminderAction>): List<LocalDateTime> {
        val occurrences = mutableListOf<LocalDateTime>()
        val start = reminder.startDateTime

        // Optimization: If start date is in future of target day, return empty
        if (start.toLocalDate().isAfter(targetDay)) return emptyList()

        // 1. Non-Recurring
        if (reminder.recurrence == null) {
            if (start.toLocalDate() == targetDay) {
                occurrences.add(start)
            }
            return occurrences
        }

        // 2. Recurring
        val rule = reminder.recurrence
        var occurrenceCount = 0
        if (rule.count != null) {
            // Count all past completed actions for this reminder to see if we've hit the limit
            occurrenceCount = allActions.count { it.type == ActionType.COMPLETED }
        }

        if (rule.count != null && occurrenceCount >= rule.count) {
            return emptyList() // The recurring reminder has completed its cycle
        }

        when (rule.frequency) {
            Frequency.HOURLY -> {
                var current = start
                while (current.toLocalDate().isBefore(targetDay)) {
                    current = current.plusHours(rule.interval.toLong())
                }
                while (current.toLocalDate() == targetDay) {
                    occurrences.add(current)
                    current = current.plusHours(rule.interval.toLong())
                }
            }

            Frequency.DAILY -> {
                val daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), targetDay)
                if (daysBetween >= 0 && daysBetween % rule.interval == 0L) {
                    occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                }
            }

            Frequency.WEEKLY -> {
                val requiredDays = rule.daysOfWeek ?: setOf(start.dayOfWeek)
                if (requiredDays.contains(targetDay.dayOfWeek)) {
                    occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                }
            }

            Frequency.MONTHLY -> {
                if (targetDay.dayOfMonth == start.dayOfMonth) {
                    val monthsBetween = ChronoUnit.MONTHS.between(start.toLocalDate(), targetDay)
                    if (monthsBetween % rule.interval == 0L) {
                        occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                    }
                }
            }

            Frequency.YEARLY -> {
                if (targetDay.dayOfMonth == start.dayOfMonth && targetDay.month == start.month) {
                    val yearsBetween = ChronoUnit.YEARS.between(start.toLocalDate(), targetDay)
                    if (yearsBetween % rule.interval == 0L) {
                        occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                    }
                }
            }
        }

        return occurrences
    }
}
