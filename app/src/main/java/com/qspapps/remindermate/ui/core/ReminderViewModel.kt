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
            val action = reminderInstance.actionOf(ActionType.COMPLETED)
            if (reminderInstance.isCompleted) { // Is completed, so user wants to un-complete
                reminderRepository.deleteAction(action)
                // Schedule this one again
                reminderAlarmScheduler.scheduleInstance(reminderInstance.copy(isCompleted = false))
            } else { // Is not completed, so user wants to complete
                reminderRepository.insertAction(action)
                notificationService.cancelNotification(reminderInstance.reminderId)
                reminderAlarmScheduler.cancel(reminder)
                // Schedule the next occurrence if this reminder recurs
                reminderAlarmScheduler.schedule(reminder, after = reminderInstance.displayTime)
            }
        }
    }

    fun snoozeReminder(reminderInstance: ReminderInstance, newTime: LocalDateTime) {
        viewModelScope.launch {
            reminderRepository.insertAction(
                reminderInstance.actionOf(ActionType.SNOOZED, rescheduledTime = newTime)
            )
            notificationService.cancelNotification(reminderInstance.reminderId)
            reminderAlarmScheduler.scheduleInstance(
                reminderInstance.copy(displayTime = newTime, isCompleted = false)
            )
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository.getReminderById(reminderId)?.let { reminder ->
                notificationService.cancelNotification(reminderId)
                reminderAlarmScheduler.cancel(reminder)
            }
            reminderRepository.deleteReminderById(reminderId)
        }
    }

    fun deleteReminderInstance(reminderInstance: ReminderInstance) {
        viewModelScope.launch {
            reminderRepository.insertAction(reminderInstance.actionOf(ActionType.DELETED))
            notificationService.cancelNotification(reminderInstance.reminderId)
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

private fun ReminderInstance.actionOf(
    type: ActionType,
    rescheduledTime: LocalDateTime? = null
) = ReminderAction(
    reminderId = reminderId,
    originalScheduledTime = originalTime,
    type = type,
    rescheduledTime = rescheduledTime
)
