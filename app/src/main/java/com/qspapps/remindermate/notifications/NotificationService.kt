package com.qspapps.remindermate.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.qspapps.remindermate.MainActivity
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.utils.Constants
import com.qspapps.remindermate.utils.DateTimeUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            action = "ACTION_COMPLETE"
            putExtra("REMINDER_ID", reminder.id)
            putExtra("ORIGINAL_TIME", originalTime)
        }

        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 4, // Unique request code
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeTime = DateTimeUtils.minsFromNow(Constants.SNOOZE_MINUTES)
        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("REMINDER_ID", reminder.id)
            putExtra("ORIGINAL_TIME", originalTime)
            putExtra("SNOOZED_TIME", snoozeTime)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt() * 4 + 1, // Unique request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)

        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_alarm_on_24)
            .setLargeIcon(largeIcon)
            .setContentTitle(reminder.title)
            .setContentText(reminder.description ?: "Due at ${triggerTime.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            .setContentIntent(activityPendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Snooze +${Constants.SNOOZE_MINUTES} mins", snoozePendingIntent)
            .build()

        notificationManager.notify(reminder.id.toInt(), notification)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "REMINDER_CHANNEL_ID"
    }
}
