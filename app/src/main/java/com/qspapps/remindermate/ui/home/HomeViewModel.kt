package com.qspapps.remindermate.ui.home

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.BackupAndRestore
import com.qspapps.remindermate.data.BackupData
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.ReminderAction
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
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
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val reminderAlarmScheduler: ReminderAlarmScheduler,
    private val backupAndRestore: BackupAndRestore
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
                val instances = ReminderInstance.getRemindersForDay(date, reminders, actions)
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

    fun toggleCompleted(reminderInstance: ReminderInstance) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderInstance.reminderId) ?: return@launch
            if (reminderInstance.isCompleted) { // Is completed, so user wants to un-complete
                val action = ReminderAction(
                    reminderId = reminderInstance.reminderId,
                    originalScheduledTime = reminderInstance.originalTime,
                    type = ActionType.COMPLETED
                )
                reminderRepository.deleteAction(action)
                // Schedule this one again
                reminderAlarmScheduler.scheduleInstance(reminderInstance.copy(isCompleted = false))
            } else { // Is not completed, so user wants to complete
                val action = ReminderAction(
                    reminderId = reminderInstance.reminderId,
                    originalScheduledTime = reminderInstance.originalTime,
                    type = ActionType.COMPLETED
                )
                reminderRepository.insertAction(action)
                // Schedule next one if recurring
                reminderAlarmScheduler.schedule(reminder, after = reminderInstance.displayTime)
            }
        }
    }

    fun snoozeReminder(reminderInstance: ReminderInstance, newTime: LocalDateTime) {
        viewModelScope.launch {
            val action = ReminderAction(
                reminderId = reminderInstance.reminderId,
                originalScheduledTime = reminderInstance.originalTime,
                type = ActionType.SNOOZED,
                rescheduledTime = newTime
            )
            reminderRepository.insertAction(action)
            // Schedule the snoozed instance
            val snoozedInstance = reminderInstance.copy(displayTime = newTime, isCompleted = false)
            reminderAlarmScheduler.scheduleInstance(snoozedInstance)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(reminderId)
            if(reminder != null) {
                reminderAlarmScheduler.cancel(reminder)
            }
            reminderRepository.deleteReminderById(reminderId)
        }
    }

    fun backupReminders(uri: Uri) {
        viewModelScope.launch {
            val reminders = reminderRepository.getAllReminders().first()
            val actions = reminderRepository.getAllActions().first()
            val backupData = backupAndRestore.backup(BackupData(reminders, actions))

            withContext(Dispatchers.IO) {
                context.contentResolver.openFileDescriptor(uri, "w")?.use {
                    FileOutputStream(it.fileDescriptor).use {
                        it.write(backupData.toByteArray())
                    }
                }
            }
        }
    }

    fun restoreReminders(uri: Uri, clearExisting: Boolean) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use {
                    BufferedReader(InputStreamReader(it)).use {
                        var line = it.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = it.readLine()
                        }
                    }
                }
                stringBuilder.toString()
            }

            if (clearExisting) {
                clearDatabase()
            }

            val backupData = backupAndRestore.restore(data)
            val idMap = mutableMapOf<Long, Long>()

            backupData.reminders.forEach { reminder ->
                val oldId = reminder.id
                val newId = reminderRepository.insert(reminder.copy(id = 0))
                idMap[oldId] = newId
            }

            backupData.actions.forEach { action ->
                val newReminderId = idMap[action.reminderId]
                if (newReminderId != null) {
                    reminderRepository.insertAction(action.copy(reminderId = newReminderId))
                }
            }
        }
    }

    private suspend fun clearDatabase() {
        reminderRepository.deleteAllActions()
        reminderRepository.deleteAllReminders()
    }
}
