package com.qspapps.remindermate.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qspapps.remindermate.data.BackupAndRestore
import com.qspapps.remindermate.data.BackupData
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val backupAndRestore: BackupAndRestore,
    private val reminderAlarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

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
                val allReminders = reminderRepository.getAllReminders().first()
                allReminders.forEach { reminder ->
                    reminderAlarmScheduler.cancel(reminder)
                }
                clearDatabase()
            }

            val backupData = backupAndRestore.restore(data)
            val idMap = mutableMapOf<Long, Long>()
            val newReminders = mutableListOf<Reminder>()

            backupData.reminders.forEach { reminder ->
                val oldId = reminder.id
                val newId = reminderRepository.insert(reminder.copy(id = 0))
                idMap[oldId] = newId
                newReminders.add(reminder.copy(id = newId))
            }

            backupData.actions.forEach { action ->
                val newReminderId = idMap[action.reminderId]
                if (newReminderId != null) {
                    reminderRepository.insertAction(action.copy(reminderId = newReminderId))
                }
            }

            newReminders.forEach { reminder ->
                reminderAlarmScheduler.schedule(reminder)
            }
        }
    }

    private suspend fun clearDatabase() {
        reminderRepository.deleteAllActions()
        reminderRepository.deleteAllReminders()
    }
}
