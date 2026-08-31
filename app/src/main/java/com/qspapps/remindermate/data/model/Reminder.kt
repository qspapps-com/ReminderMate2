package com.qspapps.remindermate.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

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
    private fun allOccurrences(): Sequence<LocalDateTime> {
        val occurrences = generateSequence(startDateTime) { current ->
            recurrence?.getNextOccurrence(current)
        }
        return recurrence?.count?.let(occurrences::take) ?: occurrences
    }

    fun getOccurrences(from: LocalDateTime, to: LocalDateTime): List<LocalDateTime> =
        allOccurrences()
            .dropWhile { it.isBefore(from) }
            .takeWhile { it.isBefore(to) }
            .toList()

    /**
     * The soonest instance strictly after [after] (or the very first instance when [after] is
     * null) that has not been completed or deleted.
     */
    fun getNextOccurrence(actions: List<ReminderAction>, after: LocalDateTime? = null): ReminderInstance? {
        val ignoredTimes = actions
            .filter { it.type == ActionType.COMPLETED || it.type == ActionType.DELETED }
            .mapTo(mutableSetOf()) { it.originalScheduledTime }

        // `after` is exclusive, so the sentinel has to sit just before the first occurrence.
        val searchFrom = after ?: startDateTime.minusNanos(1)
        val horizon = startDateTime.plusYears(SEARCH_HORIZON_YEARS)

        return allOccurrences()
            .takeWhile { it.isBefore(horizon) }
            .filter { it !in ignoredTimes }
            .map { originalTime ->
                val snoozeAction = actions.find {
                    it.originalScheduledTime == originalTime && it.type == ActionType.SNOOZED
                }
                ReminderInstance(
                    reminderId = id,
                    title = title,
                    description = description,
                    displayTime = snoozeAction?.rescheduledTime ?: originalTime,
                    originalTime = originalTime,
                    // Completed occurrences were filtered out above, so this is always pending.
                    isCompleted = false,
                    isSnoozed = snoozeAction != null,
                    isRecurring = recurrence != null
                )
            }
            .filter { it.displayTime.isAfter(searchFrom) }
            // Snoozing moves an occurrence later, so occurrences are not necessarily produced in
            // display order. Look at a small window and pick the earliest rather than the first.
            .take(SNOOZE_REORDER_WINDOW)
            .minByOrNull { it.displayTime }
    }

    private companion object {
        /** Recurring reminders repeat forever; stop generating occurrences at some point. */
        const val SEARCH_HORIZON_YEARS = 10L

        /**
         * How many occurrences to inspect when resolving "the next one". A snooze can push an
         * occurrence past later ones, and the UI only allows snoozing the pending instance, so a
         * handful of occurrences is always enough to find the true earliest.
         */
        const val SNOOZE_REORDER_WINDOW = 5
    }
}
