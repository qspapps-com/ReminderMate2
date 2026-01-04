package com.qspapps.remindermate.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.local.BackupAndRestore
import com.qspapps.remindermate.data.local.BackupData
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.Theme
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val theme: Theme = Theme.SYSTEM,
    val hideCompleted: Boolean = false,
    val lastError: Pair<String, Long>? = null,
    val workerRunHistory: Map<String, Long> = emptyMap()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val backupAndRestore: BackupAndRestore,
    private val reminderAlarmScheduler: ReminderAlarmScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.theme,
        userPreferencesRepository.hideCompleted,
        userPreferencesRepository.lastError,
        userPreferencesRepository.workerRunHistory
    ) { theme, hideCompleted, lastError, workerRunHistory ->
        SettingsUiState(theme, hideCompleted, lastError, workerRunHistory)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun updateTheme(theme: Theme) {
        viewModelScope.launch {
            userPreferencesRepository.setTheme(theme)
        }
    }

    fun updateHideCompleted(hide: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHideCompleted(hide)
        }
    }

    fun backupReminders(uri: Uri) {
        viewModelScope.launch {
            val reminders = reminderRepository.getAllReminders().first()
            val actions = reminderRepository.getAllActions().first()
            val backupData = backupAndRestore.backup(BackupData(reminders, actions))

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(backupData)
                }
            }
        }
    }

    fun restoreReminders(uri: Uri, clearExisting: Boolean) {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use {
                    it.readBytes()
                }
            }?: return@launch

            if (clearExisting) {
                performFullCleanup()
            }

            val backupData = backupAndRestore.restore(data)

            val restoredReminders = reminderRepository.restoreBackupData(
                backupData.reminders, backupData.actions)

            restoredReminders.forEach { reminder ->
                reminderAlarmScheduler.schedule(reminder)
            }
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch {
            performFullCleanup()
        }
    }

    private suspend fun performFullCleanup() {
        val allReminders = reminderRepository.getAllReminders().first()
        allReminders.forEach { reminder ->
            reminderAlarmScheduler.cancel(reminder)
        }
        reminderRepository.deleteAllActions()
        reminderRepository.deleteAllReminders()
    }
}
