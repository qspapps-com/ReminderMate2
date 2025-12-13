package com.qspapps.remindermate.data.legacy

import android.util.Log
import com.qspapps.remindermate.data.local.BackupData
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TAG = "DataConverter"
object DataConverter {

    // 1. Formatter matching your JSON format
    private val formatter = DateTimeFormatter.ofPattern("d MMM yyyy hh:mm a", Locale.ENGLISH)

    fun convertToBackupData(jsonReminders: List<JsonReminder>): BackupData {
        val targetReminders = mutableListOf<Reminder>()
        val targetActions = mutableListOf<ReminderAction>()

        // ID Counter (JSON doesn't have IDs, so we generate them to link Actions)
        var currentId = 1L

        for (jsonItem in jsonReminders) {
            val reminderId = currentId++

            // --- 1. Parse Start Time and Recurrence ---

            // Default start time (used if no recurrence)
            // If the JSON has neither recurrence nor reminders, we can't determine a time.
            // We'll skip invalid data or default to Now.
            var startDateTime: LocalDateTime = LocalDateTime.now()
            var recurrenceRule: RecurrenceRule? = null

            val jsonRecurrences = jsonItem.recurrences

            if (jsonRecurrences.isNotEmpty()) {
                // Parse the first recurrence to get a baseline
                val firstRec = jsonRecurrences.first()
                val parsedStart = parseDate(firstRec.startTime) ?: LocalDateTime.now()
                startDateTime = parsedStart

                if (jsonRecurrences.size == 1) {
                    // Simple 1:1 mapping
                    recurrenceRule = mapSimpleRecurrence(firstRec)
                } else {
                    // Complex Case: Multiple recurrence rules (e.g., "Go to office" Mon, Tue, Wed...)
                    // We try to merge them into one WEEKLY rule with specific days.
                    val (mergedRule, earliestDate) = mergeRecurrences(jsonRecurrences)
                    recurrenceRule = mergedRule
                    startDateTime = earliestDate
                }
            } else if (!jsonItem.reminders.isNullOrEmpty()) {
                // No recurrence rule, but has a scheduled instance (One-time task)
                startDateTime = parseDate(jsonItem.reminders.first().firstWhen) ?: LocalDateTime.now()
            }

            // --- 2. Build the Target Reminder ---

            val reminder = Reminder(
                id = reminderId,
                title = jsonItem.title,
                description = jsonItem.description.ifBlank { null },
                startDateTime = startDateTime,
                recurrence = recurrenceRule
            )
            targetReminders.add(reminder)

            // --- 3. Build Actions (History) ---

            jsonItem.reminders?.forEach { instance ->
                val firstTime = parseDate(instance.firstWhen)
                val finalTime = parseDate(instance.scheduledTime) // "when" in JSON

                if (firstTime != null && finalTime != null) {

                    // A. Check for SNOOZE (If original time != final time)
                    if (instance.status.equals("completed", ignoreCase = true)) {
                        targetActions.add(
                            ReminderAction(
                                reminderId = reminderId,
                                originalScheduledTime = firstTime, // Completed at the rescheduled time
                                type = ActionType.COMPLETED,
                                rescheduledTime = finalTime
                            )
                        )
                    } else if (instance.status.equals("deleted", ignoreCase = true)) {
                        targetActions.add(
                            ReminderAction(
                                reminderId = reminderId,
                                originalScheduledTime = firstTime, // Completed at the rescheduled time
                                type = ActionType.DELETED,
                                rescheduledTime = finalTime
                            )
                        )
                    } else if (!firstTime.isEqual(finalTime)) {
                        targetActions.add(
                            ReminderAction(
                                reminderId = reminderId,
                                originalScheduledTime = firstTime,
                                type = ActionType.SNOOZED,
                                rescheduledTime = finalTime
                            )
                        )
                    } else {
                        Log.w(TAG, "Unable to add reminder action for $instance")
                    }
                } else {
                    Log.w(TAG, "firstTime or finalTime is null: $firstTime, $finalTime")
                }
            }
        }

        return BackupData(targetReminders, targetActions)
    }

    // --- Helper: Map basic recurrence ---
    private fun mapSimpleRecurrence(jsonRec: JsonRecurrence): RecurrenceRule? {
        val freq = mapFrequency(jsonRec.intervalUnit)
        if (freq == null || jsonRec.repeatInterval == 0) {
            return null
        }
        return RecurrenceRule(
            frequency = freq,
            interval = jsonRec.repeatInterval,
            daysOfWeek = null, // Simple recurrence usually implies strictly calculated intervals
            count = null
        )
    }

    // --- Helper: Merge multiple "Weekly" entries into one Rule with DaysOfWeek ---
    private fun mergeRecurrences(list: List<JsonRecurrence>): Pair<RecurrenceRule?, LocalDateTime> {
        // 1. Check if all are weekly
        val allWeekly = list.all { it.intervalUnit.equals("week", ignoreCase = true) }

        // Find earliest start date to be the "Anchor" for the reminder
        val earliestDate = list.mapNotNull { parseDate(it.startTime) }.minOrNull() ?: LocalDateTime.now()

        return if (allWeekly) {
            // Collect days: Mon, Tue, etc.
            val daysSet = list.mapNotNull {
                parseDate(it.startTime)?.dayOfWeek
            }.toSet()

            val rule = RecurrenceRule(
                frequency = Frequency.WEEKLY,
                interval = list.first().repeatInterval, // Assuming all have same interval (e.g., 1 week)
                daysOfWeek = daysSet
            )
            Pair(rule, earliestDate)
        } else {
            // Fallback: If we have mixed frequencies (Daily AND Monthly),
            // the new model doesn't support it in one object.
            // We return the rule of the first one as a best-effort.
            Pair(mapSimpleRecurrence(list.first()), earliestDate)
        }
    }

    private fun mapFrequency(unit: String): Frequency? {
        return when (unit.lowercase()) {
            "minute" -> Frequency.MINUTE
            "hour" -> Frequency.HOURLY
            "day" -> Frequency.DAILY
            "week" -> Frequency.WEEKLY
            "month" -> Frequency.MONTHLY
            "year" -> Frequency.YEARLY
            else -> null
        }
    }

    private fun parseDate(dateStr: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateStr, formatter)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to parse date $dateStr. ${e.message}")
            null
        }
    }
}

// --- Source Class Aliases (Representing your JSON structure) ---
// I'm assuming these are the classes you generated in step 1,
// referenced here for type safety.

@Serializable
data class JsonReminder(
    val title: String,
    val description: String,
    val recurrences: List<JsonRecurrence> = emptyList(),
    val reminders: List<JsonReminderInstance>? = null
)

@Serializable
data class JsonRecurrence(
    val startTime: String,
    val repeatInterval: Int,
    val intervalUnit: String, // e.g., "minute", "hour", "day"
    val endTime: String? = null
)

@Serializable
data class JsonReminderInstance(
    val firstWhen: String,

    // 'when' is a reserved keyword in Kotlin, so we map the JSON key "when"
    // to a variable named "scheduledTime"
    @SerialName("when")
    val scheduledTime: String,

    val status: String // e.g., "todo", "completed"
)
