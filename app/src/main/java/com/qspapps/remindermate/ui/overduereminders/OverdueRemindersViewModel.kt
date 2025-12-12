package com.qspapps.remindermate.ui.overduereminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class OverdueRemindersUiState(
    val overdueReminders: List<ReminderInstance> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class OverdueRemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

    val uiState: StateFlow<OverdueRemindersUiState> = combine(
        reminderRepository.getAllReminders(),
        reminderRepository.getAllActions()
    ) { reminders, actions ->
        val now = LocalDateTime.now()
        val overdue = ReminderInstance.getReminderInstances(reminders, actions, LocalDateTime.MIN, now)
            .filter { !it.isCompleted && it.displayTime.isBefore(now) }

        OverdueRemindersUiState(overdueReminders = overdue)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OverdueRemindersUiState(isLoading = true)
    )

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderId)
            if (reminder != null) {
                reminderAlarmScheduler.cancel(reminder)
            }
            reminderRepository.deleteReminderById(reminderId)
        }
    }
}
