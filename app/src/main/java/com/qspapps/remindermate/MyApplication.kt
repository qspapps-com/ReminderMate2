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
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.workers.CleanupWorker
import com.qspapps.remindermate.workers.OverdueWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var userPrefs: UserPreferencesRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        installCrashRecorder()
        createNotificationChannel()
        scheduleTasks()
    }

    /**
     * Records the crash so it shows up in Settings. The write has to block, since the process is
     * about to die, but it is bounded: a slow disk must not turn a crash into an ANR.
     */
    private fun installCrashRecorder() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runBlocking {
                withTimeoutOrNull(CRASH_SAVE_TIMEOUT_MS) {
                    userPrefs.saveError("App Crash: ${throwable.localizedMessage}")
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun scheduleTasks() {
        val workManager = WorkManager.getInstance(this)

        // 1. Weekly Cleanup
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(CleanupWorker.REPEAT_INTERVAL_DAYS,
            TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )

        // 2. Daily Overdue Check
        val delay = delayUntilNext(OVERDUE_CHECK_HOUR)
        val overdueRequest = PeriodicWorkRequestBuilder<OverdueWorker>(OverdueWorker.REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            OverdueWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            overdueRequest
        )
    }

    /** Millis until the next occurrence of [hour] o'clock, local time. */
    private fun delayUntilNext(hour: Int): Long {
        val now = LocalDateTime.now()
        val today = now.withHour(hour).truncatedTo(ChronoUnit.HOURS)
        val target = if (now.isAfter(today)) today.plusDays(1) else today
        return ChronoUnit.MILLIS.between(now, target)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NotificationService.REMINDER_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = getString(R.string.notification_channel_description) }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private companion object {
        const val CRASH_SAVE_TIMEOUT_MS = 500L
        const val OVERDUE_CHECK_HOUR = 6
    }
}
