package com.qspapps.remindermate.ui.home

import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.ui.core.ReminderViewModel
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val reminders: List<ReminderInstance> = emptyList(),
    val isLoading: Boolean = false,
    val showCompleted: Boolean = true,
    val defaultTimes: List<LocalTime> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    reminderAlarmScheduler: ReminderAlarmScheduler,
    notificationService: NotificationService
) : ReminderViewModel(reminderRepository, reminderAlarmScheduler, notificationService) {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _showCompleted = MutableStateFlow(true)
    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    val currentTime: StateFlow<LocalDateTime> = _currentTime.asStateFlow()

    init {
        // 1. Launch the long-running live-time update coroutine
        viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalDateTime.now()
                delay(60_000L) // Delay for a minute
            }
        }

        // 2. Launch the one-time preference loading coroutine
        viewModelScope.launch {
            _showCompleted.value = !userPreferencesRepository.hideCompleted.first()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        reminderRepository.getAllReminders(),
        reminderRepository.getAllActions(),
        _selectedDate,
        _showCompleted,
        userPreferencesRepository.defaultReminderTimes
    ) { reminders, actions, date, showCompleted, defaultTimes ->
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
            showCompleted = showCompleted,
            defaultTimes
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
