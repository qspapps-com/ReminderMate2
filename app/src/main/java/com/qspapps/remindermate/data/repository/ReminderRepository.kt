package com.qspapps.remindermate.data.repository

import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(
    private val reminderDao: ReminderDao,
    private val reminderActionDao: ReminderActionDao
) {

    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll()

    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getById(id)

    suspend fun insert(reminder: Reminder): Long = reminderDao.insert(reminder)

    suspend fun update(reminder: Reminder) = reminderDao.update(reminder)

    // Associated actions are removed by the foreign key's ON DELETE CASCADE.
    suspend fun deleteReminderById(id: Long) = reminderDao.deleteById(id)

    suspend fun deleteAllReminders() = reminderDao.deleteAll()

    fun getAllActions(): Flow<List<ReminderAction>> = reminderActionDao.getAllActions()

    suspend fun getActionsByReminderId(reminderId: Long): List<ReminderAction> =
        reminderActionDao.getActionsByReminderId(reminderId)

    suspend fun insertAction(reminderAction: ReminderAction) = reminderActionDao.insert(reminderAction)

    suspend fun deleteAction(reminderAction: ReminderAction) = reminderActionDao.delete(reminderAction)

    suspend fun deleteAllActions() = reminderActionDao.deleteAll()

    /**
     * Drops reminders that are fully dealt with before [threshold], and moves surviving recurring
     * reminders forward so their start date is the earliest occurrence still pending.
     */
    suspend fun cleanupOldReminders(threshold: LocalDateTime) {
        val actionsByReminder = reminderActionDao.getAllActions().first().groupBy { it.reminderId }

        for (reminder in reminderDao.getAll().first()) {
            val actions = actionsByReminder[reminder.id].orEmpty()

            if (reminder.recurrence == null) {
                if (actions.any { it.isCleared() } && reminder.startDateTime.isBefore(threshold)) {
                    deleteReminderById(reminder.id)
                }
                continue
            }

            // The earliest past occurrence nobody has dealt with, else the first one still to come.
            val firstRemaining = reminder.getOccurrences(reminder.startDateTime, threshold)
                .firstOrNull { time ->
                    actions.none { it.originalScheduledTime == time && it.isCleared() }
                }
                ?: reminder.getNextOccurrence(emptyList(), threshold.minusNanos(1))?.originalTime

            when {
                // Nothing left, ever, and every past occurrence is handled.
                firstRemaining == null -> deleteReminderById(reminder.id)

                firstRemaining != reminder.startDateTime -> {
                    update(reminder.copy(startDateTime = firstRemaining))
                    // The actions before the new start date can no longer be reached.
                    actions.filter { it.originalScheduledTime.isBefore(firstRemaining) }
                        .forEach { reminderActionDao.delete(it) }
                }
            }
        }
    }

    /** Inserts a backup under fresh ids, remapping its actions. Returns the stored reminders. */
    suspend fun restoreBackupData(
        reminders: List<Reminder>,
        actions: List<ReminderAction>
    ): List<Reminder> {
        // id = 0 lets Room assign a new auto-increment id; keep the old id to remap actions.
        val restored = reminders.associateBy({ it.id }, { it.copy(id = insert(it.copy(id = 0))) })
        actions.forEach { action ->
            restored[action.reminderId]?.let { insertAction(action.copy(reminderId = it.id)) }
        }
        return restored.values.toList()
    }
}

private fun ReminderAction.isCleared() =
    type == ActionType.COMPLETED || type == ActionType.DELETED
