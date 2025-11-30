package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.ui.home.HomeViewModel

@Composable
fun ReminderItem(reminderInstance: ReminderInstance, viewModel: HomeViewModel, navController: NavController) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("add_edit_reminder?reminderId=${reminderInstance.reminderId}") }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = reminderInstance.isCompleted,
            onCheckedChange = { viewModel.completeReminder(reminderInstance) },
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = reminderInstance.title)
            if (reminderInstance.description != null) {
                Text(text = reminderInstance.description)
            }
            Text(text = reminderInstance.displayTime.toString())
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Snooze 15 mins") },
                    onClick = {
                        viewModel.snoozeReminder(reminderInstance, reminderInstance.displayTime.plusMinutes(15))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Snooze 1 hour") },
                    onClick = {
                        viewModel.snoozeReminder(reminderInstance, reminderInstance.displayTime.plusHours(1))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Snooze 1 day") },
                    onClick = {
                        viewModel.snoozeReminder(reminderInstance, reminderInstance.displayTime.plusDays(1))
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Update") },
                    onClick = {
                        navController.navigate("add_edit_reminder?reminderId=${reminderInstance.reminderId}")
                        showMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        viewModel.deleteReminder(reminderInstance.reminderId)
                        showMenu = false
                    }
                )
            }
        }
    }
}
