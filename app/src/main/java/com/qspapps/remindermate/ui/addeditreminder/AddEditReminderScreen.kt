package com.qspapps.remindermate.ui.addeditreminder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.Frequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    navController: NavController,
    viewModel: AddEditReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFrequencyDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .padding(16.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    label = { Text("Title") }
                )

                ExposedDropdownMenuBox(
                    expanded = isFrequencyDropdownExpanded,
                    onExpandedChange = { isFrequencyDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.recurrence?.frequency?.name ?: "",
                        onValueChange = { },
                        label = { Text("Repeats") },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFrequencyDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFrequencyDropdownExpanded,
                        onDismissRequest = { isFrequencyDropdownExpanded = false }
                    ) {
                        Frequency.values().forEach { frequency ->
                            DropdownMenuItem(
                                text = { Text(frequency.name) },
                                onClick = {
                                    viewModel.updateRecurrence(uiState.recurrence?.copy(frequency = frequency) ?: com.qspapps.remindermate.data.model.RecurrenceRule(frequency = frequency))
                                    isFrequencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (uiState.recurrence != null) {
                    OutlinedTextField(
                        value = uiState.recurrence?.count?.toString() ?: "",
                        onValueChange = { countString ->
                            val count = countString.toIntOrNull()
                            viewModel.updateRecurrence(uiState.recurrence?.copy(count = count))
                        },
                        label = { Text("Number of times") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Button(onClick = {
                    viewModel.saveReminder()
                    navController.popBackStack()
                }) {
                    Text(text = "Save")
                }
            }
        }
    }
}
