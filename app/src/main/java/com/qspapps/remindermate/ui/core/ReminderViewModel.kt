package com.qspapps.remindermate.ui.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.ReminderAction
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import com.qspapps.remindermate.ui.navigation.AppScreen
import kotlinx.coroutines.launch
import java.time.LocalDateTime

abstract class ReminderViewModel(
    protected val reminderRepository: ReminderRepository,
    protected val reminderAlarmScheduler: ReminderAlarmScheduler,
    protected val notificationService: NotificationService
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
                notificationService.cancelNotification(reminderInstance.reminderId.toInt())
                // Cancel existing one from reminderAlarmScheduler
                reminderAlarmScheduler.cancel(reminder)
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
            notificationService.cancelNotification(reminderInstance.reminderId.toInt())
            // Schedule the snoozed instance
            val snoozedInstance = reminderInstance.copy(displayTime = newTime, isCompleted = false)
            reminderAlarmScheduler.scheduleInstance(snoozedInstance)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderId)
            if(reminder != null) {
                notificationService.cancelNotification(reminderId.toInt())
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
            notificationService.cancelNotification(reminderInstance.reminderId.toInt())
            // Since we're deleting an instance, we need to schedule the next one if it's a recurring reminder.
            val reminder = reminderRepository.getReminderById(reminderInstance.reminderId)
            if (reminder?.recurrence != null) {
                reminderAlarmScheduler.schedule(reminder, after = reminderInstance.displayTime)
            }
        }
    }

    fun getReminderActions(navController: NavController): ReminderActions {
        return ReminderActions(
            onCompletedChange = ::toggleCompleted,
            onSnooze = ::snoozeReminder,
            onDeleteInstance = ::deleteReminderInstance,
            onDeleteReminder = ::deleteReminder,
            onUpdate = {  reminderId ->
                navController.navigate(AppScreen.AddEditReminder.createRoute(reminderId))
            }
        )
    }
}
