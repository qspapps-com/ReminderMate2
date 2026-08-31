package com.qspapps.remindermate.data.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

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
        require(interval > 0) { "Interval must be a positive integer. Provided: $interval" }
        require(count == null || count > 0) { "Count must be a positive integer. Provided: $count" }
    }

    fun getNextOccurrence(fromDateTime: LocalDateTime): LocalDateTime = when (frequency) {
        Frequency.MINUTE -> fromDateTime.plusMinutes(interval.toLong())
        Frequency.HOURLY -> fromDateTime.plusHours(interval.toLong())
        Frequency.DAILY -> fromDateTime.plusDays(interval.toLong())
        Frequency.WEEKLY -> nextWeeklyOccurrence(fromDateTime)
        Frequency.MONTHLY -> fromDateTime.plusMonths(interval.toLong())
        Frequency.YEARLY -> fromDateTime.plusYears(interval.toLong())
    }

    private fun nextWeeklyOccurrence(fromDateTime: LocalDateTime): LocalDateTime {
        if (daysOfWeek.isNullOrEmpty()) return fromDateTime.plusWeeks(interval.toLong())

        // Search forward for the next allowed day, e.g. "every Monday and Friday".
        var candidate = fromDateTime.plusDays(1)
        while (candidate.dayOfWeek !in daysOfWeek) {
            candidate = candidate.plusDays(1)
        }
        if (interval == 1) return candidate

        // For intervals > 1, skip whole weeks once the candidate crosses into a new week.
        val startOfWeek = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        val crossedIntoNextWeek =
            fromDateTime.toLocalDate().with(startOfWeek) != candidate.toLocalDate().with(startOfWeek)
        return if (crossedIntoNextWeek) candidate.plusWeeks(interval - 1L) else candidate
    }

    companion object {
        val WEEKDAYS: Set<DayOfWeek> = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        val WEEKENDS: Set<DayOfWeek> = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        /**
         * Decodes the `FREQUENCY;interval;days;count` format used by database schema v3 and
         * earlier. Only [com.qspapps.remindermate.data.local.MIGRATION_3_4] should need this;
         * new rows are stored as JSON.
         *
         * @return null if [ruleString] is not a well-formed legacy rule.
         */
        fun decodeLegacy(ruleString: String): RecurrenceRule? = runCatching {
            val parts = ruleString.split(';')
            if (parts.size < 2) return null
            RecurrenceRule(
                frequency = Frequency.valueOf(parts[0]),
                interval = parts[1].toInt(),
                daysOfWeek = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }
                    ?.split(',')?.map(DayOfWeek::valueOf)?.toSet(),
                count = parts.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toInt()
            )
        }.getOrNull()
    }
}
