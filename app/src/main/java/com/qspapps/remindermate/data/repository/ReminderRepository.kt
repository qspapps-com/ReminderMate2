package com.qspapps.remindermate.data.repository

import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.coroutines.flow.Flow

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
        reminderDao.deleteById(id)
    }

    fun getActionsForReminder(reminderId: Long): Flow<List<ReminderAction>> = reminderActionDao.getActionsForReminder(reminderId)

    fun getAllActions(): Flow<List<ReminderAction>> = reminderActionDao.getAllActions()

    suspend fun insertAction(reminderAction: ReminderAction) {
        reminderActionDao.insert(reminderAction)
    }

    suspend fun deleteAction(reminderAction: ReminderAction) {
        reminderActionDao.delete(reminderAction)
    }
}
