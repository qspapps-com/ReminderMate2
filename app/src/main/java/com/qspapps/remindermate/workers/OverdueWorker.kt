package com.qspapps.remindermate.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.NotificationService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

@HiltWorker
class OverdueWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val notificationService: NotificationService,
    userPrefs: UserPreferencesRepository
) : BaseReminderWorker(
    context,
    params,
    userPrefs,
    workName = WORK_NAME
) {
    companion object {
        const val WORK_NAME = "OverdueWork"
        const val REPEAT_INTERVAL_HOURS = 24L
    }

    override suspend fun executeWork(): Result {
        val reminders = reminderRepository.getAllReminders().first()
        val actions = reminderRepository.getAllActions().first()

        val overdueList = ReminderInstance.getOverdueReminders(
            reminders = reminders,
            actions = actions,
            currentTime = LocalDateTime.now()
        )

        if (overdueList.isNotEmpty()) {
            notificationService.showOverdueSummaryNotification(overdueList.size)
        }
        return Result.success()
    }
}