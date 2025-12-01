package com.qspapps.remindermate.ui.addeditreminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    navController: NavController,
    viewModel: AddEditReminderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFrequencyDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewReminder) "Add Reminder" else "Edit Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        }
    ) {
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
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDatePicker = true }) {
                        Text(text = uiState.startDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                    Button(onClick = { showTimePicker = true }) {
                        Text(text = uiState.startDateTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = isFrequencyDropdownExpanded,
                    onExpandedChange = { isFrequencyDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.recurrence.toDisplayString(),
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
                        val options = listOf("None", "Daily", "Weekdays", "Weekends", "Weekly", "Monthly", "Yearly")
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    val newRule = when (option) {
                                        "None" -> null
                                        "Daily" -> RecurrenceRule(frequency = Frequency.DAILY)
                                        "Weekdays" -> RecurrenceRule(
                                            frequency = Frequency.WEEKLY,
                                            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                                        )
                                        "Weekends" -> RecurrenceRule(
                                            frequency = Frequency.WEEKLY,
                                            daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                                        )
                                        "Weekly" -> RecurrenceRule(
                                            frequency = Frequency.WEEKLY,
                                            daysOfWeek = setOf(uiState.startDateTime.dayOfWeek)
                                        )
                                        "Monthly" -> RecurrenceRule(frequency = Frequency.MONTHLY)
                                        "Yearly" -> RecurrenceRule(frequency = Frequency.YEARLY)
                                        else -> uiState.recurrence
                                    }
                                    viewModel.updateRecurrence(newRule)
                                    isFrequencyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                val recurrence = uiState.recurrence
                if (recurrence != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (recurrence.frequency == Frequency.WEEKLY) {
                        DayOfWeekSelector(
                            selectedDays = recurrence.daysOfWeek ?: emptySet(),
                            onDayClick = { day ->
                                val currentDays = recurrence.daysOfWeek ?: emptySet()
                                val newDays = if (day in currentDays) currentDays - day else currentDays + day
                                val finalDays = newDays.ifEmpty { setOf(uiState.startDateTime.dayOfWeek) }
                                viewModel.updateRecurrence(recurrence.copy(daysOfWeek = finalDays))
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedTextField(
                        value = recurrence.interval.toString(),
                        onValueChange = { intervalString ->
                            val interval = intervalString.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            viewModel.updateRecurrence(recurrence.copy(interval = interval))
                        },
                        label = { Text("Every") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recurrence.count?.toString() ?: "",
                        onValueChange = { countString ->
                            val count = countString.toIntOrNull()
                            viewModel.updateRecurrence(recurrence.copy(count = count))
                        },
                        label = { Text("Number of times") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        viewModel.saveReminder()
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.updateStartDateTime(uiState.startDateTime.withYear(year).withMonth(month + 1).withDayOfMonth(dayOfMonth))
                showDatePicker = false
            },
            uiState.startDateTime.year,
            uiState.startDateTime.monthValue - 1,
            uiState.startDateTime.dayOfMonth
        )
        datePickerDialog.setOnDismissListener { showDatePicker = false }
        datePickerDialog.show()
    }

    if (showTimePicker) {
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hour, minute ->
                viewModel.updateStartDateTime(uiState.startDateTime.withHour(hour).withMinute(minute))
                showTimePicker = false
            },
            uiState.startDateTime.hour,
            uiState.startDateTime.minute, true
        )
        timePickerDialog.setOnDismissListener { showTimePicker = false }
        timePickerDialog.show()
    }
}

private fun RecurrenceRule?.toDisplayString(): String {
    if (this == null) return "None"

    if (this.frequency == Frequency.WEEKLY) {
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        if (this.daysOfWeek == weekdays) return "Weekdays"
        if (this.daysOfWeek == weekends) return "Weekends"
    }
    return this.frequency.name.lowercase().replaceFirstChar { it.uppercase() }
}

@Composable
private fun DayOfWeekSelector(
    selectedDays: Set<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        for (day in DayOfWeek.entries) {
            DayOfWeekButton(
                day = day,
                isSelected = day in selectedDays,
                onClick = { onDayClick(day) }
            )
        }
    }
}

@Composable
private fun DayOfWeekButton(
    day: DayOfWeek,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier
    ) {
        Text(text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()))
    }
}
