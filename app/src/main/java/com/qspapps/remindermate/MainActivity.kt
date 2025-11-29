package com.qspapps.remindermate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.qspapps.remindermate.ui.navigation.AppNavigation
import com.qspapps.remindermate.ui.theme.ReminderMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReminderMateTheme {
                AppNavigation()
            }
        }
    }
}
