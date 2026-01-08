package com.qspapps.remindermate.ui.core

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qspapps.remindermate.R

@Composable
fun TimePickerDialog(
    title: String = stringResource(id = R.string.select_time_title),
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(id = R.string.ok_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(id = R.string.cancel_button))
            }
        }
    )
}
