package com.qspapps.remindermate.data.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

@Serializable
data class RecurrenceRule(
    val frequency: Frequency,
    val interval: Int = 1, // e.g., "2" for every 2 hours
    // Only used if frequency is WEEKLY.
    // For "Weekdays", list is [MONDAY..FRIDAY]. For "Weekends", [SATURDAY, SUNDAY].
    val daysOfWeek: Set<DayOfWeek>? = null,
    val count: Int? = null // The number of times the reminder should recur
) {
    override fun toString(): String {
        val daysOfWeekString = daysOfWeek?.joinToString(",") { it.name } ?: ""
        val countString = count?.toString() ?: ""
        return "${frequency.name};$interval;$daysOfWeekString;$countString"
    }

    companion object {
        fun fromString(ruleString: String): RecurrenceRule? {
            return try {
                val parts = ruleString.split(';')
                if (parts.size < 2) return null

                val frequency = Frequency.valueOf(parts[0])
                val interval = parts[1].toInt()
                val daysOfWeek = if (parts.size > 2 && parts[2].isNotEmpty()) {
                    parts[2].split(',').map { DayOfWeek.valueOf(it) }.toSet()
                } else {
                    null
                }
                val count = if (parts.size > 3 && parts[3].isNotEmpty()) {
                    parts[3].toInt()
                } else {
                    null
                }
                RecurrenceRule(frequency, interval, daysOfWeek, count)
            } catch (e: Exception) {
                // Log the exception or handle it as needed
                null
            }
        }
    }
}
