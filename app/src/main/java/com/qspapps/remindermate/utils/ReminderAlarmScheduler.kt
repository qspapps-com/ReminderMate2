package com.qspapps.remindermate.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderInstance
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class ReminderAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderActionDao: ReminderActionDao
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun schedule(reminder: Reminder, after: LocalDateTime? = null) {
        val actions = reminderActionDao.getActionsByReminderId(reminder.id)
        val nextOccurrence = ReminderInstance.getNextOccurrence(reminder, actions, after)

        if (nextOccurrence != null) {
            scheduleInstance(nextOccurrence)
        } else if (after == null) {
            cancel(reminder)
        }
    }

    fun scheduleInstance(instance: ReminderInstance) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_TRIGGER_REMINDER"
            putExtra("REMINDER_ID", instance.reminderId)
            putExtra("ORIGINAL_TIME", instance.originalTime)
            putExtra("TRIGGER_TIME", instance.displayTime) // LocalDateTime is serializable
        }

        // Using reminderId as request code to ensure there is only one alarm per reminder.
        val requestCode = instance.reminderId.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = instance.displayTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(reminder: Reminder) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_TRIGGER_REMINDER"
            putExtra("REMINDER_ID", reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
