package com.qspapps.remindermate.ui.addeditreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.RecurrenceRule
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.utils.DateTimeUtils
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class AddEditReminderUiState(
    val title: String = "",
    val description: String = "",
    val startDateTime: LocalDateTime = DateTimeUtils.minsFromNow(60),
    val recurrence: RecurrenceRule? = null,
    val isNewReminder: Boolean = true,
    val isLoading: Boolean = false,
    val showDateTimeError: Boolean = false,
    val defaultTimes: List<LocalTime> = emptyList()
)

@HiltViewModel
class AddEditReminderViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditReminderUiState())
    val uiState = _uiState.asStateFlow()

    private var reminderId: Long? = null

    init {
        reminderId = savedStateHandle.get<Long>("reminderId")
        viewModelScope.launch {
            if (reminderId != null && reminderId != 0L) {
                loadReminder(reminderId!!)
            } else {
                // Logic for New Reminders
                val defaults = userPreferencesRepository.defaultReminderTimes.first()
                val now = LocalDateTime.now()

                // Find first default time that is after 'now' today
                val suggestedDateTime = defaults
                    .map { now.with(it) }
                    .filter { it.isAfter(now) }
                    .minByOrNull { it }
                    ?: now.plusHours(1) // Fallback if no future default time today

                _uiState.update {
                    it.copy(
                        isNewReminder = true,
                        startDateTime = suggestedDateTime,
                        defaultTimes = defaults
                    )
                }
            }
        }
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val reminder = reminderRepository.getReminderById(id)
            if (reminder != null) {
                _uiState.update {
                    it.copy(
                        title = reminder.title,
                        description = reminder.description ?: "",
                        startDateTime = reminder.startDateTime,
                        recurrence = reminder.recurrence,
                        isNewReminder = false,
                        isLoading = false,
                        showDateTimeError = false
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateStartDateTime(startDateTime: LocalDateTime) {
        val showError = startDateTime.isBefore(LocalDateTime.now())
        _uiState.update { it.copy(startDateTime = startDateTime, showDateTimeError = showError) }
    }

    fun updateRecurrence(recurrence: RecurrenceRule?) {
        _uiState.update { it.copy(recurrence = recurrence) }
    }

    fun saveReminder() {
        viewModelScope.launch {
            val uiState = _uiState.value
            if (uiState.title.isBlank() || uiState.showDateTimeError) {
                return@launch
            }
            val reminder = Reminder(
                id = reminderId ?: 0,
                title = uiState.title,
                description = uiState.description.takeIf { it.isNotBlank() },
                startDateTime = uiState.startDateTime,
                recurrence = uiState.recurrence
            )
            if (uiState.isNewReminder) {
                val newId = reminderRepository.insert(reminder)
                alarmScheduler.schedule(reminder.copy(id = newId))
            } else {
                reminderRepository.update(reminder)
                alarmScheduler.schedule(reminder)
            }
        }
    }
}
