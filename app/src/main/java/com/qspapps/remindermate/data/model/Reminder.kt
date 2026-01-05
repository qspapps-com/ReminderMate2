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
    private fun getAllOccurrencesSequence(): Sequence<LocalDateTime> {
        return generateSequence(startDateTime) { current ->
            recurrence?.getNextOccurrence(current)
        }.let { sequence ->
            val count = recurrence?.count
            if (count != null) sequence.take(count) else sequence
        }
    }

    fun getOccurrences(from: LocalDateTime, to: LocalDateTime): List<LocalDateTime> {
        return getAllOccurrencesSequence()
            .dropWhile { it.isBefore(from) }
            .takeWhile { it.isBefore(to) }
            .toList()
    }

    fun getNextOccurrence(actions: List<ReminderAction>, after: LocalDateTime? = null): ReminderInstance? {
        val ignoredTimes = actions
            .filter { it.type == ActionType.COMPLETED || it.type == ActionType.DELETED }
            .map { it.originalScheduledTime }
            .toSet()

        val searchFrom = after ?: startDateTime.minusNanos(1)

        return getAllOccurrencesSequence()
            .takeWhile { it.isBefore(startDateTime.plusYears(10)) }
            // 1. Filter out ignored occurrences immediately
            .filter { !ignoredTimes.contains(it) }
            // 2. Map only valid occurrences to instances
            .map { originalTime ->
                val snoozeAction = actions.find {
                    it.originalScheduledTime == originalTime && it.type == ActionType.SNOOZED
                }
                val displayTime = snoozeAction?.rescheduledTime ?: originalTime

                ReminderInstance(
                    reminderId = id,
                    title = title,
                    description = description,
                    displayTime = displayTime,
                    originalTime = originalTime,
                    isCompleted = false,
                    isSnoozed = snoozeAction != null,
                    isRecurring = recurrence != null
                )
            }
            // 3. Filter for items appearing AFTER our search point
            .filter { it.displayTime.isAfter(searchFrom) }
            // 4. Take a buffer and sort to handle chronological 'swaps' (snoozes)
            .take(5)
            .toList()
            .minByOrNull { it.displayTime }
    }
}
