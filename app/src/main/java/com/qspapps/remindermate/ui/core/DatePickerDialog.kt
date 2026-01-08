package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
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
fun DatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    selectableDates: SelectableDates = object : SelectableDates {} // Default allows all dates
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
        selectableDates = selectableDates
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
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
                // Only show "Today" shortcut if today is selectable
                val today = LocalDate.now()
                val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                if (selectableDates.isSelectableDate(todayMillis)) {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis = todayMillis
                        datePickerState.displayedMonthMillis = todayMillis
                    }) {
                        Text(stringResource(id = R.string.today))
                    }
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
