package com.qspapps.remindermate.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val HIDE_COMPLETED = booleanPreferencesKey("hide_completed")
    }

    val theme = context.dataStore.data
        .map { preferences ->
            Theme.valueOf(preferences[PreferencesKeys.THEME] ?: Theme.SYSTEM.name)
        }

    val hideCompleted = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HIDE_COMPLETED] ?: false
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
}
