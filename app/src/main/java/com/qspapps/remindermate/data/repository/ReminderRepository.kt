package com.qspapps.remindermate.data.repository

import androidx.compose.animation.core.copy
import androidx.compose.foundation.gestures.forEach
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class ReminderRepository(private val reminderDao: ReminderDao, private val reminderActionDao: ReminderActionDao) {

    fun getAllReminders(): Flow<List<Reminder>> = reminderDao.getAll()

    suspend fun getReminderById(id: Long): Reminder? = reminderDao.getById(id)

    suspend fun insert(reminder: Reminder): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder)
    }

    suspend fun deleteReminderById(id: Long) {
        // Associated actions are deleted using foreign key constraints
        reminderDao.deleteById(id)
    }

    suspend fun deleteAllReminders() {
        // Associated actions are deleted using foreign key constraints
        reminderDao.deleteAll()
    }

    suspend fun getActionsByReminderId(reminderId: Long): List<ReminderAction> = reminderActionDao.getActionsByReminderId(reminderId)

    fun getAllActions(): Flow<List<ReminderAction>> = reminderActionDao.getAllActions()

    suspend fun insertAction(reminderAction: ReminderAction) {
        reminderActionDao.insert(reminderAction)
    }

    suspend fun deleteAction(reminderAction: ReminderAction) {
        reminderActionDao.delete(reminderAction)
    }

    suspend fun deleteAllActions() {
        reminderActionDao.deleteAll()
    }

    suspend fun cleanupOldReminders(threshold: LocalDateTime) {
        val allReminders = reminderDao.getAll().first()
        val allActions = reminderActionDao.getAllActions().first().groupBy { it.reminderId }

        for (reminder in allReminders) {
            val actions = allActions[reminder.id] ?: emptyList()
            
            if (reminder.recurrence == null) {
                // One-off reminder: Delete if completed or deleted before threshold
                val terminalAction = actions.find { it.type == ActionType.COMPLETED || it.type == ActionType.DELETED }
                if (terminalAction != null && reminder.startDateTime.isBefore(threshold)) {
                    deleteReminderById(reminder.id)
                }
            } else {
                // Recurring reminder
                // Find all occurrences from startDateTime up to threshold
                val occurrencesBeforeThreshold = reminder.getOccurrences(reminder.startDateTime, threshold)

                var firstRemainingTime: LocalDateTime? = null
                
                // Find the first instance that is NOT completed and NOT deleted before threshold
                for (time in occurrencesBeforeThreshold) {
                    val action = actions.find { it.originalScheduledTime == time }
                    if (action?.type != ActionType.COMPLETED && action?.type != ActionType.DELETED) {
                        firstRemainingTime = time
                        break
                    }
                }

                if (firstRemainingTime == null) {
                    // All instances before threshold are completed/deleted.
                    // Find the first raw occurrence at or after threshold (passing empty actions to get raw schedule)
                    firstRemainingTime = reminder.getNextOccurrence(emptyList(), threshold.minusNanos(1))?.originalTime
                }

                if (firstRemainingTime != null) {
                    if (firstRemainingTime != reminder.startDateTime) {
                        update(reminder.copy(startDateTime = firstRemainingTime))
                        // Clean up actions that are now before the new startDateTime
                        actions.filter { it.originalScheduledTime.isBefore(firstRemainingTime!!) }
                            .forEach { reminderActionDao.delete(it) }
                    }
                } else {
                    // No more occurrences ever and all past ones are handled
                    deleteReminderById(reminder.id)
                }
            }
        }
    }

    suspend fun restoreBackupData(reminders: List<Reminder>, actions: List<ReminderAction>): List<Reminder> {
        val idMap = mutableMapOf<Long, Long>()
        val newReminders = mutableListOf<Reminder>()

        // 1. Insert Reminders and store the new IDs
        reminders.forEach { reminder ->
            val oldId = reminder.id
            // Insert with id = 0 to let Room generate a new auto-increment ID
            val newId = insert(reminder.copy(id = 0))
            idMap[oldId] = newId
            newReminders.add(reminder.copy(id = newId))
        }

        // 2. Insert Actions using the mapped Reminder IDs
        actions.forEach { action ->
            val newReminderId = idMap[action.reminderId]
            if (newReminderId != null) {
                insertAction(action.copy(reminderId = newReminderId))
            }
        }

        return newReminders
    }
}
