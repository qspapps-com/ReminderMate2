package com.qspapps.remindermate.ui.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.ReminderAction
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import kotlinx.coroutines.launch
import java.time.LocalDateTime

abstract class ReminderViewModel(
    protected val reminderRepository: ReminderRepository,
    protected val reminderAlarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

    fun toggleCompleted(reminderInstance: ReminderInstance) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderInstance.reminderId) ?: return@launch
            if (reminderInstance.isCompleted) { // Is completed, so user wants to un-complete
                val action = ReminderAction(
                    reminderId = reminderInstance.reminderId,
                    originalScheduledTime = reminderInstance.originalTime,
                    type = ActionType.COMPLETED
                )
                reminderRepository.deleteAction(action)
                // Schedule this one again
                reminderAlarmScheduler.scheduleInstance(reminderInstance.copy(isCompleted = false))
            } else { // Is not completed, so user wants to complete
                val action = ReminderAction(
                    reminderId = reminderInstance.reminderId,
                    originalScheduledTime = reminderInstance.originalTime,
                    type = ActionType.COMPLETED
                )
                reminderRepository.insertAction(action)
                // Schedule next one if recurring
                reminderAlarmScheduler.schedule(reminder, after = reminderInstance.displayTime)
            }
        }
    }

    fun snoozeReminder(reminderInstance: ReminderInstance, newTime: LocalDateTime) {
        viewModelScope.launch {
            val action = ReminderAction(
                reminderId = reminderInstance.reminderId,
                originalScheduledTime = reminderInstance.originalTime,
                type = ActionType.SNOOZED,
                rescheduledTime = newTime
            )
            reminderRepository.insertAction(action)
            // Schedule the snoozed instance
            val snoozedInstance = reminderInstance.copy(displayTime = newTime, isCompleted = false)
            reminderAlarmScheduler.scheduleInstance(snoozedInstance)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderId)
            if(reminder != null) {
                reminderAlarmScheduler.cancel(reminder)
            }
            reminderRepository.deleteReminderById(reminderId)
        }
    }

    fun deleteReminderInstance(reminderInstance: ReminderInstance) {
        viewModelScope.launch {
            val action = ReminderAction(
                reminderId = reminderInstance.reminderId,
                originalScheduledTime = reminderInstance.originalTime,
                type = ActionType.DELETED
            )
            reminderRepository.insertAction(action)
            // Since we're deleting an instance, we need to schedule the next one if it's a recurring reminder.
            val reminder = reminderRepository.getReminderById(reminderInstance.reminderId)
            if (reminder?.recurrence != null) {
                reminderAlarmScheduler.schedule(reminder, after = reminderInstance.displayTime)
            }
        }
    }
}
