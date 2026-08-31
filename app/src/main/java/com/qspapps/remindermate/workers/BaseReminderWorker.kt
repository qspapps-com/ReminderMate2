package com.qspapps.remindermate.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.qspapps.remindermate.data.repository.UserPreferencesRepository

abstract class BaseReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val userPrefs: UserPreferencesRepository,
    val workName: String
) : CoroutineWorker(context, params) {

    /** Performs the work. Throwing marks the run as failed and schedules a retry. */
    protected abstract suspend fun executeWork()

    final override suspend fun doWork(): Result = try {
        executeWork()
        userPrefs.updateWorkerRunTime(workName)
        Result.success()
    } catch (e: Exception) {
        userPrefs.saveError("$workName Error: ${e.localizedMessage}")
        Result.retry()
    }
}
