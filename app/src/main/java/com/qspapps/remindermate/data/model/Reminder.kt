package com.qspapps.remindermate.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Serializable
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val startDateTime: LocalDateTime,
    val recurrence: RecurrenceRule? = null
) {
    fun getOccurrences(from: LocalDate, to: LocalDate): List<LocalDateTime> {
        val occurrences = mutableListOf<LocalDateTime>()
        var currentDate = if (from.isBefore(startDateTime.toLocalDate())) startDateTime.toLocalDate() else from

        while (!currentDate.isAfter(to)) {
            occurrences.addAll(calculateOccurrencesForDay(this, currentDate))
            currentDate = currentDate.plusDays(1)
        }
        return occurrences
    }

    fun getNextOccurrence(actions: List<ReminderAction>, after: LocalDateTime? = null): ReminderInstance? {
        val ignoredTimes = actions.filter { it.type == ActionType.COMPLETED || it.type == ActionType.DELETED }
            .map { it.originalScheduledTime }
            .toSet()

        val searchFrom = after ?: startDateTime.minusNanos(1)

        if (recurrence == null) {
            val originalTime = startDateTime
            if (ignoredTimes.contains(originalTime)) {
                return null
            }

            val snoozedAction = actions.firstOrNull { it.originalScheduledTime == originalTime && it.type == ActionType.SNOOZED }
            val displayTime = snoozedAction?.rescheduledTime ?: originalTime

            if (displayTime.isAfter(searchFrom)) {
                return ReminderInstance(
                    reminderId = id, title = title, description = description,
                    displayTime = displayTime, originalTime = originalTime,
                    isCompleted = false, isSnoozed = snoozedAction != null, isRecurring = false
                )
            }
            return null
        }

        var targetDay = searchFrom.toLocalDate()
        val daysSearchTo = lastRecurrenceEstimateDaysFromStartDate(this)

        while (targetDay <= daysSearchTo) { // Look ahead up to 10 years
            val occurrencesOnDay = calculateOccurrencesForDay(this, targetDay)

            for (originalTime in occurrencesOnDay) {
                if (ignoredTimes.contains(originalTime)) continue

                val snoozeAction = actions.find { it.originalScheduledTime == originalTime && it.type == ActionType.SNOOZED }
                val displayTime = snoozeAction?.rescheduledTime ?: originalTime

                if (displayTime.isAfter(searchFrom)) {
                    return ReminderInstance(
                        reminderId = id, title = title, description = description,
                        displayTime = displayTime, originalTime = originalTime,
                        isCompleted = false, isSnoozed = snoozeAction != null, isRecurring = true
                    )
                }
            }
            targetDay = targetDay.plusDays(1)
        }
        return null
    }

    private fun calculateOccurrencesForDay(reminder: Reminder, targetDay: LocalDate): List<LocalDateTime> {
        val occurrences = mutableListOf<LocalDateTime>()
        val start = reminder.startDateTime

        if (start.toLocalDate().isAfter(targetDay)) {
            return emptyList()
        }

        if (reminder.recurrence == null || reminder.recurrence.interval == 0) {
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
                                println("occurrencesFromFullCycles:$occurrencesFromFullCycles")
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

    private fun lastRecurrenceEstimateDaysFromStartDate(reminder: Reminder): LocalDate {
        if (reminder.recurrence == null) {
            return reminder.startDateTime.toLocalDate()
        }
        val recurrence = reminder.recurrence
        if (recurrence.count == null) {
            return reminder.startDateTime.toLocalDate().plusDays(365*10) // 10 years
        }
        val periodMinutes = when(recurrence.frequency) {
            Frequency.MINUTE -> 1
            Frequency.HOURLY -> 60
            Frequency.DAILY -> 60*24
            Frequency.WEEKLY -> 60*24*7
            Frequency.MONTHLY -> 60*24*31
            Frequency.YEARLY -> 60*24*365
        }
        val count = when(recurrence.frequency) {
            Frequency.WEEKLY if (recurrence.daysOfWeek != null && recurrence.daysOfWeek.isNotEmpty()) ->
                ceil(recurrence.count, recurrence.daysOfWeek.size)
            else -> recurrence.count
        }
        return reminder.startDateTime.toLocalDate().plusDays(ceil(
            recurrence.interval* periodMinutes*count, 24*60).toLong())
    }

    private fun getWeekValues(start: DayOfWeek): Map<DayOfWeek, Int> =
        DayOfWeek.entries.associateWith { (7 + it.value - start.value) % 7 }

    private fun ceil(num: Int, denom:Int): Int = (num + denom - 1) / denom
}
