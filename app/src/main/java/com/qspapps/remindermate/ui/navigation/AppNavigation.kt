package com.qspapps.remindermate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qspapps.remindermate.ui.about.AboutScreen
import com.qspapps.remindermate.ui.addeditreminder.AddEditReminderScreen
import com.qspapps.remindermate.ui.allreminders.AllRemindersScreen
import com.qspapps.remindermate.ui.home.HomeScreen
import com.qspapps.remindermate.ui.overduereminders.OverdueRemindersScreen
import com.qspapps.remindermate.ui.settings.SettingsScreen

@Composable
fun AppNavigation(startScreen: String? = null) {
    val navController = rememberNavController()
    // Handle deep link/intent navigation
    LaunchedEffect(startScreen) {if (startScreen == "overdue") {
        navController.navigate(AppScreen.OverdueReminders.route) {
            // Ensure we don't have multiple copies of home on the stack
            popUpTo(AppScreen.Home.route) { saveState = true }
            launchSingleTop = true
        }
    }
    }
    NavHost(navController = navController, startDestination = AppScreen.Home.route) {
        composable(AppScreen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(
            route = AppScreen.AddEditReminder.route,
            arguments = listOf(navArgument("reminderId") {
                type = NavType.LongType
                defaultValue = 0L
            })
        ) {
            AddEditReminderScreen(navController = navController)
        }
        composable(AppScreen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(AppScreen.AllReminders.route) {
            AllRemindersScreen(navController = navController)
        }
        composable(AppScreen.OverdueReminders.route) {
            OverdueRemindersScreen(navController = navController)
        }
        composable(AppScreen.About.route) {
            AboutScreen(navController = navController)
        }
    }
}
