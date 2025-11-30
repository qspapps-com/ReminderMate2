package com.qspapps.remindermate.ui.home

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.qspapps.remindermate.ui.core.ReminderItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val title = if (uiState.selectedDate.isEqual(LocalDate.now())) {
        "Today's Reminders"
    } else {
        uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowCompleted() }) {
                        Icon(
                            imageVector = if (uiState.showCompleted) Icons.Filled.Done else Icons.Outlined.Done,
                            contentDescription = if (uiState.showCompleted) "Hide Completed" else "Show Completed"
                        )
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_edit_reminder") }) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.reminders) { reminderInstance ->
                        ReminderItem(reminderInstance, viewModel, navController)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.loadRemindersForDay(LocalDate.of(year, month + 1, dayOfMonth))
                showDatePicker = false
            },
            uiState.selectedDate.year,
            uiState.selectedDate.monthValue - 1,
            uiState.selectedDate.dayOfMonth
        )
        datePickerDialog.show()
    }
}
