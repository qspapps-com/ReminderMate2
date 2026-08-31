package com.qspapps.remindermate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.di.ApplicationScope
import com.qspapps.remindermate.utils.DateTimeUtils.minsFromNow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderRepository: ReminderRepository
    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var alarmScheduler: ReminderAlarmScheduler
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationService.EXTRA_REMINDER_ID, -1)
        if (reminderId == -1L) return

        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                val reminder = reminderRepository.getReminderById(reminderId)
                val originalTime = intent.serializableExtra<LocalDateTime>(
                    NotificationService.EXTRA_ORIGINAL_TIME
                )
                if (reminder != null && originalTime != null) {
                    handle(intent, reminder, originalTime)
                }
            } catch (e: Exception) {
                // Log the error to DataStore so it appears in Settings
                userPreferencesRepository.saveError("Notification Error: ${e.localizedMessage}")
            } finally {
                pendingResult.finish() // Ensure finish() is called to end the broadcast
            }
        }
    }

    private suspend fun handle(intent: Intent, reminder: Reminder, originalTime: LocalDateTime) {
        when (intent.action) {
            NotificationService.ACTION_TRIGGER_REMINDER -> {
                val triggerTime = intent.serializableExtra<LocalDateTime>(
                    NotificationService.EXTRA_TRIGGER_TIME
                ) ?: return
                notificationService.showNotification(reminder, triggerTime, originalTime)
                alarmScheduler.schedule(reminder, after = triggerTime)
            }

            NotificationService.ACTION_COMPLETE ->
                recordAction(reminder, ReminderAction(reminder.id, originalTime, ActionType.COMPLETED))

            NotificationService.ACTION_SNOOZE -> {
                val snoozeMins = intent.getLongExtra(
                    NotificationService.EXTRA_SNOOZE_MINS,
                    NotificationService.SNOOZE_MINUTES
                )
                recordAction(
                    reminder,
                    ReminderAction(
                        reminderId = reminder.id,
                        originalScheduledTime = originalTime,
                        type = ActionType.SNOOZED,
                        rescheduledTime = minsFromNow(snoozeMins)
                    )
                )
            }
        }
    }

    private suspend fun recordAction(reminder: Reminder, action: ReminderAction) {
        reminderRepository.insertAction(action)
        notificationService.cancelNotification(reminder.id)
        alarmScheduler.schedule(reminder)
    }
}

private inline fun <reified T : Serializable> Intent.serializableExtra(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(key) as? T
    }
