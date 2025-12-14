package com.qspapps.remindermate.ui.core

import com.qspapps.remindermate.data.model.ReminderInstance
import java.time.LocalDateTime

// Define this in a separate file or at the top of your file
data class ReminderActions(
    val onCompletedChange: (ReminderInstance) -> Unit,
    val onSnooze: (ReminderInstance, LocalDateTime) -> Unit,
    val onDeleteInstance: (ReminderInstance) -> Unit,
    val onDeleteReminder: (Long) -> Unit,
    val onUpdate: (Long) -> Unit
)