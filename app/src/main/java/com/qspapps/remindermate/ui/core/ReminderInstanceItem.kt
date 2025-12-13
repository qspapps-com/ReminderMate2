package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.utils.DateTimeUtils
import com.qspapps.remindermate.utils.DateTimeUtils.formatDate
import com.qspapps.remindermate.utils.DateTimeUtils.formatTime
import com.qspapps.remindermate.utils.DateTimeUtils.isDue
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderInstanceItem(
    reminderInstance: ReminderInstance,
    onCompletedChange: (ReminderInstance) -> Unit,
    onSnooze: (ReminderInstance, LocalDateTime) -> Unit,
    onDeleteInstance: (ReminderInstance) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onUpdate: (Long) -> Unit,
    showDate: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf<ReminderInstance?>(null) }

    if (showCustomSnoozeDialog) {
        CustomSnoozeDialogs(
            onDismiss = { showCustomSnoozeDialog = false },
            onConfirm = { newDateTime ->
                onSnooze(reminderInstance, newDateTime)
            }
        )
    }

    showDeleteConfirmation?.let { reminderToDelete ->
        DeleteConfirmationDialog(
            reminderInstance = reminderToDelete,
            onDismiss = { showDeleteConfirmation = null },
            onDeleteInstance = onDeleteInstance,
            onDeleteReminder = onDeleteReminder
        )
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
                onCheckedChange = { onCompletedChange(reminderInstance) }
            )
        },
        headlineContent = { Text(reminderInstance.title, style = textStyle) },
        supportingContent = { getSupportingContent(reminderInstance, showDate)?.let { Text(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(reminderInstance.displayTime),
                    color = if (isDue(reminderInstance.displayTime)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.snooze_15_minutes_menu_item)) },
                            onClick = {
                                onSnooze(
                                    reminderInstance,
                                    DateTimeUtils.minsFromNow(15)
                                )
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.snooze_1_day_menu_item)) },
                            onClick = {
                                onSnooze(
                                    reminderInstance,
                                    reminderInstance.displayTime.plusDays(1)
                                )
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.custom_snooze_menu_item)) },
                            onClick = {
                                showMenu = false
                                showCustomSnoozeDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.update_menu_item)) },
                            onClick = {
                                onUpdate(reminderInstance.reminderId)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.delete_menu_item)) },
                            onClick = {
                                showDeleteConfirmation = reminderInstance
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    )
}

fun getSupportingContent(reminderInstance: ReminderInstance, showDate: Boolean):String? {
    val s1 = reminderInstance.description
    val s2 = if (showDate) formatDate(reminderInstance.displayTime) else null
    return when {
        s1 != null && s2 != null -> "$s1\n$s2"
        s1 != null -> s1 // s2 must be null here
        s2 != null -> s2 // s1 must be null here
        else -> null // Both are null
    }
}