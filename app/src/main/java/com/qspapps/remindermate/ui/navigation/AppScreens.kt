package com.qspapps.remindermate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.qspapps.remindermate.R

sealed class AppScreen(
    val route: String,
    val displayName: Int? = null,
    val icon: ImageVector? = null
) {
    data object Home : AppScreen("home")
    data object Settings : AppScreen("settings", R.string.settings_title, Icons.Default.Settings)
    data object About : AppScreen("about", R.string.about, Icons.Default.Info)
    data object AllReminders : AppScreen("all_reminders", R.string.all_reminders_title,
        Icons.AutoMirrored.Filled.List
    )
    data object OverdueReminders : AppScreen("overdue_reminders", R.string.overdue_reminders_title, Icons.Default.Notifications)

    data object AddEditReminder : AppScreen("add_edit_reminder?reminderId={reminderId}") {
        fun createRoute(reminderId: Long) = "add_edit_reminder?reminderId=$reminderId"
    }
}