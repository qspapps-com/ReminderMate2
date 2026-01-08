package com.qspapps.remindermate.ui.core

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePicker
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
    val snoozeTime = now.plusMinutes(30)
    val context = LocalContext.current

    var selectedLocalDate by remember { mutableStateOf(snoozeTime.toLocalDate()) }

    val timePickerState = rememberTimePickerState(
        initialHour = snoozeTime.hour,
        initialMinute = snoozeTime.minute
    )

    if (showDatePicker) {
        DatePickerDialog(
            initialDate = selectedLocalDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    return date >= LocalDate.now()
                }
            },
            onDateSelected = { date ->
                selectedLocalDate = date
                showDatePicker = false
                showTimePicker = true
            },
            onDismiss = {
                showDatePicker = false
                onDismiss()
            }
        )
    }

    if (showTimePicker) {
        val pastSnoozeErrorText = stringResource(id = R.string.past_snooze_error)
        TimePickerDialog(
            onDismissRequest = {
                showTimePicker = false
                onDismiss()
            },
            onConfirm = {
                val newDateTime = selectedLocalDate.atTime(timePickerState.hour, timePickerState.minute)
                if (newDateTime.isBefore(LocalDateTime.now())) {
                    Toast.makeText(
                        context,
                        pastSnoozeErrorText,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    onConfirm(newDateTime)
                    showTimePicker = false
                    onDismiss()
                }
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}