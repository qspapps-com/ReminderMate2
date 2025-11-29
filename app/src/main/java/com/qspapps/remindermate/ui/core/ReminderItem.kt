package com.qspapps.remindermate.ui.core

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.ReminderInstance
import com.qspapps.remindermate.ui.home.HomeViewModel

@Composable
fun ReminderItem(reminderInstance: ReminderInstance, viewModel: HomeViewModel, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { navController.navigate("add_edit_reminder?reminderId=${reminderInstance.reminderId}") }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = reminderInstance.title)
            if (reminderInstance.description != null) {
                Text(text = reminderInstance.description)
            }
            Text(text = reminderInstance.displayTime.toString())
            if (!reminderInstance.isCompleted) {
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { viewModel.snoozeReminder(reminderInstance) }) {
                        Text(text = "Snooze")
                    }
                    Button(onClick = { viewModel.completeReminder(reminderInstance) }) {
                        Text(text = "Complete")
                    }
                }
            }
        }
    }
}
