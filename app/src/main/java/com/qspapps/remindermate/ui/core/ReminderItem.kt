package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule
import com.qspapps.remindermate.data.model.Reminder
import java.time.format.DateTimeFormatter

@Composable
fun ReminderItem(
    reminder: Reminder,
    onUpdate: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(reminder.title) },
        supportingContent = {
            val dateTimeFormatter = DateTimeFormatter.ofPattern(stringResource(id = R.string.reminder_item_date_time_format))
            val fullDescription = buildString {
                if (!reminder.description.isNullOrEmpty()) {
                    append(reminder.description)
                    append("\n")
                }
                append(stringResource(id = R.string.reminder_starts_prefix))
                append(reminder.startDateTime.format(dateTimeFormatter))
                append("\n")
                append(formatRecurrenceRule(reminder.recurrence))
            }
            Text(text = fullDescription)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.update_menu_item)) },
                        onClick = {
                            showMenu = false
                            onUpdate(reminder.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.delete_menu_item)) },
                        onClick = {
                            showMenu = false
                            onDelete(reminder.id)
                        }
                    )
                }
            }
        }
    )
}
