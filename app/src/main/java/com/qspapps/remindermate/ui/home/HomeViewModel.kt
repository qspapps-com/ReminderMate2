package com.qspapps.remindermate.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.ReminderAction
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.model.ReminderScheduler
import com.qspapps.remindermate.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val reminders: List<ReminderInstance> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRemindersForDay(LocalDate.now())
    }

    fun loadRemindersForDay(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDate = date) }
            val remindersFlow = reminderRepository.getAllReminders()
            val actionsFlow = reminderRepository.getAllActions()

            remindersFlow.combine(actionsFlow) { reminders, actions ->
                reminderScheduler.getRemindersForDay(date, reminders, actions)
            }.collect { remindersForDay ->
                _uiState.update {
                    it.copy(
                        reminders = remindersForDay,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun completeReminder(reminderInstance: ReminderInstance) {
        viewModelScope.launch {
            val action = ReminderAction(
                reminderId = reminderInstance.reminderId,
                originalScheduledTime = reminderInstance.originalTime,
                type = ActionType.COMPLETED
            )
            reminderRepository.insertAction(action)
        }
    }

    fun snoozeReminder(reminderInstance: ReminderInstance) {
        viewModelScope.launch {
            val action = ReminderAction(
                reminderId = reminderInstance.reminderId,
                originalScheduledTime = reminderInstance.originalTime,
                type = ActionType.SNOOZED,
                resheduledTime = reminderInstance.displayTime.plusMinutes(10)
            )
            reminderRepository.insertAction(action)
        }
    }
}
