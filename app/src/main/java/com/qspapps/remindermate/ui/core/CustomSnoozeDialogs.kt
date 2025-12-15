package com.qspapps.remindermate.ui.core

import android.widget.Toast
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.qspapps.remindermate.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSnoozeDialogs(
    onDismiss: () -> Unit,
    onConfirm: (LocalDateTime) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(true) }
    var showTimePicker by remember { mutableStateOf(false) }
    val now = LocalDateTime.now()
    val context = LocalContext.current

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Instant.now().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selectedDate = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                return selectedDate >= LocalDate.now()
            }
        }
    )
    val timePickerState = rememberTimePickerState(
        initialHour = now.hour,
        initialMinute = (now.minute + 30) % 60
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
                onDismiss()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text(stringResource(id = R.string.ok_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDatePicker = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = {
                showTimePicker = false
                onDismiss()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDate = datePickerState.selectedDateMillis?.let {
                            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                        if (selectedDate != null) {
                            val newDateTime = selectedDate.atTime(timePickerState.hour, timePickerState.minute)
                            if (newDateTime.isBefore(LocalDateTime.now())) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.past_snooze_error),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onConfirm(newDateTime)
                                showTimePicker = false
                                onDismiss()
                            }
                        } else {
                            // Should not happen as a date must be selected to get here
                            showTimePicker = false
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.ok_button))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.cancel_button))
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}

@Composable
fun TimePickerDialog(
    title: String = stringResource(id = R.string.select_time_title),
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
