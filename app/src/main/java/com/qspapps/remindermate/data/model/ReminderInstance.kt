package com.qspapps.remindermate.data.model

import java.time.LocalDateTime

data class ReminderInstance(
    val reminderId: Long,
    val title: String,
    val description: String?,
    val displayTime: LocalDateTime, // The time to show on the UI (could be original or snoozed)
    val originalTime: LocalDateTime, // The original scheduled time
    val isCompleted: Boolean,
    val isSnoozed: Boolean
)
