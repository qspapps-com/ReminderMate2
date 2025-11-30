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
import java.time.LocalDateTime
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val reminders: List<ReminderInstance> = emptyList(),
    val isLoading: Boolean = false,
    val showCompleted: Boolean = false
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
                val instances = reminderScheduler.getRemindersForDay(date, reminders, actions)
                if (_uiState.value.showCompleted) {
                    instances
                } else {
                    instances.filter { !it.isCompleted }
                }
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

    fun toggleShowCompleted() {
        _uiState.update { it.copy(showCompleted = !it.showCompleted) }
        loadRemindersForDay(_uiState.value.selectedDate)
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

    fun snoozeReminder(reminderInstance: ReminderInstance, newTime: LocalDateTime) {
        viewModelScope.launch {
            val action = ReminderAction(
                reminderId = reminderInstance.reminderId,
                originalScheduledTime = reminderInstance.originalTime,
                type = ActionType.SNOOZED,
                resheduledTime = newTime
            )
            reminderRepository.insertAction(action)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository.deleteReminderById(reminderId)
        }
    }
}
