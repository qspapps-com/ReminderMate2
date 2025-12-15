package com.qspapps.remindermate.data.local

import android.util.Log
import com.qspapps.remindermate.data.legacy.DataConverter
import com.qspapps.remindermate.data.legacy.JsonReminder
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val TAG = "BackupAndRestore"
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
            Log.w(TAG, "Not a GZIP file, assuming plain JSON: ${e.message}")
            String(data)
        }

        return try {
            // Try parsing as the new format first
            json.decodeFromString<BackupData>(jsonString)
        } catch (e: Exception) {
            // If it fails, try parsing as the legacy format
            Log.w(TAG, "Failed to parse as new format, trying legacy: ${e.message}")
            val legacyReminders = json.decodeFromString<List<JsonReminder>>(jsonString)
            DataConverter.convertToBackupData(legacyReminders)
        }
    }
}
