package com.qspapps.remindermate.ui.home

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    var showMenu by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val title = if (uiState.selectedDate.isEqual(LocalDate.now())) {
        "Today's Reminders"
    } else {
        uiState.selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                viewModel.backupReminders(uri)
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.also { uri ->
                showRestoreDialog = uri
            }
        }
    }

    if (showRestoreDialog != null) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = null },
            title = { Text("Restore Reminders") },
            text = { Text("Do you want to delete all existing reminders before restoring?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreDialog?.let { viewModel.restoreReminders(it, true) }
                    showRestoreDialog = null
                }) { Text("Delete and Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog?.let { viewModel.restoreReminders(it, false) }
                    showRestoreDialog = null
                }) { Text("Keep Existing") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowCompleted() }) {
                        Icon(
                            imageVector = if (uiState.showCompleted) Icons.Filled.Check else Icons.Filled.CheckCircle,
                            contentDescription = if (uiState.showCompleted) "Hide Completed" else "Show Completed"
                        )
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                                putExtra(Intent.EXTRA_TITLE, "reminders.json")
                            }
                            backupLauncher.launch(intent)
                        }, text = { Text("Backup") })
                        DropdownMenuItem(onClick = {
                            showMenu = false
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "application/json"
                            }
                            restoreLauncher.launch(intent)
                        }, text = { Text("Restore") })
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
        var swipeAmount = 0f
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .pointerInput(uiState.selectedDate) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeAmount = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            swipeAmount += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 100
                            if (swipeAmount < -threshold) {
                                viewModel.loadRemindersForDay(uiState.selectedDate.plusDays(1))
                            } else if (swipeAmount > threshold) {
                                viewModel.loadRemindersForDay(uiState.selectedDate.minusDays(1))
                            }
                        }
                    )
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.reminders) { reminderInstance ->
                        ReminderItem(
                            reminderInstance = reminderInstance,
                            onCompletedChange = viewModel::toggleCompleted,
                            onSnooze = viewModel::snoozeReminder,
                            onDelete = viewModel::deleteReminder,
                            onUpdate = { reminderId ->
                                navController.navigate("add_edit_reminder?reminderId=$reminderId")
                            }
                        )
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
