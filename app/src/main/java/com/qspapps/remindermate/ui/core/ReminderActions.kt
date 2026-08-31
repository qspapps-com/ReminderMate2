package com.qspapps.remindermate.ui.core

import com.qspapps.remindermate.data.model.ReminderInstance
import java.time.LocalDateTime

/**
 * The callbacks a reminder row needs. Bundled so rows take one stable parameter instead of five,
 * and so screens can build it once per screen rather than once per row.
 */
data class ReminderActions(
    val onCompletedChange: (ReminderInstance) -> Unit,
    val onSnooze: (ReminderInstance, LocalDateTime) -> Unit,
    val onDeleteInstance: (ReminderInstance) -> Unit,
    val onDeleteReminder: (Long) -> Unit,
    val onUpdate: (Long) -> Unit
)
