package com.qspapps.remindermate.ui.overduereminders

import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.ui.core.ReminderViewModel
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class OverdueRemindersUiState(
    val overdueReminders: List<ReminderInstance> = emptyList(),
    val isLoading: Boolean = false,
    val defaultTimes: List<LocalTime> = emptyList()
)

@HiltViewModel
class OverdueRemindersViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    userPreferencesRepository: UserPreferencesRepository, // Injected
    reminderAlarmScheduler: ReminderAlarmScheduler,
    notificationService: NotificationService
) : ReminderViewModel(reminderRepository, reminderAlarmScheduler, notificationService) {

    val uiState: StateFlow<OverdueRemindersUiState> = combine(
        reminderRepository.getAllReminders(),
        reminderRepository.getAllActions(),
        userPreferencesRepository.defaultReminderTimes
    ) { reminders, actions, defaultTimes ->
        val overdue = ReminderInstance.getOverdueReminders(
            reminders = reminders,
            actions = actions,
            currentTime = LocalDateTime.now(),
        )

        OverdueRemindersUiState(overdueReminders = overdue, isLoading = false, defaultTimes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverdueRemindersUiState(isLoading = true)
    )
}
