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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.ui.core.ReminderItem

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

            val selectedText = when (val filter = uiState.selectedFilter) {
                is FilterType.All -> stringResource(id = R.string.filter_option_all)
                is FilterType.None -> stringResource(id = R.string.filter_option_none)
                is FilterType.FrequencyFilter -> stringResource(id = filter.frequency.toStringResource())
            }

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
                        label = { Text(stringResource(id = R.string.filter_by_frequency_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        filterOptions.forEach { filter ->
                            val text = when (filter) {
                                is FilterType.All -> stringResource(id = R.string.filter_option_all)
                                is FilterType.None -> stringResource(id = R.string.filter_option_none)
                                is FilterType.FrequencyFilter -> stringResource(id = filter.frequency.toStringResource())
                            }
                            DropdownMenuItem(
                                text = { Text(text) },
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
                items(uiState.reminders) { reminder ->
                    ReminderItem(
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

@Composable
private fun Frequency.toStringResource(): Int {
    return when (this) {
        Frequency.MINUTE -> R.string.repeat_option_minute
        Frequency.HOURLY -> R.string.repeat_option_hourly
        Frequency.DAILY -> R.string.repeat_option_daily
        Frequency.WEEKLY -> R.string.repeat_option_weekly
        Frequency.MONTHLY -> R.string.repeat_option_monthly
        Frequency.YEARLY -> R.string.repeat_option_yearly
    }
}
