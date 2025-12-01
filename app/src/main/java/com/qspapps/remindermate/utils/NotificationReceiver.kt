package com.qspapps.remindermate.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.ReminderAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var reminderActionDao: ReminderActionDao

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var alarmScheduler: ReminderAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("REMINDER_ID", -1)
        if (reminderId == -1L) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        runBlocking { // Using runBlocking for simplicity. A better approach would be a custom coroutine scope.
            val reminder = reminderDao.getById(reminderId)
            if (reminder != null) {
                when (intent.action) {
                    "ACTION_TRIGGER_REMINDER" -> {
                        val triggerTime = intent.getSerializableExtra("TRIGGER_TIME") as? LocalDateTime ?: return@runBlocking
                        val originalTime = intent.getSerializableExtra("ORIGINAL_TIME") as? LocalDateTime ?: return@runBlocking

                        notificationService.showNotification(reminder, triggerTime, originalTime)
                        alarmScheduler.schedule(reminder, after = triggerTime)
                    }
                    "ACTION_COMPLETE" -> {
                        val originalTime = intent.getSerializableExtra("ORIGINAL_TIME") as? LocalDateTime ?: return@runBlocking
                        val action = ReminderAction(
                            reminderId = reminderId,
                            originalScheduledTime = originalTime,
                            type = ActionType.COMPLETED
                        )
                        reminderActionDao.insert(action)
                        notificationManager.cancel(reminder.id.toInt())
                        alarmScheduler.schedule(reminder)
                    }
                    "ACTION_SNOOZE" -> {
                        val originalTime = intent.getSerializableExtra("ORIGINAL_TIME") as? LocalDateTime ?: return@runBlocking
                        val snoozedTime = LocalDateTime.now().plusMinutes(5)
                        val action = ReminderAction(
                            reminderId = reminderId,
                            originalScheduledTime = originalTime,
                            type = ActionType.SNOOZED,
                            resheduledTime = snoozedTime
                        )
                        reminderActionDao.insert(action)
                        notificationManager.cancel(reminder.id.toInt())
                        alarmScheduler.schedule(reminder)
                    }
                }
            }
        }
    }
}
