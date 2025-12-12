package com.qspapps.remindermate.ui.home

import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.ui.core.ReminderViewModel
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val reminders: List<ReminderInstance> = emptyList(),
    val isLoading: Boolean = false,
    val showCompleted: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    reminderAlarmScheduler: ReminderAlarmScheduler
) : ReminderViewModel(reminderRepository, reminderAlarmScheduler) {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _showCompleted = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            _showCompleted.value = !userPreferencesRepository.hideCompleted.first()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        reminderRepository.getAllReminders(),
        reminderRepository.getAllActions(),
        _selectedDate,
        _showCompleted
    ) { reminders, actions, date, showCompleted ->
        val instances = ReminderInstance.getRemindersForDay(date, reminders, actions)
        val filteredInstances = if (showCompleted) {
            instances
        } else {
            instances.filter { !it.isCompleted }
        }
        HomeUiState(
            selectedDate = date,
            reminders = filteredInstances,
            isLoading = false,
            showCompleted = showCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun loadRemindersForDay(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleShowCompleted() {
        _showCompleted.update { !it }
    }
}
