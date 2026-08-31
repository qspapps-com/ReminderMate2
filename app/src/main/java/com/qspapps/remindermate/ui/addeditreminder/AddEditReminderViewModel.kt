package com.qspapps.remindermate.ui.addeditreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.model.RecurrenceRule
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import com.qspapps.remindermate.ui.navigation.ARG_REMINDER_ID
import com.qspapps.remindermate.utils.DateTimeUtils
import com.qspapps.remindermate.utils.nextAfter
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

    /** Null when adding: the route carries 0 for "new reminder". */
    private val reminderId: Long? =
        savedStateHandle.get<Long>(ARG_REMINDER_ID)?.takeIf { it != 0L }

    init {
        viewModelScope.launch {
            if (reminderId != null) loadReminder(reminderId) else prefillNewReminder()
        }
    }

    private suspend fun loadReminder(id: Long) {
        _uiState.update { it.copy(isLoading = true) }
        val reminder = reminderRepository.getReminderById(id)
        if (reminder == null) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
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

    private suspend fun prefillNewReminder() {
        val defaults = userPreferencesRepository.defaultReminderTimes.first()
        val now = LocalDateTime.now()
        _uiState.update {
            it.copy(
                isNewReminder = true,
                // The next configured reminder time today, or an hour from now if none is left.
                startDateTime = defaults.nextAfter(now) ?: now.plusHours(1),
                defaultTimes = defaults
            )
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
