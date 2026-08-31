package com.qspapps.remindermate.data.model

import java.time.LocalDateTime

/**
 * A single occurrence of a [Reminder], resolved against the actions recorded against it.
 *
 * Instances are derived, never stored. See
 * [com.qspapps.remindermate.domain.ReminderSchedule] for the functions that build them.
 */
data class ReminderInstance(
    val reminderId: Long,
    val title: String,
    val description: String?,
    val displayTime: LocalDateTime, // The time to show on the UI (could be original or snoozed)
    val originalTime: LocalDateTime, // The original scheduled time
    val isCompleted: Boolean,
    val isSnoozed: Boolean,
    val isRecurring: Boolean
)
