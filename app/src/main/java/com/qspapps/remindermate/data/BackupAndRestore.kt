package com.qspapps.remindermate.data

import com.qspapps.remindermate.data.legacy.DataConverter
import com.qspapps.remindermate.data.legacy.JsonReminder
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class BackupData(val reminders: List<Reminder>, val actions: List<ReminderAction>)

class BackupAndRestore {

    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

    fun backup(backupData: BackupData): ByteArray {
        val jsonString = json.encodeToString(backupData)
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).bufferedWriter().use { it.write(jsonString) }
        return outputStream.toByteArray()
    }

    fun restore(data: ByteArray): BackupData {
        val jsonString = try {
            val inputStream = ByteArrayInputStream(data)
            GZIPInputStream(inputStream).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            // Not a GZIP file, assume it's a plain JSON string
            String(data)
        }

        return try {
            // Try parsing as the new format first
            json.decodeFromString<BackupData>(jsonString)
        } catch (e: Exception) {
            // If it fails, try parsing as the legacy format
            val legacyReminders = json.decodeFromString<List<JsonReminder>>(jsonString)
            DataConverter.convertToBackupData(legacyReminders)
        }
    }
}
