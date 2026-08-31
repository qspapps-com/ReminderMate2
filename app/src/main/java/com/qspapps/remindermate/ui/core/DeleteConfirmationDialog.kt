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
    // A recurring reminder offers "this instance" as the alternative to deleting everything;
    // a one-off has nothing to distinguish, so the alternative is simply cancelling.
    val recurring = reminderInstance.isRecurring

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_reminder)) },
        text = {
            Text(
                stringResource(
                    if (recurring) R.string.delete_reminder_confirmation
                    else R.string.delete_confirmation_simple
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onDeleteReminder(reminderInstance.reminderId)
                onDismiss()
            }) {
                Text(
                    stringResource(
                        if (recurring) R.string.delete_all_occurrences else R.string.delete
                    )
                )
            }
        },
        dismissButton = {
            if (recurring) {
                TextButton(onClick = {
                    onDeleteInstance(reminderInstance)
                    onDismiss()
                }) { Text(stringResource(R.string.delete_this_instance)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
