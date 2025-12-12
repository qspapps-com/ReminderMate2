package com.qspapps.remindermate.ui.allreminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AllRemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val selectedFrequency: Frequency? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class AllRemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

    private val _selectedFrequency = MutableStateFlow<Frequency?>(null)

    val uiState: StateFlow<AllRemindersUiState> = combine(
        reminderRepository.getAllReminders(),
        _selectedFrequency
    ) { reminders, frequency ->
        val filteredReminders = if (frequency == null) {
            reminders
        } else {
            reminders.filter { it.recurrence?.frequency == frequency }
        }
        AllRemindersUiState(reminders = filteredReminders)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllRemindersUiState(isLoading = true)
    )

    fun setFrequencyFilter(frequency: Frequency?) {
        _selectedFrequency.value = frequency
    }

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
