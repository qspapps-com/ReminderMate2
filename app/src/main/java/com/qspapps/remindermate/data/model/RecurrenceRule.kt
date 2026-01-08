package com.qspapps.remindermate.data.model

import android.util.Log
import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDateTime

private const val TAG = "RecurrenceRule"

@Serializable
data class RecurrenceRule(
    val frequency: Frequency,
    val interval: Int = 1, // e.g., "2" for every 2 hours
    // Only used if frequency is WEEKLY.
    // For "Weekdays", list is [MONDAY..FRIDAY]. For "Weekends", [SATURDAY, SUNDAY].
    val daysOfWeek: Set<DayOfWeek>? = null,
    val count: Int? = null // The number of times the reminder should recur
) {
    init {
        // Validation to ensure interval is always positive
        require(interval > 0) { "Interval must be a positive integer. Provided: $interval" }
    }
    override fun toString(): String {
        val daysOfWeekString = daysOfWeek?.joinToString(",") { it.name } ?: ""
        val countString = count?.toString() ?: ""
        return "${frequency.name};$interval;$daysOfWeekString;$countString"
    }

    fun getNextOccurrence(fromDateTime: LocalDateTime): LocalDateTime {
        return when (frequency) {
            Frequency.MINUTE -> fromDateTime.plusMinutes(interval.toLong())

            Frequency.HOURLY -> fromDateTime.plusHours(interval.toLong())

            Frequency.DAILY -> fromDateTime.plusDays(interval.toLong())

            Frequency.WEEKLY -> {
                if (daysOfWeek.isNullOrEmpty()) {
                    fromDateTime.plusWeeks(interval.toLong())
                } else {
                    var candidate = fromDateTime.plusDays(1)
                    // Search forward for the next day of the week that is in the allowed set.
                    // This handles cases like "Every Monday and Friday"
                    while (!daysOfWeek.contains(candidate.dayOfWeek)) {
                        candidate = candidate.plusDays(1)
                    }

                    if (interval > 1) {
                        // Find the start of the week for both dates (Monday)
                        val fromStartOfWeek = fromDateTime.toLocalDate()
                            .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val candidateStartOfWeek = candidate.toLocalDate()
                            .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

                        // If candidate falls in next week, we should skip by interval weeks - 1
                        if (fromStartOfWeek != candidateStartOfWeek) {
                            val weeksToAdd = interval - 1L
                            candidate = candidate.plusWeeks(weeksToAdd)
                        }
                    }
                    candidate
                }
            }

            Frequency.MONTHLY -> fromDateTime.plusMonths(interval.toLong())

            Frequency.YEARLY -> fromDateTime.plusYears(interval.toLong())
        }
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
                Log.e(TAG, "Error parsing recurrence rule: $ruleString", e)
                null
            }
        }
    }
}
