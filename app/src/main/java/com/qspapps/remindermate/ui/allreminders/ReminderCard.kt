package com.qspapps.remindermate.ui.allreminders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qspapps.remindermate.data.model.Reminder

@Composable
fun ReminderCard(reminder: Reminder) {
    Card(modifier = Modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = reminder.title)
            reminder.description?.let { Text(text = it) }
        }
    }
}
