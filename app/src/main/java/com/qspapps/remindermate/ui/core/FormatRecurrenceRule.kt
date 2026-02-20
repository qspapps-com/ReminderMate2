package com.qspapps.remindermate.ui.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule

@Composable
fun formatRecurrenceRule(rule: RecurrenceRule?): String {
    if (rule == null) return stringResource(id = R.string.one_time_reminder)

    return buildString {
        append(stringResource(id = R.string.repeats_prefix))
        append(" ")
        when (rule.frequency) {
            Frequency.HOURLY -> append(pluralStringResource(id = R.plurals.recurrence_hour, count = rule.interval, rule.interval))
            Frequency.DAILY -> append(pluralStringResource(id = R.plurals.recurrence_day, count = rule.interval, rule.interval))
            Frequency.WEEKLY -> {
                append(pluralStringResource(id = R.plurals.recurrence_week, count = rule.interval, rule.interval))
                rule.daysOfWeek?.let { daysOfWeek ->
                    append(" ")
                    append(stringResource(id = R.string.recurrence_on_prefix))
                    append(" ")
                    append(daysOfWeek.sortedBy { it.value }.joinToString { day -> day.name.lowercase().replaceFirstChar { it.uppercase() } })
                }
            }
            Frequency.MONTHLY -> append(pluralStringResource(id = R.plurals.recurrence_month, count = rule.interval, rule.interval))
            Frequency.YEARLY -> append(pluralStringResource(id = R.plurals.recurrence_year, count = rule.interval, rule.interval))
            Frequency.MINUTE -> append(pluralStringResource(id = R.plurals.recurrence_minute, count = rule.interval, rule.interval))
        }
        rule.count?.let {
            append(pluralStringResource(id = R.plurals.recurrence_for_times, count = it, it))
        }
    }
}