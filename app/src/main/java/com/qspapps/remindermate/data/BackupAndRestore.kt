package com.qspapps.remindermate.data

import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class BackupData(val reminders: List<Reminder>, val actions: List<ReminderAction>)

class BackupAndRestore {

    private val json = Json { prettyPrint = true }

    fun backup(backupData: BackupData): ByteArray {
        val jsonString = json.encodeToString(backupData)
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).bufferedWriter().use { it.write(jsonString) }
        return outputStream.toByteArray()
    }

    fun restore(data: ByteArray): BackupData {
        val inputStream = ByteArrayInputStream(data)
        val jsonString = GZIPInputStream(inputStream).bufferedReader().use { it.readText() }
        return json.decodeFromString(jsonString)
    }
}
