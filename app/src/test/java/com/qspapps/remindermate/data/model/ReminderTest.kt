package com.qspapps.remindermate.data.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderTest {

    // --- Tests for getOccurrences (previously calculateOccurrencesForDay) ---

    @Test
    fun getOccurrences_yearlyRecurrence_everyTwoYears() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 10, 0)
        val rule = RecurrenceRule(Frequency.YEARLY, interval = 2)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)

        // Occurrence 1: 2024
        var targetDay = LocalDate.of(2024, 6, 15)
        var result = reminder.getOccurrences(targetDay, targetDay)
        assertEquals(1, result.size)

        // Skip 2025
        targetDay = LocalDate.of(2025, 6, 15)
        result = reminder.getOccurrences(targetDay, targetDay)
        assertEquals(0, result.size)

        // Occurrence 2: 2026
        targetDay = LocalDate.of(2026, 6, 15)
        result = reminder.getOccurrences(targetDay, targetDay)
        assertEquals(1, result.size)

        // Occurrence 3: 2028 -> Should occur
        targetDay = LocalDate.of(2028, 6, 15)
        result = reminder.getOccurrences(targetDay, targetDay)
        assertEquals(1, result.size)
    }

    // --- Tests for HOURLY Recurrence ---

    @Test
    fun getOccurrences_hourlyRecurrence_multipleOccurrencesOnDay() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 8, 30)
        // Every 2 hours
        val rule = RecurrenceRule(Frequency.HOURLY, interval = 2)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        val targetDay = startDateTime.toLocalDate()

        val result = reminder.getOccurrences(targetDay, targetDay)

        // Expected times: 08:30, 10:30, 12:30, 14:30, 16:30, 18:30, 20:30, 22:30
        assertEquals(8, result.size)
        assertEquals(LocalTime.of(8, 30), result[0].toLocalTime())
        assertEquals(LocalTime.of(22, 30), result.last().toLocalTime())
    }

    @Test
    fun getOccurrences_hourlyRecurrence_acrossMidnight() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 23, 0)
        // Every 3 hours
        val rule = RecurrenceRule(Frequency.HOURLY, interval = 3)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)

        // Target Day: June 16th
        val targetDay = LocalDate.of(2024, 6, 16)

        val result = reminder.getOccurrences(targetDay, targetDay)

        // Start (23:00 on 6/15). Next occurrence is 02:00 on 6/16.
        // Expected times on 6/16: 02:00, 05:00, 08:00, 11:00, 14:00, 17:00, 20:00, 23:00
        assertEquals(8, result.size)
        assertEquals(LocalTime.of(2, 0), result.first().toLocalTime())
        assertEquals(LocalTime.of(23, 0), result.last().toLocalTime())
    }

    @Test
    fun getOccurrences_hourlyRecurrence_withCountLimit() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 20, 0)
        // Every 2 hours, 3 times
        val rule = RecurrenceRule(Frequency.HOURLY, interval = 2, count = 3)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)

        // Expected: 20:00 (1), 22:00 (2), 00:00 on 6/16 (3).

        // Target Day 1: June 15th
        var result = reminder.getOccurrences(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 15))
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(20, 0), result[0].toLocalTime())
        assertEquals(LocalTime.of(22, 0), result[1].toLocalTime())

        // Target Day 2: June 16th
        result = reminder.getOccurrences(LocalDate.of(2024, 6, 16), LocalDate.of(2024, 6, 16))
        // The 00:00 occurrence should be the last one (index 3). Nothing should occur after.
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(0, 0), result.first().toLocalTime())
    }

    // --- Tests for MINUTE Recurrence ---

    @Test
    fun getOccurrences_minuteRecurrence_multipleOccurrencesOnDay() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 10, 30)
        // Every 15 minutes
        val rule = RecurrenceRule(Frequency.MINUTE, interval = 15)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        val targetDay = startDateTime.toLocalDate()

        val result = reminder.getOccurrences(targetDay, targetDay)

        // Expected times: 10:30, 10:45, 11:00, 11:15...
        // The test is complex to check all, just check the first and last occurrence for the day
        val expectedFirst = LocalTime.of(10, 30)
        val expectedLast = LocalTime.of(23, 45) // 23:45 + 15 min = 00:00 next day, so 23:45 is last

        assertEquals(54, result.size) // (24 * 60 - (10 * 60 + 30)) / 15 + 1? Should be 55.
        assertEquals(expectedFirst, result.first().toLocalTime())
        assertEquals(expectedLast, result.last().toLocalTime())
    }

    @Test
    fun getOccurrences_minuteRecurrence_acrossMidnightWithCountLimit() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 23, 58)
        // Every 2 minutes, 3 times
        val rule = RecurrenceRule(Frequency.MINUTE, interval = 2, count = 3)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)

        // Expected: 23:58 (1), 00:00 on 6/16 (2), 00:02 on 6/16 (3).

        // Target Day 1: June 15th
        var result = reminder.getOccurrences(LocalDate.of(2024, 6, 15), LocalDate.of(2024, 6, 15))
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(23, 58), result.first().toLocalTime())

        // Target Day 2: June 16th
        result = reminder.getOccurrences(LocalDate.of(2024, 6, 16), LocalDate.of(2024, 6, 16))
        // Occurrences at 00:00 (index 2) and 00:02 (index 3)
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(0, 0), result[0].toLocalTime())
        assertEquals(LocalTime.of(0, 2), result[1].toLocalTime())

        // Target Day 3: June 17th -> Should not occur
        result = reminder.getOccurrences(LocalDate.of(2024, 6, 17), LocalDate.of(2024, 6, 17))
        assertEquals(0, result.size)
    }

    // --- Tests for getNextOccurrence ---

    private fun createAction(
        reminderId: Long,
        type: ActionType,
        originalTime: LocalDateTime,
        rescheduledTime: LocalDateTime? = null
    ): ReminderAction {
        return ReminderAction(
            reminderId = reminderId,
            originalScheduledTime = originalTime,
            type = type,
            rescheduledTime = rescheduledTime
        )
    }

    @Test
    fun getNextOccurrence_noRecurrence_noActions_returnsInstance() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val reminder = Reminder(1, "Test", null, start, null)

        val next = reminder.getNextOccurrence(emptyList())

        assertEquals(
            ReminderInstance(1, "Test", null, start, start, isCompleted = false, isSnoozed = false, isRecurring = false),
            next
        )
    }

    @Test
    fun getNextOccurrence_noRecurrence_afterTime_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val reminder = Reminder(1, "Test", null, start, null)

        val next = reminder.getNextOccurrence(emptyList(), after = start.plusMinutes(1))

        assertEquals(null, next)
    }

    @Test
    fun getNextOccurrence_noRecurrence_completed_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val reminder = Reminder(1, "Test", null, start, null)
        val actions = listOf(createAction(1, ActionType.COMPLETED, start))

        val next = reminder.getNextOccurrence(actions)

        assertEquals(null, next)
    }

    @Test
    fun getNextOccurrence_noRecurrence_snoozed_returnsSnoozedInstance() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val snoozedTime = start.plusHours(1)
        val reminder = Reminder(1, "Test", null, start, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, start, snoozedTime))

        val next = reminder.getNextOccurrence(actions)

        assertEquals(
            ReminderInstance(1, "Test", null, snoozedTime, start, isCompleted = false, isSnoozed = true, isRecurring = false),
            next
        )
    }

    @Test
    fun getNextOccurrence_noRecurrence_snoozed_afterSnoozedTime_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val snoozedTime = start.plusHours(1)
        val reminder = Reminder(1, "Test", null, start, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, start, snoozedTime))

        val next = reminder.getNextOccurrence(actions, after = snoozedTime.plusSeconds(1))

        assertEquals(null, next)
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_noActions_returnsFirst() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)

        val next = reminder.getNextOccurrence(emptyList())

        assertEquals(
            ReminderInstance(1, "Test", null, start, start, isCompleted = false, isSnoozed = false, isRecurring = true),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_afterFirst_returnsSecond() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)

        val next = reminder.getNextOccurrence(emptyList(), after = start.plusSeconds(1))

        val expectedTime = start.plusDays(1)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false, isRecurring = true),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_firstCompleted_returnsSecond() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)
        val actions = listOf(createAction(1, ActionType.COMPLETED, start))

        val next = reminder.getNextOccurrence(actions)

        val expectedTime = start.plusDays(1)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false, isRecurring = true),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_firstSnoozed_returnsSnoozed() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val snoozedTime = start.plusMinutes(30)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)
        val actions = listOf(createAction(1, ActionType.SNOOZED, start, snoozedTime))

        // Search from the beginning
        val next = reminder.getNextOccurrence(actions)

        assertEquals(
            ReminderInstance(1, "Test", null, snoozedTime, start, isCompleted = false, isSnoozed = true, isRecurring = true),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_firstSnoozed_afterSnooze_returnsNextDay() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val snoozedTime = start.plusMinutes(30)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)
        val actions = listOf(createAction(1, ActionType.SNOOZED, start, snoozedTime))

        // Search after the snoozed time
        val next = reminder.getNextOccurrence(actions, after = snoozedTime.plusSeconds(1))

        val expectedTime = start.plusDays(1)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false, isRecurring = true),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_countLimitReached_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY, count = 2)
        val reminder = Reminder(1, "Test", null, start, rule)
        val actions = listOf(
            createAction(1, ActionType.COMPLETED, start),
            createAction(1, ActionType.COMPLETED, start.plusDays(1))
        )

        val next = reminder.getNextOccurrence(actions)

        assertEquals(null, next)
    }
}