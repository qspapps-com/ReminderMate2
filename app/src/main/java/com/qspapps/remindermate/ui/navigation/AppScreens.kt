package com.qspapps.remindermate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppScreen(
    val route: String,
    val displayName: String? = null,
    val icon: ImageVector? = null
) {
    object Home : AppScreen("home")
    object Settings : AppScreen("settings", "Settings", Icons.Default.Settings)
    object AllReminders : AppScreen("all_reminders", "All Reminders",
        Icons.AutoMirrored.Filled.List
    )
    object OverdueReminders : AppScreen("overdue_reminders", "Overdue Reminders", Icons.Default.Notifications)

    object AddEditReminder : AppScreen("add_edit_reminder?reminderId={reminderId}") {
        fun createRoute(reminderId: Long) = "add_edit_reminder?reminderId=$reminderId"
    }
}