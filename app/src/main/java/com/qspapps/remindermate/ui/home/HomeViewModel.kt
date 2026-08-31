package com.qspapps.remindermate.ui.home

import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.domain.remindersForDay
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import com.qspapps.remindermate.ui.core.ReminderViewModel
import com.qspapps.remindermate.utils.minuteTicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val defaultTimes: List<LocalTime> = emptyList(),
    val currentTime: LocalDateTime = LocalDateTime.now()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    reminderAlarmScheduler: ReminderAlarmScheduler,
    notificationService: NotificationService
) : ReminderViewModel(reminderRepository, reminderAlarmScheduler, notificationService) {

    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        reminderRepository.getAllReminders(),
        reminderRepository.getAllActions(),
        selectedDate,
        userPreferencesRepository.hideCompleted,
        userPreferencesRepository.defaultReminderTimes
    ) { reminders, actions, date, hideCompleted, defaultTimes ->
        val instances = remindersForDay(date, reminders, actions)
        HomeUiState(
            selectedDate = date,
            reminders = if (hideCompleted) instances.filterNot { it.isCompleted } else instances,
            isLoading = false,
            showCompleted = !hideCompleted,
            defaultTimes = defaultTimes
        )
    }.combine(minuteTicker()) { state, now ->
        state.copy(currentTime = now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun loadRemindersForDay(date: LocalDate) {
        selectedDate.value = date
    }

    /** Persists the choice, so Home and Settings cannot disagree about it. */
    fun toggleShowCompleted() {
        viewModelScope.launch {
            userPreferencesRepository.setHideCompleted(uiState.value.showCompleted)
        }
    }
}
