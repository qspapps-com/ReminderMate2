package com.qspapps.remindermate.ui.core

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.ReminderInstance

@Composable
fun DeleteConfirmationDialog(
    reminderInstance: ReminderInstance,
    onDismiss: () -> Unit,
    onDeleteInstance: (ReminderInstance) -> Unit,
    onDeleteReminder: (Long) -> Unit
) {
    val title = stringResource(R.string.delete_reminder)

    if (reminderInstance.isRecurring) {
        // Dialog for recurring reminders
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(stringResource(R.string.delete_reminder_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder(reminderInstance.reminderId)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.delete_all_occurrences))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteInstance(reminderInstance)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.delete_this_instance))
                }
            }
        )
    } else {
        // Dialog for non-recurring reminders
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(stringResource(R.string.delete_confirmation_simple)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder(reminderInstance.reminderId)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
