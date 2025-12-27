package com.qspapps.remindermate.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.qspapps.remindermate.MainActivity
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.utils.DateTimeUtils
import java.time.LocalDateTime

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showNotification(reminder: Reminder, triggerTime: LocalDateTime, originalTime: LocalDateTime) {
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val activityPendingIntent = PendingIntent.getActivity(
            context,
            1,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_ORIGINAL_TIME, originalTime)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 4, // Unique request code
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_ORIGINAL_TIME, originalTime)
            putExtra(EXTRA_SNOOZE_MINS, SNOOZE_MINUTES)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 4 + 1, // Unique request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_alarm_on_24)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description ?: context.getString(R.string.notification_due_at,
                DateTimeUtils.formatTime(triggerTime)))
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notification_action_complete), completePendingIntent)
            .addAction(0, context.getString(R.string.notification_action_snooze, SNOOZE_MINUTES), snoozePendingIntent)
            .build()

        notificationManager.notify(reminder.id.toInt(), notification)
    }
    fun showOverdueSummaryNotification(count: Int) {
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            // You might want to add an extra here to navigate specifically to Overdue screen
            putExtra("TARGET_SCREEN", "overdue")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OVERDUE_SUMMARY, // Unique request code for summary
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_alarm_on_24)
            .setContentTitle(context.getString(R.string.overdue_reminders_title))
            .setContentText(context.getString(R.string.overdue_reminders_message, count))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OVERDUE_SUMMARY, notification)
    }


    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    companion object {
        const val SNOOZE_MINUTES = 15L // Define snooze duration in one place
        const val ACTION_COMPLETE = "ACTION_COMPLETE"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val ACTION_TRIGGER_REMINDER = "ACTION_TRIGGER_REMINDER"
        const val EXTRA_REMINDER_ID = "REMINDER_ID"
        const val EXTRA_ORIGINAL_TIME = "ORIGINAL_TIME"
        const val EXTRA_TRIGGER_TIME = "TRIGGER_TIME"
        const val EXTRA_SNOOZE_MINS = "SNOOZE_MINS"
        const val REMINDER_CHANNEL_ID = "REMINDER_CHANNEL_ID"
        const val NOTIFICATION_ID_OVERDUE_SUMMARY = 1000001
        const val REQUEST_CODE_OVERDUE_SUMMARY = -100
    }
}
