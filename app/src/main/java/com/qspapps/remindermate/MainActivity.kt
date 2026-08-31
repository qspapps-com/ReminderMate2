package com.qspapps.remindermate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qspapps.remindermate.data.repository.Theme
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.ui.core.NotificationPermissionRequest
import com.qspapps.remindermate.ui.navigation.AppNavigation
import com.qspapps.remindermate.ui.settings.SettingsViewModel
import com.qspapps.remindermate.ui.theme.ReminderMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    /** Screen requested by the notification that launched us, consumed once by [AppNavigation]. */
    private var pendingNavigation by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNavigation = intent.targetScreen()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme = when (uiState.theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }
            ReminderMateTheme(darkTheme = useDarkTheme) {
                NotificationPermissionRequest()
                AppNavigation(
                    startScreen = pendingNavigation,
                    onStartScreenHandled = { pendingNavigation = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavigation = intent.targetScreen()
    }

    private fun Intent.targetScreen() =
        getStringExtra(NotificationService.EXTRA_TARGET_SCREEN)
}
