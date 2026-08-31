package com.qspapps.remindermate.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderAlarmScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun schedule(reminder: Reminder, after: LocalDateTime? = null) {
        val actions = reminderRepository.getActionsByReminderId(reminder.id)
        val nextOccurrence = reminder.getNextOccurrence(actions, after)

        if (nextOccurrence != null) {
            scheduleInstance(nextOccurrence)
        } else if (after == null) {
            cancel(reminder)
        }
    }

    fun scheduleInstance(instance: ReminderInstance) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationService.ACTION_TRIGGER_REMINDER
            putExtra(NotificationService.EXTRA_REMINDER_ID, instance.reminderId)
            putExtra(NotificationService.EXTRA_ORIGINAL_TIME, instance.originalTime)
            putExtra(NotificationService.EXTRA_TRIGGER_TIME, instance.displayTime)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationService.alarmRequestCode(instance.reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis =
            instance.displayTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Exact alarms need a user-granted permission from API 31; fall back to an inexact one.
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancel(reminder: Reminder) {
        // PendingIntent matching ignores extras, so only the action and request code matter here.
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationService.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationService.alarmRequestCode(reminder.id),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}
