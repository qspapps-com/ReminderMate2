package com.qspapps.remindermate.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ReminderInstance(
    val reminderId: Long,
    val title: String,
    val description: String?,
    val displayTime: LocalDateTime, // The time to show on the UI (could be original or snoozed)
    val originalTime: LocalDateTime, // The original scheduled time
    val isCompleted: Boolean,
    val isSnoozed: Boolean
) {
    companion object {
        fun getRemindersForDay(
            day: LocalDate,
            reminders: List<Reminder>,
            actions: List<ReminderAction>
        ): List<ReminderInstance> {

            val instances = mutableListOf<ReminderInstance>()
            val actionsMap = actions.groupBy { it.reminderId }

            for (reminder in reminders) {
                val reminderActions = actionsMap[reminder.id] ?: emptyList()
                val theoreticalTimes = calculateOccurrencesForDay(reminder, day)

                for (origTime in theoreticalTimes) {
                    val action = reminderActions.find { it.originalScheduledTime == origTime }

                    if (action != null) {
                        when (action.type) {
                            ActionType.COMPLETED -> {
                                instances.add(ReminderInstance(reminder.id, reminder.title, reminder.description, origTime, origTime, isCompleted = true, isSnoozed = false))
                            }
                            ActionType.SNOOZED -> {
                                val newTime = action.resheduledTime!!
                                if (newTime.toLocalDate() == day) {
                                    instances.add(ReminderInstance(reminder.id, reminder.title, reminder.description, newTime, origTime, isCompleted = false, isSnoozed = true))
                                }
                            }
                        }
                    } else {
                        instances.add(ReminderInstance(reminder.id, reminder.title, reminder.description, origTime, origTime, isCompleted = false, isSnoozed = false))
                    }
                }

                val incomingSnoozes = reminderActions.filter {
                    it.type == ActionType.SNOOZED &&
                            it.resheduledTime?.toLocalDate() == day &&
                            it.originalScheduledTime.toLocalDate() != day
                }

                for (snooze in incomingSnoozes) {
                    instances.add(ReminderInstance(reminder.id, reminder.title, reminder.description, snooze.resheduledTime!!, snooze.originalScheduledTime, isCompleted = false, isSnoozed = true))
                }
            }

            return instances.sortedBy { it.displayTime }
        }

        fun getNextOccurrence(reminder: Reminder, actions: List<ReminderAction>, after: LocalDateTime? = null): ReminderInstance? {
            val completedTimes = actions.filter { it.type == ActionType.COMPLETED }
                .map { it.originalScheduledTime }
                .toSet()

            val searchFrom = after ?: reminder.startDateTime.minusNanos(1)

            if (reminder.recurrence == null) {
                val originalTime = reminder.startDateTime
                if (completedTimes.contains(originalTime)) {
                    return null
                }

                val snoozedAction = actions.firstOrNull { it.originalScheduledTime == originalTime && it.type == ActionType.SNOOZED }
                val displayTime = snoozedAction?.resheduledTime ?: originalTime

                if (displayTime.isAfter(searchFrom)) {
                    return ReminderInstance(
                        reminderId = reminder.id, title = reminder.title, description = reminder.description,
                        displayTime = displayTime, originalTime = originalTime,
                        isCompleted = false, isSnoozed = snoozedAction != null
                    )
                }
                return null
            }

            val rule = reminder.recurrence
            if (rule.count != null && completedTimes.size >= rule.count) {
                return null
            }

            var targetDay = searchFrom.toLocalDate()
            var daySearchFrom = searchFrom

            for (i in 0..36500) { // Look ahead up to 100 years
                val occurrencesOnDay = calculateOccurrencesForDay(reminder, targetDay)

                for (originalTime in occurrencesOnDay) {
                    if (completedTimes.contains(originalTime)) continue

                    val snoozeAction = actions.find { it.originalScheduledTime == originalTime && it.type == ActionType.SNOOZED }
                    val displayTime = snoozeAction?.resheduledTime ?: originalTime

                    if (displayTime.isAfter(daySearchFrom)) {
                        return ReminderInstance(
                            reminderId = reminder.id, title = reminder.title, description = reminder.description,
                            displayTime = displayTime, originalTime = originalTime,
                            isCompleted = false, isSnoozed = snoozeAction != null
                        )
                    }
                }
                targetDay = targetDay.plusDays(1)
                daySearchFrom = targetDay.atStartOfDay().minusNanos(1)
            }

            return null
        }

        fun calculateOccurrencesForDay(reminder: Reminder, targetDay: LocalDate): List<LocalDateTime> {
            val occurrences = mutableListOf<LocalDateTime>()
            val start = reminder.startDateTime

            if (start.toLocalDate().isAfter(targetDay)) {
                 return emptyList()
            }

            if (reminder.recurrence == null) {
                if (start.toLocalDate() == targetDay) {
                    occurrences.add(start)
                }
                return occurrences
            }

            val rule = reminder.recurrence

            when (rule.frequency) {
                Frequency.HOURLY -> {
                    var current: LocalDateTime
                    if (targetDay.isEqual(start.toLocalDate())) {
                        current = start
                    } else {
                        val hoursSinceStart = ChronoUnit.HOURS.between(start, targetDay.atTime(start.toLocalTime()))
                        val remainder = hoursSinceStart % rule.interval
                        val adjustment = if (remainder == 0L) 0L else rule.interval - remainder
                        current = targetDay.atTime(start.toLocalTime()).plusHours(adjustment)
                    }
                    
                    while (current.toLocalDate() == targetDay) {
                        if (!current.isBefore(start)) {
                            occurrences.add(current)
                        }
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
                        val startOfWeek = start.toLocalDate().minusDays(start.dayOfWeek.value.toLong() - 1)
                        val targetWeek = targetDay.minusDays(targetDay.dayOfWeek.value.toLong() - 1)
                        val weeksBetween = ChronoUnit.WEEKS.between(startOfWeek, targetWeek)
                        if (weeksBetween >= 0 && weeksBetween % rule.interval == 0L) {
                            occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                        }
                    }
                }

                Frequency.MONTHLY -> {
                    if (targetDay.dayOfMonth == start.dayOfMonth) {
                         val monthsBetween = ChronoUnit.MONTHS.between(start.toLocalDate().withDayOfMonth(1), targetDay.withDayOfMonth(1))
                        if (monthsBetween >= 0 && monthsBetween % rule.interval == 0L) {
                            occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                        }
                    }
                }

                Frequency.YEARLY -> {
                    if (targetDay.dayOfMonth == start.dayOfMonth && targetDay.month == start.month) {
                        val yearsBetween = ChronoUnit.YEARS.between(start.toLocalDate(), targetDay)
                        if (yearsBetween >= 0 && yearsBetween % rule.interval == 0L) {
                            occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                        }
                    }
                }
            }

            return occurrences
        }
    }
}
