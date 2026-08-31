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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.ui.core.ReminderItem
import com.qspapps.remindermate.ui.core.labelRes
import com.qspapps.remindermate.ui.navigation.AppScreen

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
                title = { Text(stringResource(id = R.string.all_reminders_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            var expanded by remember { mutableStateOf(false) }
            val filterOptions = listOf(FilterType.All, FilterType.None) + Frequency.entries.map(FilterType::FrequencyFilter)

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
                        value = uiState.selectedFilter.displayText(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(id = R.string.filter_by_frequency_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filterOptions.forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.displayText()) },
                                onClick = {
                                    viewModel.setFilter(filter)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items = uiState.reminders, key = { it.id }) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onUpdate = {
                            navController.navigate(AppScreen.AddEditReminder.createRoute(it))
                        },
                        onDelete = viewModel::deleteReminder
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterType.displayText(): String = stringResource(
    id = when (this) {
        FilterType.All -> R.string.filter_option_all
        FilterType.None -> R.string.filter_option_none
        is FilterType.FrequencyFilter -> frequency.labelRes
    }
)
