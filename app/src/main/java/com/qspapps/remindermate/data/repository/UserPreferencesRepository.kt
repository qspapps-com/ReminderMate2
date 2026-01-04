package com.qspapps.remindermate.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
        val LAST_ERROR_MESSAGE = stringPreferencesKey("last_error_message")
        val LAST_ERROR_TIME = longPreferencesKey("last_error_time")
        val WORKER_RUN_HISTORY = stringPreferencesKey("worker_run_history")
        val DEFAULT_REMINDER_TIMES = stringPreferencesKey("default_reminder_times")
    }
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

    val theme = context.dataStore.data
        .map { preferences ->
            Theme.valueOf(preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name)
        }

    val hideCompleted = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] ?: false
        }

    val lastError: Flow<Pair<String, Long>?> = context.dataStore.data
        .map { preferences ->
            val message = preferences[PreferencesKeys.LAST_ERROR_MESSAGE]
            val time = preferences[PreferencesKeys.LAST_ERROR_TIME] ?: 0L
            if (message != null) message to time else null
        }
    val workerRunHistory: Flow<Map<String, Long>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[PreferencesKeys.WORKER_RUN_HISTORY] ?: "{}"
            try {
                Json.decodeFromString<Map<String, Long>>(json)
            } catch (e: Exception) {
                emptyMap()
            }
        }

    val defaultReminderTimes: Flow<List<LocalTime>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[PreferencesKeys.DEFAULT_REMINDER_TIMES]
            if (json == null) {
                emptyList()
            } else {
                try {
                    Json.decodeFromString<List<String>>(json).map {
                        LocalTime.parse(it, timeFormatter)
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    suspend fun setTheme(theme: Theme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun setHideCompleted(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] = hide
        }
    }
    suspend fun saveError(message: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_ERROR_MESSAGE] = message
            preferences[PreferencesKeys.LAST_ERROR_TIME] = System.currentTimeMillis()
        }
    }
    suspend fun updateWorkerRunTime(workerName: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[PreferencesKeys.WORKER_RUN_HISTORY] ?: "{}"
            val currentMap = try {
                Json.decodeFromString<Map<String, Long>>(currentJson).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }

            currentMap[workerName] = System.currentTimeMillis()
            preferences[PreferencesKeys.WORKER_RUN_HISTORY] = Json.encodeToString(currentMap)
        }
    }
    suspend fun updateDefaultReminderTimes(times: List<LocalTime>) {
        context.dataStore.edit { preferences ->
            val serializedTimes = times.map { it.format(timeFormatter) }
            preferences[PreferencesKeys.DEFAULT_REMINDER_TIMES] = Json.encodeToString(serializedTimes)
        }
    }
}
