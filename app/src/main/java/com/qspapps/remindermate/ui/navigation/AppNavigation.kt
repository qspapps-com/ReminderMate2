package com.qspapps.remindermate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qspapps.remindermate.ui.addeditreminder.AddEditReminderScreen
import com.qspapps.remindermate.ui.allreminders.AllRemindersScreen
import com.qspapps.remindermate.ui.home.HomeScreen
import com.qspapps.remindermate.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable(
            route = "add_edit_reminder?reminderId={reminderId}",
            arguments = listOf(navArgument("reminderId") {
                type = NavType.LongType
                defaultValue = 0L
            })
        ) {
            AddEditReminderScreen(navController = navController)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("all_reminders") {
            AllRemindersScreen(navController = navController)
        }
    }
}
