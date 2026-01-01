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

    abstract suspend fun executeWork(): Result

    override suspend fun doWork(): Result {
        return try {
            val result = executeWork()

            // Fix: Check for success using equality with the static factory result
            // since 'Result.Success' is restricted to library groups.
            if (result == Result.success()) {
                userPrefs.updateWorkerRunTime(workName)
            }
            result
        } catch (e: Exception) {
            userPrefs.saveError("$workName Error: ${e.localizedMessage}")
            Result.retry()
        }
    }
}
