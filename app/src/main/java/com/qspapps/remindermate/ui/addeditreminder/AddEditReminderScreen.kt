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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

private sealed class RepeatOption(val displayName: Int) {
    data object None : RepeatOption(R.string.repeat_option_none)
    data object Minute : RepeatOption(R.string.repeat_option_minute)
    data object Hourly : RepeatOption(R.string.repeat_option_hourly)
    data object Daily : RepeatOption(R.string.repeat_option_daily)
    data object Weekdays : RepeatOption(R.string.repeat_option_weekdays)
    data object Weekends : RepeatOption(R.string.repeat_option_weekends)
    data object Weekly : RepeatOption(R.string.repeat_option_weekly)
    data object Monthly : RepeatOption(R.string.repeat_option_monthly)
    data object Yearly : RepeatOption(R.string.repeat_option_yearly)
}

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
                title = { Text(stringResource(if (uiState.isNewReminder) R.string.add_reminder_title else R.string.edit_reminder_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.cancel_button)
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
                    label = { Text(stringResource(id = R.string.title_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    label = { Text(stringResource(id = R.string.description_label)) },
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

                if (uiState.showDateTimeError) {
                    Text(
                        text = stringResource(id = R.string.past_reminder_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
                        label = { Text(stringResource(id = R.string.repeats_label)) },
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFrequencyDropdownExpanded)
                        },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFrequencyDropdownExpanded,
                        onDismissRequest = { isFrequencyDropdownExpanded = false }
                    ) {
                        val options = listOf(RepeatOption.None, RepeatOption.Minute, RepeatOption.Hourly, RepeatOption.Daily, RepeatOption.Weekdays, RepeatOption.Weekends, RepeatOption.Weekly, RepeatOption.Monthly, RepeatOption.Yearly)
                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(id = option.displayName)) },
                                onClick = {
                                    val newRule: RecurrenceRule? = when (option) {
                                        RepeatOption.None -> null
                                        RepeatOption.Minute -> RecurrenceRule(frequency = Frequency.MINUTE)
                                        RepeatOption.Hourly -> RecurrenceRule(frequency = Frequency.HOURLY)
                                        RepeatOption.Daily -> RecurrenceRule(frequency = Frequency.DAILY)
                                        RepeatOption.Weekdays -> RecurrenceRule(
                                            frequency = Frequency.WEEKLY,
                                            daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                                        )
                                        RepeatOption.Weekends -> RecurrenceRule(
                                            frequency = Frequency.WEEKLY,
                                            daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                                        )
                                        RepeatOption.Weekly -> RecurrenceRule(
                                            frequency = Frequency.WEEKLY,
                                            daysOfWeek = setOf(uiState.startDateTime.dayOfWeek)
                                        )
                                        RepeatOption.Monthly -> RecurrenceRule(frequency = Frequency.MONTHLY)
                                        RepeatOption.Yearly -> RecurrenceRule(frequency = Frequency.YEARLY)
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
                        label = { Text(stringResource(id = R.string.every_label)) },
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
                        label = { Text(stringResource(id = R.string.number_of_times_label)) },
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
                    enabled = uiState.title.isNotBlank() && !uiState.showDateTimeError,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.save_button))
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
        datePickerDialog.datePicker.minDate = Calendar.getInstance().timeInMillis
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

@Composable
private fun RecurrenceRule?.toDisplayString(): String {
    if (this == null) return stringResource(id = R.string.repeat_option_none)

    if (this.frequency == Frequency.WEEKLY) {
        val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        if (this.daysOfWeek == weekdays) return stringResource(id = R.string.repeat_option_weekdays)
        if (this.daysOfWeek == weekends) return stringResource(id = R.string.repeat_option_weekends)
    }
    return when (this.frequency) {
        Frequency.MINUTE -> stringResource(id = R.string.repeat_option_minute)
        Frequency.HOURLY -> stringResource(id = R.string.repeat_option_hourly)
        Frequency.DAILY -> stringResource(id = R.string.repeat_option_daily)
        Frequency.WEEKLY -> stringResource(id = R.string.repeat_option_weekly)
        Frequency.MONTHLY -> stringResource(id = R.string.repeat_option_monthly)
        Frequency.YEARLY -> stringResource(id = R.string.repeat_option_yearly)
    }
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
