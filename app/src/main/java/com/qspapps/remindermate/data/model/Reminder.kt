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

        // Start from the very first scheduled time
        var current = startDateTime
        var countSoFar = 1

        // 1. Skip occurrences that happen before the 'from' date
        while (current.toLocalDate().isBefore(from)) {
            val next = recurrence?.getNextOccurrence(current) ?: break

            current = next
            countSoFar++
        }

        // 2. Collect occurrences within the [from, to] range
        while (!current.toLocalDate().isAfter(to)) {
            // Check if we've exceeded the maximum allowed occurrences
            if (recurrence?.count != null && countSoFar > recurrence.count) {
                break
            }

            // If it falls within our target window, add it
            if (!current.toLocalDate().isBefore(from)) {
                occurrences.add(current)
            }

            val next = recurrence?.getNextOccurrence(current) ?: break

            current = next
            countSoFar++
        }

        return occurrences
    }

    fun getNextOccurrence(actions: List<ReminderAction>, after: LocalDateTime? = null): ReminderInstance? {
        val ignoredTimes = actions.filter { it.type == ActionType.COMPLETED || it.type == ActionType.DELETED }
            .map { it.originalScheduledTime }
            .toSet()

        val searchFrom = after ?: startDateTime.minusNanos(1)

        // Find the first occurrence after searchFrom that isn't ignored
        var currentOriginal = startDateTime
        var currentCount = 1

        while (true) {
            val isIgnored = ignoredTimes.contains(currentOriginal)
            val snoozeAction = actions.find { it.originalScheduledTime == currentOriginal && it.type == ActionType.SNOOZED }
            val displayTime = snoozeAction?.rescheduledTime ?: currentOriginal

            if (!isIgnored && displayTime.isAfter(searchFrom)) {
                return ReminderInstance(
                    reminderId = id, title = title, description = description,
                    displayTime = displayTime, originalTime = currentOriginal,
                    isCompleted = false, isSnoozed = snoozeAction != null,
                    isRecurring = recurrence != null
                )
            }

            // Move to next iteration
            if (recurrence == null) break
            currentOriginal = recurrence.getNextOccurrence(currentOriginal)
            currentCount++

            // Terminate if count exceeded or if we've searched too far (10 years)
            if (recurrence.count != null && currentCount > recurrence.count) break
            if (currentOriginal.isAfter(startDateTime.plusYears(10))) break
        }

        return null
    }
}
