package com.qspapps.remindermate

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.Theme
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.ui.navigation.AppNavigation
import com.qspapps.remindermate.ui.settings.SettingsViewModel
import com.qspapps.remindermate.ui.theme.ReminderMateTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject lateinit var notificationService: NotificationService
    private val viewModel: SettingsViewModel by viewModels()

    private var pendingNavigation = mutableStateOf<String?>(null)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, getString(R.string.notifications_disabled_toast), Toast.LENGTH_LONG).show()
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the intent
        pendingNavigation.value = intent.getStringExtra("TARGET_SCREEN")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNavigation.value = intent.getStringExtra("TARGET_SCREEN")

        askNotificationPermission()
        checkAndCleanupOldReminders()
        if (pendingNavigation.value != "overdue") {
            checkAndNotifyOverdueReminders()
        }
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val useDarkTheme = when (uiState.theme) {
                Theme.LIGHT -> false
                Theme.DARK -> true
                Theme.SYSTEM -> isSystemInDarkTheme()
            }
            ReminderMateTheme(darkTheme = useDarkTheme) {
                AppNavigation(startScreen = pendingNavigation.value)
                LaunchedEffect(pendingNavigation.value) {
                    pendingNavigation.value = null
                }
            }
        }
    }

    private fun checkAndCleanupOldReminders() {
        lifecycleScope.launch {
            val lastCleanup = userPreferencesRepository.lastCleanupTime.first()
            val currentTime = Instant.now().epochSecond
            val sevenDaysInSeconds = 7 * 24 * 60 * 60L

            if (currentTime - lastCleanup > sevenDaysInSeconds) {
                val threshold = LocalDateTime.now().minusDays(30)
                reminderRepository.cleanupOldReminders(threshold)
                userPreferencesRepository.setLastCleanupTime(currentTime)
            }
        }
    }

    private fun checkAndNotifyOverdueReminders() {
        lifecycleScope.launch {
            val lastCheck = userPreferencesRepository.lastOverdueCheckTime.first()
            val currentTimeSeconds = Instant.now().epochSecond
            val oneDayInSeconds = 24 * 60 * 60L

            if (currentTimeSeconds - lastCheck > oneDayInSeconds) {
                // Get data from repositories
                val reminders = reminderRepository.getAllReminders().first()
                val actions = reminderRepository.getAllActions().first()

                val overdueList = ReminderInstance.getOverdueReminders(
                    reminders = reminders,
                    actions = actions,
                    currentTime = LocalDateTime.now()
                )

                if (overdueList.isNotEmpty()) {
                    notificationService.showOverdueSummaryNotification(overdueList.size)
                }

                // Update the last check time regardless of whether we found any
                userPreferencesRepository.setLastOverdueCheckTime(currentTimeSeconds)
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level 33 and higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_required_title))
                    .setMessage(getString(R.string.notification_permission_rationale))
                    .setPositiveButton(getString(R.string.permission_accept)) { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton(getString(R.string.permission_decline)) { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, getString(R.string.notifications_disabled_toast), Toast.LENGTH_LONG).show()
                    }
                    .create()
                    .show()
            } else {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
