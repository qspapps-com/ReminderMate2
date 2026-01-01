package com.qspapps.remindermate.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime

@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    userPrefs: UserPreferencesRepository
) : BaseReminderWorker(
    context,
    params,
    userPrefs,
    workName = WORK_NAME
) {
    companion object {
        const val WORK_NAME = "CleanupWork"
        const val REPEAT_INTERVAL_DAYS = 7L
    }

    override suspend fun executeWork(): Result {
        val threshold = LocalDateTime.now().minusDays(30)
        reminderRepository.cleanupOldReminders(threshold)
        return Result.success()
    }
}