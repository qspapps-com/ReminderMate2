package com.qspapps.remindermate.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.local.BackupAndRestore
import com.qspapps.remindermate.data.local.BackupData
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.Theme
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val theme: Theme = Theme.SYSTEM,
    val hideCompleted: Boolean = false,
    val lastError: Pair<String, Long>? = null,
    val workerRunHistory: Map<String, Long> = emptyMap(),
    val defaultReminderTimes: List<LocalTime> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val backupAndRestore: BackupAndRestore,
    private val reminderAlarmScheduler: ReminderAlarmScheduler,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.theme,
        userPreferencesRepository.hideCompleted,
        userPreferencesRepository.lastError,
        userPreferencesRepository.workerRunHistory,
        userPreferencesRepository.defaultReminderTimes,
        ::SettingsUiState
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun updateTheme(theme: Theme) {
        viewModelScope.launch { userPreferencesRepository.setTheme(theme) }
    }

    fun updateHideCompleted(hide: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setHideCompleted(hide) }
    }

    fun updateDefaultReminderTimes(times: List<LocalTime>) {
        viewModelScope.launch { userPreferencesRepository.updateDefaultReminderTimes(times) }
    }

    fun backupReminders(uri: Uri) {
        viewModelScope.launch {
            val backupData = BackupData(
                reminders = reminderRepository.getAllReminders().first(),
                actions = reminderRepository.getAllActions().first()
            )
            backupAndRestore.writeTo(uri, backupData)
        }
    }

    fun restoreReminders(uri: Uri, clearExisting: Boolean) {
        viewModelScope.launch {
            val backupData = backupAndRestore.readFrom(uri) ?: return@launch
            if (clearExisting) {
                performFullCleanup()
            }
            reminderRepository
                .restoreBackupData(backupData.reminders, backupData.actions)
                .forEach { reminderAlarmScheduler.schedule(it) }
        }
    }

    fun clearAllReminders() {
        viewModelScope.launch { performFullCleanup() }
    }

    private suspend fun performFullCleanup() {
        reminderRepository.getAllReminders().first().forEach { reminderAlarmScheduler.cancel(it) }
        reminderRepository.deleteAllActions()
        reminderRepository.deleteAllReminders()
    }
}
