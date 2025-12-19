package com.qspapps.remindermate.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.ReminderAction
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

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var reminderActionDao: ReminderActionDao

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var alarmScheduler: ReminderAlarmScheduler

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationService.EXTRA_REMINDER_ID, -1)
        if (reminderId == -1L) return

        val pendingResult = goAsync()

        applicationScope.launch { // Using created scope to launch a coroutine
            try {
                val reminder = reminderDao.getById(reminderId)
                if (reminder != null) {
                    when (intent.action) {
                        NotificationService.ACTION_TRIGGER_REMINDER -> {
                            val triggerTime = intent.getSerializableExtraCompatible<LocalDateTime>(NotificationService.EXTRA_TRIGGER_TIME) ?: return@launch
                            val originalTime = intent.getSerializableExtraCompatible<LocalDateTime>(NotificationService.EXTRA_ORIGINAL_TIME) ?: return@launch

                            notificationService.showNotification(reminder, triggerTime, originalTime)
                            alarmScheduler.schedule(reminder, after = triggerTime)
                        }
                        NotificationService.ACTION_COMPLETE -> {
                            val originalTime = intent.getSerializableExtraCompatible<LocalDateTime>(NotificationService.EXTRA_ORIGINAL_TIME) ?: return@launch
                            val action = ReminderAction(
                                reminderId = reminderId,
                                originalScheduledTime = originalTime,
                                type = ActionType.COMPLETED
                            )
                            reminderActionDao.insert(action)
                            notificationService.cancelNotification(reminder.id.toInt())
                            alarmScheduler.schedule(reminder)
                        }
                        NotificationService.ACTION_SNOOZE -> {
                            val originalTime = intent.getSerializableExtraCompatible<LocalDateTime>(NotificationService.EXTRA_ORIGINAL_TIME) ?: return@launch
                            val snoozeMins = intent.getLongExtra(NotificationService.EXTRA_SNOOZE_MINS, 15)
                            val action = ReminderAction(
                                reminderId = reminderId,
                                originalScheduledTime = originalTime,
                                type = ActionType.SNOOZED,
                                rescheduledTime = minsFromNow(snoozeMins)
                            )
                            reminderActionDao.insert(action)
                            notificationService.cancelNotification(reminder.id.toInt())
                            alarmScheduler.schedule(reminder)
                        }
                    }
                }
            } finally {
                pendingResult.finish() // Ensure finish() is called to end the broadcast
            }
        }
    }
}

private inline fun <reified T : Serializable> Intent.getSerializableExtraCompatible(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSerializableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getSerializableExtra(key) as? T
    }
}
