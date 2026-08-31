package com.qspapps.remindermate.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.qspapps.remindermate.data.legacy.DataConverter
import com.qspapps.remindermate.data.legacy.JsonReminder
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "BackupAndRestore"

@Serializable
data class BackupData(val reminders: List<Reminder>, val actions: List<ReminderAction>)

@Singleton
class BackupAndRestore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val json = Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true }

    /** Writes a gzipped backup to [uri]. Returns false if the document could not be opened. */
    suspend fun writeTo(uri: Uri, backupData: BackupData): Boolean = withContext(Dispatchers.IO) {
        val bytes = encode(backupData)
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } != null
    }

    /** Reads a backup from [uri], accepting gzipped or plain JSON in current or legacy format. */
    suspend fun readFrom(uri: Uri): BackupData? = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        bytes?.let(::decode)
    }

    fun encode(backupData: BackupData): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).bufferedWriter().use { it.write(json.encodeToString(backupData)) }
        return outputStream.toByteArray()
    }

    fun decode(data: ByteArray): BackupData {
        val jsonString = try {
            GZIPInputStream(ByteArrayInputStream(data)).bufferedReader().use { it.readText() }
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
            DataConverter.convertToBackupData(json.decodeFromString<List<JsonReminder>>(jsonString))
        }
    }
}
