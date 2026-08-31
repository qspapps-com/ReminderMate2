package com.qspapps.remindermate.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.qspapps.remindermate.R

const val ARG_REMINDER_ID = "reminderId"

sealed class AppScreen(val route: String) {
    data object Home : AppScreen("home")
    data object Settings : AppScreen("settings")
    data object About : AppScreen("about")
    data object AllReminders : AppScreen("all_reminders")
    data object OverdueReminders : AppScreen("overdue_reminders")

    data object AddEditReminder : AppScreen("add_edit_reminder?$ARG_REMINDER_ID={$ARG_REMINDER_ID}") {
        fun createRoute(reminderId: Long) = "add_edit_reminder?$ARG_REMINDER_ID=$reminderId"
    }
}

/** A destination reachable from the Home overflow menu. */
data class MenuDestination(
    val screen: AppScreen,
    @StringRes val displayName: Int,
    val icon: ImageVector
)

val homeMenuDestinations = listOf(
    MenuDestination(AppScreen.AllReminders, R.string.all_reminders_title, Icons.AutoMirrored.Filled.List),
    MenuDestination(AppScreen.OverdueReminders, R.string.overdue_reminders_title, Icons.Default.Notifications),
    MenuDestination(AppScreen.Settings, R.string.settings_title, Icons.Default.Settings),
    MenuDestination(AppScreen.About, R.string.about, Icons.Default.Info)
)
