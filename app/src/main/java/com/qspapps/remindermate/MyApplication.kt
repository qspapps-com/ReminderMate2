package com.qspapps.remindermate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.workers.CleanupWorker
import com.qspapps.remindermate.workers.OverdueWorker
import dagger.hilt.android.HiltAndroidApp
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    override val workManagerConfiguration: Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleTasks()
    }
    private fun scheduleTasks() {
        val workManager = WorkManager.getInstance(this)

        // 1. Weekly Cleanup
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(7, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            "CleanupWork",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )

        // 2. Daily Overdue Check (Targeting 6 AM)
        val delay = calculateDelayUntilSixAM()
        val overdueRequest = PeriodicWorkRequestBuilder<OverdueWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "OverdueWork",
            ExistingPeriodicWorkPolicy.KEEP,
            overdueRequest
        )
    }

    private fun calculateDelayUntilSixAM(): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(6).withMinute(0).withSecond(0).withNano(0)
        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }
        return ChronoUnit.MILLIS.between(now, target)
    }

    private fun createNotificationChannel() {
        val name = "Reminder-Channel"
        val descriptionText = "Channel for Reminder notifications"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NotificationService.REMINDER_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
