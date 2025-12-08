package com.qspapps.remindermate.data

import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(val reminders: List<Reminder>, val actions: List<ReminderAction>)

class BackupAndRestore {

    private val json = Json { prettyPrint = true }

    fun backup(backupData: BackupData): String {
        return json.encodeToString(backupData)
    }

    fun restore(data: String): BackupData {
        return json.decodeFromString(data)
    }
}
