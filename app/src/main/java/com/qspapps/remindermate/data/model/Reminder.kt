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
            // 1. Safety limit to prevent infinite loops on poorly defined rules
            .takeWhile { it.isBefore(startDateTime.plusYears(10)) }
            // 2. Map to instances so we can see the 'displayTime' (snooze logic)
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
            // 3. Filter out items that are finished/deleted
            .filter { !ignoredTimes.contains(it.originalTime) }
            // 4. IMPORTANT: Filter for things happening AFTER our search point first
            // We use a buffer of original occurrences to find potential "next" candidates
            .filter { it.displayTime.isAfter(searchFrom) }
            // 5. Take a small window of candidates to check for chronological "swaps"
            // (e.g. if r1 was snoozed past r2)
            .take(5)
            .toList().minByOrNull { it.displayTime }
    }
}
