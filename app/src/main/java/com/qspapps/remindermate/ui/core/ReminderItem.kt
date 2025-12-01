package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.ui.home.HomeViewModel
import com.qspapps.remindermate.utils.DateTimeUtils.formatTime
import com.qspapps.remindermate.utils.DateTimeUtils.isDue
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderItem(reminderInstance: ReminderInstance, viewModel: HomeViewModel, navController: NavController) {
    val now = LocalDateTime.now()
    var showMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(Instant.now().toEpochMilli())
    val timePickerState = rememberTimePickerState(now.hour, now.minute + 30)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        val selectedDate =
                            datePickerState.selectedDateMillis?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            }
                        if (selectedDate != null) {
                            val newDateTime = selectedDate.atTime(timePickerState.hour, timePickerState.minute)
                            viewModel.snoozeReminder(
                                reminderInstance,
                                newDateTime
                            )
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }


    val textStyle = if (reminderInstance.isCompleted) {
        MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    ListItem(
        leadingContent = {
            Checkbox(
                checked = reminderInstance.isCompleted,
                onCheckedChange = { viewModel.completeReminder(reminderInstance) }
            )
        },
        headlineContent = { Text(reminderInstance.title, style = textStyle) },
        supportingContent = { reminderInstance.description?.let { Text(reminderInstance.description) }  },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(reminderInstance.displayTime),
                    color = if (isDue(reminderInstance.displayTime)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                ) // Consider formatting this better

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Snooze 15 mins") },
                            onClick = {
                                viewModel.snoozeReminder(
                                    reminderInstance,
                                    reminderInstance.displayTime.plusMinutes(15)
                                )
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Snooze 1 day") },
                            onClick = {
                                viewModel.snoozeReminder(
                                    reminderInstance,
                                    reminderInstance.displayTime.plusDays(1)
                                )
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Custom Snooze") },
                            onClick = {
                                showMenu = false
                                showDatePicker = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Update") },
                            onClick = {
                                navController.navigate("add_edit_reminder?reminderId=${reminderInstance.reminderId}")
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                viewModel.deleteReminder(reminderInstance.reminderId)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
