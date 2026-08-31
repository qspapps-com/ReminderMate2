package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.utils.DateTimeUtils
import com.qspapps.remindermate.utils.DateTimeUtils.formatDateTime
import com.qspapps.remindermate.utils.DateTimeUtils.formatTime
import com.qspapps.remindermate.utils.nextAfter
import java.time.LocalDateTime
import java.time.LocalTime

private const val QUICK_SNOOZE_MINUTES = 15L

@Composable
fun ReminderInstanceItem(
    reminderInstance: ReminderInstance,
    actions: ReminderActions,
    showDate: Boolean,
    isOverdue: Boolean,
    defaultTimes: List<LocalTime> = emptyList()
) {
    var showMenu by remember { mutableStateOf(false) }
    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showCustomSnoozeDialog) {
        CustomSnoozeDialogs(
            onDismiss = { showCustomSnoozeDialog = false },
            onConfirm = { newDateTime -> actions.onSnooze(reminderInstance, newDateTime) }
        )
    }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            reminderInstance = reminderInstance,
            onDismiss = { showDeleteConfirmation = false },
            onDeleteInstance = actions.onDeleteInstance,
            onDeleteReminder = actions.onDeleteReminder
        )
    }

    val baseStyle = MaterialTheme.typography.bodyLarge
    val textStyle = if (reminderInstance.isCompleted) {
        baseStyle.copy(textDecoration = TextDecoration.LineThrough)
    } else {
        baseStyle
    }

    ListItem(
        modifier = Modifier.testTag("reminder_item_${reminderInstance.title}"),
        leadingContent = {
            Checkbox(
                checked = reminderInstance.isCompleted,
                onCheckedChange = { actions.onCompletedChange(reminderInstance) }
            )
        },
        headlineContent = { Text(reminderInstance.title, style = textStyle) },
        supportingContent = { supportingText(reminderInstance)?.let { Text(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showDate) {
                        formatDateTime(reminderInstance.displayTime, "\n")
                    } else {
                        formatTime(reminderInstance.displayTime)
                    },
                    color = if (isOverdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (!reminderInstance.isCompleted) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(id = R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            InstanceMenuItems(
                                reminderInstance = reminderInstance,
                                defaultTimes = defaultTimes,
                                actions = actions,
                                onItemClicked = { showMenu = false },
                                onCustomSnooze = { showCustomSnoozeDialog = true },
                                onDelete = { showDeleteConfirmation = true }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun InstanceMenuItems(
    reminderInstance: ReminderInstance,
    defaultTimes: List<LocalTime>,
    actions: ReminderActions,
    onItemClicked: () -> Unit,
    onCustomSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.snooze_15_minutes_menu_item)) },
        onClick = {
            actions.onSnooze(reminderInstance, DateTimeUtils.minsFromNow(QUICK_SNOOZE_MINUTES))
            onItemClicked()
        }
    )

    val nextDefaultTime = defaultTimes.nextAfter(
        reference = LocalDateTime.now(),
        mustBeAfter = reminderInstance.displayTime
    )
    // Offer the next configured reminder time, falling back to "tomorrow" when none is left today.
    if (nextDefaultTime != null) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        id = R.string.snooze_until_menu_item,
                        formatTime(nextDefaultTime)
                    )
                )
            },
            onClick = {
                actions.onSnooze(reminderInstance, nextDefaultTime)
                onItemClicked()
            }
        )
    } else {
        DropdownMenuItem(
            text = { Text(stringResource(id = R.string.snooze_1_day_menu_item)) },
            onClick = {
                actions.onSnooze(reminderInstance, reminderInstance.displayTime.plusDays(1))
                onItemClicked()
            }
        )
    }

    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.custom_snooze_menu_item)) },
        onClick = {
            onItemClicked()
            onCustomSnooze()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.update_menu_item)) },
        onClick = {
            actions.onUpdate(reminderInstance.reminderId)
            onItemClicked()
        }
    )
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.delete_menu_item)) },
        onClick = {
            onItemClicked()
            onDelete()
        }
    )
}

/** Description and, for a snoozed instance, a note about when it was originally due. */
@Composable
private fun supportingText(instance: ReminderInstance): String? {
    val snoozeNote = if (instance.displayTime != instance.originalTime) {
        val original = if (instance.displayTime.toLocalDate() == instance.originalTime.toLocalDate()) {
            formatTime(instance.originalTime)
        } else {
            formatDateTime(instance.originalTime, " ")
        }
        stringResource(id = R.string.snoozed_original_time, original)
    } else {
        null
    }
    return listOfNotNull(instance.description, snoozeNote)
        .joinToString("\n")
        .ifEmpty { null }
}
