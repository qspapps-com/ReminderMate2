package com.qspapps.remindermate.ui.allreminders

import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.ui.core.ReminderViewModel
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class FilterType {
    object All : FilterType()
    object None : FilterType()
    data class FrequencyFilter(val frequency: Frequency) : FilterType()
}

data class AllRemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val selectedFilter: FilterType = FilterType.All,
    val isLoading: Boolean = false
)

@HiltViewModel
class AllRemindersViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    reminderAlarmScheduler: ReminderAlarmScheduler
) : ReminderViewModel(reminderRepository, reminderAlarmScheduler) {

    private val _selectedFilter = MutableStateFlow<FilterType>(FilterType.All)

    val uiState: StateFlow<AllRemindersUiState> = combine(
        reminderRepository.getAllReminders(),
        _selectedFilter
    ) { reminders, filter ->
        val filteredReminders = when (filter) {
            FilterType.All -> reminders
            FilterType.None -> reminders.filter { it.recurrence == null }
            is FilterType.FrequencyFilter -> reminders.filter { it.recurrence?.frequency == filter.frequency }
        }
        AllRemindersUiState(reminders = filteredReminders, selectedFilter = filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllRemindersUiState(isLoading = true)
    )

    fun setFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }
}
