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
            val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")
            val fullDescription = buildString {
                if (!reminder.description.isNullOrEmpty()) {
                    append(reminder.description)
                    append("\n")
                }
                append("Starts: ")
                append(reminder.startDateTime.format(dateTimeFormatter))
                append("\n")
                append(formatRecurrenceRule(reminder.recurrence))
            }
            Text(text = fullDescription)
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Update") },
                        onClick = {
                            showMenu = false
                            onUpdate(reminder.id)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
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

private fun formatRecurrenceRule(rule: RecurrenceRule?): String {
    if (rule == null) return "One-time reminder"

    val sb = StringBuilder("Repeats ")
    when (rule.frequency) {
        Frequency.HOURLY -> sb.append(if (rule.interval == 1) "hourly" else "every ${rule.interval} hours")
        Frequency.DAILY -> sb.append(if (rule.interval == 1) "daily" else "every ${rule.interval} days")
        Frequency.WEEKLY -> {
            sb.append(if (rule.interval == 1) "weekly" else "every ${rule.interval} weeks")
            rule.daysOfWeek?.let {
                sb.append(" on ")
                sb.append(it.joinToString { day -> day.name.lowercase().replaceFirstChar { it.uppercase() } })
            }
        }
        Frequency.MONTHLY -> sb.append(if (rule.interval == 1) "monthly" else "every ${rule.interval} months")
        Frequency.YEARLY -> sb.append(if (rule.interval == 1) "yearly" else "every ${rule.interval} years")
        Frequency.MINUTE -> sb.append(if (rule.interval == 1) "every minute" else "every ${rule.interval} minutes")
    }
    rule.count?.let {
        sb.append(" for $it times")
    }
    return sb.toString()
}
