package com.qspapps.remindermate.data.model

import java.time.DayOfWeek
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
            // Removed the old check: if (rule.count != null && completedTimes.size >= rule.count) { return null }

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

        private fun getWeekValues(start: DayOfWeek): Map<DayOfWeek, Int> =
            DayOfWeek.entries.associateWith { (7 + it.value - start.value) % 7 }

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
                Frequency.MINUTE -> {
                    var current: LocalDateTime
                    var theoreticalCount: Int

                    if (targetDay.isEqual(start.toLocalDate())) {
                        current = start
                        theoreticalCount = 1
                    } else {
                        // Logic to find the first occurrence on targetDay, anchored at 00:00
                        val targetDayStart = targetDay.atStartOfDay()
                        val minutesBetween = ChronoUnit.MINUTES.between(start, targetDayStart)

                        val remainder = minutesBetween % rule.interval
                        val adjustmentMinutes = if (remainder == 0L) 0L else rule.interval - remainder

                        current = targetDayStart.plusMinutes(adjustmentMinutes)

                        // Calculate the 1-based index of this occurrence
                        val minutesToCurrent = ChronoUnit.MINUTES.between(start, current)
                        theoreticalCount = (minutesToCurrent / rule.interval).toInt() + 1
                    }

                    while (current.toLocalDate() == targetDay) {
                        if (rule.count != null && theoreticalCount > rule.count) {
                            break
                        }
                        
                        if (!current.isBefore(start)) {
                            occurrences.add(current)
                        }
                        current = current.plusMinutes(rule.interval.toLong())
                        theoreticalCount++
                    }
                }
                Frequency.HOURLY -> {
                    var current: LocalDateTime
                    var theoreticalCount: Int
                    
                    if (targetDay.isEqual(start.toLocalDate())) {
                        current = start
                        theoreticalCount = 1
                    } else {
                        // Logic to find the first occurrence on targetDay, anchored at 00:00
                        val targetDayStart = targetDay.atStartOfDay()
                        val hoursBetween = ChronoUnit.HOURS.between(start, targetDayStart)

                        val remainder = hoursBetween % rule.interval
                        val adjustmentHours = if (remainder == 0L) 0L else rule.interval - remainder

                        current = targetDayStart.plusHours(adjustmentHours)

                        // Calculate the 1-based index of this occurrence
                        val hoursToCurrent = ChronoUnit.HOURS.between(start, current)
                        theoreticalCount = (hoursToCurrent / rule.interval).toInt() + 1
                    }
                    
                    while (current.toLocalDate() == targetDay) {
                        if (rule.count != null && theoreticalCount > rule.count) {
                            break
                        }

                        if (!current.isBefore(start)) {
                            occurrences.add(current)
                        }
                        current = current.plusHours(rule.interval.toLong())
                        theoreticalCount++
                    }
                }

                Frequency.DAILY -> {
                    val daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), targetDay)
                    if (daysBetween >= 0 && daysBetween % rule.interval == 0L) {
                        val occurrenceIndex = (daysBetween / rule.interval).toInt() + 1
                        if (rule.count == null || occurrenceIndex <= rule.count) {
                            occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                        }
                    }
                }

                Frequency.WEEKLY -> {
                    val requiredDays = rule.daysOfWeek ?: setOf(start.dayOfWeek)
                    if (requiredDays.contains(targetDay.dayOfWeek)) {
                        
                        // Calculate days between the start date and the target date
                        val daysBetween = ChronoUnit.DAYS.between(start.toLocalDate(), targetDay)
                        
                        if (daysBetween >= 0) {
                            // Calculate the number of full weeks passed since the day of week of the start date
                            val weeksBetween = daysBetween / 7L
                            
                            // Check if the interval is met
                            val shouldRecur = weeksBetween % rule.interval == 0L

                            if (shouldRecur) {
                                
                                if (rule.count != null) {
                                    // Calculate occurrence index
                                    val weeklyValues = getWeekValues(start.dayOfWeek)
                                    val daysInRecurrence = requiredDays.size
                                    
                                    // Calculate total occurrences up to the end of the previous week block
                                    val weeksPassed = weeksBetween 
                                    
                                    val occurrencesFromFullCycles = (weeksPassed / rule.interval) * daysInRecurrence
                                    println("occurencesFromFullCycles:$occurrencesFromFullCycles")
                                    // The index of the day within the current week block
                                    val currentWeekDayIndex = requiredDays.count { d ->
                                        weeklyValues.getOrDefault(d, 0) <= weeklyValues.getOrDefault(targetDay.dayOfWeek, 0) }
                                    println("currentWeekdayIndex: $currentWeekDayIndex")
                                    val occurrenceIndex = occurrencesFromFullCycles + currentWeekDayIndex
                                    
                                    if (occurrenceIndex <= rule.count) {
                                        occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                                    }
                                } else {
                                    // Original logic when rule.count is null
                                    occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                                }
                            }
                        }
                    }
                }

                Frequency.MONTHLY -> {
                    if (targetDay.dayOfMonth == start.dayOfMonth) {
                         val monthsBetween = ChronoUnit.MONTHS.between(start.toLocalDate().withDayOfMonth(1), targetDay.withDayOfMonth(1))
                        if (monthsBetween >= 0 && monthsBetween % rule.interval == 0L) {
                            val occurrenceIndex = (monthsBetween / rule.interval).toInt() + 1
                            if (rule.count == null || occurrenceIndex <= rule.count) {
                                occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                            }
                        }
                    }
                }

                Frequency.YEARLY -> {
                    if (targetDay.dayOfMonth == start.dayOfMonth && targetDay.month == start.month) {
                        val yearsBetween = ChronoUnit.YEARS.between(start.toLocalDate(), targetDay)
                        if (yearsBetween >= 0 && yearsBetween % rule.interval == 0L) {
                            val occurrenceIndex = (yearsBetween / rule.interval).toInt() + 1
                            if (rule.count == null || occurrenceIndex <= rule.count) {
                                occurrences.add(LocalDateTime.of(targetDay, start.toLocalTime()))
                            }
                        }
                    }
                }
            }

            return occurrences
        }
    }
}