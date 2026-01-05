package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qspapps.remindermate.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    // DatePickerState uses UTC milliseconds
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Convert UTC millis back to LocalDate using System Default Zone
                    val selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    onDateSelected(selectedDate)
                }
            }) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val todayMillis = LocalDate.now()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                    // This jumps the UI to today's date and selects it
                    datePickerState.selectedDateMillis = todayMillis
                    datePickerState.displayedMonthMillis = todayMillis
                }) {
                    // You might need to add this string to your strings.xml
                    // or use a hardcoded string/android resource
                    Text(stringResource(id = R.string.today))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
