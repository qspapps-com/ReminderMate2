package com.qspapps.remindermate.ui.allreminders

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.ui.core.ReminderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllRemindersScreen(
    navController: NavController,
    viewModel: AllRemindersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Reminders") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            var expanded by remember { mutableStateOf(false) }
            val items = listOf(null) + Frequency.values()
            val selectedText = uiState.selectedFrequency?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedText,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        items.forEach { frequency ->
                            val text = frequency?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "All"
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    viewModel.setFrequencyFilter(frequency)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(uiState.reminders) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onUpdate = {
                            navController.navigate("add_edit_reminder?reminderId=${it}")
                        },
                        onDelete = viewModel::deleteReminder
                    )
                }
            }
        }
    }
}
