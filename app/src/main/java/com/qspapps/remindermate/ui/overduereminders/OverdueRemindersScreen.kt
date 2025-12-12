package com.qspapps.remindermate.ui.overduereminders

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.ui.core.ReminderInstanceItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdueRemindersScreen(
    navController: NavController,
    viewModel: OverdueRemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overdue Reminders") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(uiState.overdueReminders) { reminderInstance ->
                ReminderInstanceItem(
                    reminderInstance = reminderInstance,
                    onCompletedChange = viewModel::toggleCompleted,
                    onSnooze = viewModel::snoozeReminder,
                    onDeleteInstance = viewModel::deleteReminderInstance,
                    onDeleteReminder = viewModel::deleteReminder,
                    onUpdate = {
                        navController.navigate("add_edit_reminder?reminderId=${it}")
                    }
                )
            }
        }
    }
}
