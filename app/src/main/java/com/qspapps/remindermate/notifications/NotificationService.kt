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
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showNotification(reminder: Reminder, triggerTime: LocalDateTime, originalTime: LocalDateTime) {
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_alarm_on_24)
            .setContentTitle(reminder.title)
            .setContentText(
                reminder.description ?: context.getString(
                    R.string.notification_due_at,
                    DateTimeUtils.formatTime(triggerTime)
                )
            )
            .setContentIntent(openAppIntent(requestCode(reminder.id, SLOT_CONTENT)))
            .setAutoCancel(true)
            .addAction(
                0,
                context.getString(R.string.notification_action_complete),
                actionIntent(reminder.id, ACTION_COMPLETE, originalTime, SLOT_COMPLETE)
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_snooze, SNOOZE_MINUTES),
                actionIntent(reminder.id, ACTION_SNOOZE, originalTime, SLOT_SNOOZE) {
                    putExtra(EXTRA_SNOOZE_MINS, SNOOZE_MINUTES)
                }
            )
            .build()

        notificationManager.notify(notificationId(reminder.id), notification)
    }

    fun showOverdueSummaryNotification(count: Int) {
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.outline_alarm_on_24)
            .setContentTitle(context.getString(R.string.overdue_reminders_title))
            .setContentText(context.getString(R.string.overdue_reminders_message, count))
            .setContentIntent(
                openAppIntent(REQUEST_CODE_OVERDUE_SUMMARY, targetScreen = TARGET_SCREEN_OVERDUE)
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OVERDUE_SUMMARY, notification)
    }

    fun cancelNotification(reminderId: Long) {
        notificationManager.cancel(notificationId(reminderId))
    }

    private fun openAppIntent(requestCode: Int, targetScreen: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            targetScreen?.let { putExtra(EXTRA_TARGET_SCREEN, it) }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionIntent(
        reminderId: Long,
        action: String,
        originalTime: LocalDateTime,
        slot: Int,
        extras: Intent.() -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_ORIGINAL_TIME, originalTime)
            extras()
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(reminderId, slot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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
        const val EXTRA_TARGET_SCREEN = "TARGET_SCREEN"
        const val TARGET_SCREEN_OVERDUE = "overdue"
        const val REMINDER_CHANNEL_ID = "REMINDER_CHANNEL_ID"

        /**
         * Notification and request-code namespaces are derived from the reminder's row id, so
         * they must not collide with the app-wide ids below. Both are placed at the extremes of
         * the Int range, which an AUTOINCREMENT primary key will never reach.
         */
        const val NOTIFICATION_ID_OVERDUE_SUMMARY = Int.MAX_VALUE
        const val REQUEST_CODE_OVERDUE_SUMMARY = Int.MIN_VALUE

        /** Distinct PendingIntents per reminder. Widen [REQUEST_CODE_SLOTS] before adding more. */
        private const val REQUEST_CODE_SLOTS = 4
        private const val SLOT_ALARM = 0
        private const val SLOT_COMPLETE = 1
        private const val SLOT_SNOOZE = 2
        private const val SLOT_CONTENT = 3

        fun notificationId(reminderId: Long): Int = reminderId.toInt()

        /**
         * A request code unique to a (reminder, slot) pair. Computed in [Long] so the multiply
         * cannot overflow into a neighbouring reminder's slots before narrowing.
         */
        fun requestCode(reminderId: Long, slot: Int): Int =
            (reminderId * REQUEST_CODE_SLOTS + slot).toInt()

        fun alarmRequestCode(reminderId: Long): Int = requestCode(reminderId, SLOT_ALARM)
    }
}
