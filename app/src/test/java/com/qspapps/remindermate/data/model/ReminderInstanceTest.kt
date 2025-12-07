package com.qspapps.remindermate.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderInstanceTest {

    private val testTime = LocalTime.of(10, 0)

    // Helper function to create a Reminder object
    private fun createReminder(
        startDay: LocalDate,
        recurrence: RecurrenceRule? = null,
        id: Long = 1
    ): Reminder {
        return Reminder(
            id = id,
            title = "Test Reminder",
            startDateTime = LocalDateTime.of(startDay, testTime),
            recurrence = recurrence
        )
    }

    // --- Tests for No Recurrence (One-time Reminder) ---

    @Test
    fun calculateOccurrencesForDay_noRecurrence_onTargetDay() {
        val startDay = LocalDate.of(2024, 6, 15)
        val reminder = createReminder(startDay, recurrence = null)
        val targetDay = startDay

        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        assertEquals(LocalDateTime.of(startDay, testTime), result.first())
    }

    @Test
    fun calculateOccurrencesForDay_noRecurrence_beforeTargetDay() {
        val startDay = LocalDate.of(2024, 6, 15)
        val reminder = createReminder(startDay, recurrence = null)
        val targetDay = startDay.plusDays(1)

        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(0, result.size)
    }

    // --- Tests for DAILY Recurrence ---

    @Test
    fun calculateOccurrencesForDay_dailyRecurrence_noInterval() {
        val startDay = LocalDate.of(2024, 6, 15)
        val reminder = createReminder(startDay, RecurrenceRule(Frequency.DAILY))
        val targetDay = LocalDate.of(2024, 6, 18) // 3 days later

        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        assertEquals(LocalDateTime.of(targetDay, testTime), result.first())
    }

    @Test
    fun calculateOccurrencesForDay_dailyRecurrence_withInterval() {
        val startDay = LocalDate.of(2024, 6, 15) // Saturday
        val rule = RecurrenceRule(Frequency.DAILY, interval = 3) // Every 3 days
        val reminder = createReminder(startDay, rule)

        // Target: Sunday (6/16) -> Should not occur (1 day diff, not divisible by 3)
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 16))
        assertEquals(0, result.size)

        // Target: Tuesday (6/18) -> Should occur (3 days diff)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 18))
        assertEquals(1, result.size)
        assertEquals(LocalDateTime.of(2024, 6, 18, 10, 0), result.first())
    }
    
    @Test
    fun calculateOccurrencesForDay_dailyRecurrence_withCountLimit() {
        val startDay = LocalDate.of(2024, 6, 15)
        val rule = RecurrenceRule(Frequency.DAILY, interval = 1, count = 3) // Daily, 3 times
        val reminder = createReminder(startDay, rule)

        // Occurrence 1: June 15th (Index 1)
        var targetDay = LocalDate.of(2024, 6, 15)
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)

        // Occurrence 3: June 17th (Index 3)
        targetDay = LocalDate.of(2024, 6, 17)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        
        // Occurrence 4: June 18th (Index 4) -> Should not occur
        targetDay = LocalDate.of(2024, 6, 18)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(0, result.size)
    }

    // --- Tests for WEEKLY Recurrence ---

    @Test
    fun calculateOccurrencesForDay_weeklyRecurrence_defaultDay() {
        val startDay = LocalDate.of(2024, 6, 17) // Monday
        val reminder = createReminder(startDay, RecurrenceRule(Frequency.WEEKLY))

        // Target: Same day of week a week later
        var targetDay = LocalDate.of(2024, 6, 24) // Monday
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)

        // Target: Different day of week
        targetDay = LocalDate.of(2024, 6, 25) // Tuesday
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(0, result.size)
    }
    
    @Test
    fun calculateOccurrencesForDay_weeklyRecurrence_multipleDaysAndInterval() {
        val startDay = LocalDate.of(2024, 6, 14) // Friday
        // Recur every 2 weeks on Monday and Friday
        val rule = RecurrenceRule(Frequency.WEEKLY, interval = 2, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val reminder = createReminder(startDay, rule)

        // Week 1 (Recurrence cycle 1)
        // Friday 6/14 (Start day, index 1)
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 14))
        assertEquals(1, result.size)
        // Monday 6/17 (Index 2)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 17))
        assertEquals(1, result.size)
        
        // Week 2 (Skip week)
        // Friday 6/21 (Skip)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 21))
        assertEquals(0, result.size)

        // Week 2 (Recurrence cycle 2)
        // Monday 6/24
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 24))
        assertEquals(0, result.size)
        // Friday 6/28
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 28))
        assertEquals(1, result.size)
    }

    @Test
    fun calculateOccurrencesForDay_weeklyRecurrence_withCountLimit() {
        val startDay = LocalDate.of(2024, 6, 14) // Friday (Start of week 1)
        // Recur weekly on Mon, Fri, but only 3 times total
        val rule = RecurrenceRule(Frequency.WEEKLY, interval = 1, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), count = 3)
        val reminder = createReminder(startDay, rule)
        
        // Week 1 (Partial Start Week)
        // Friday 6/14 (Occurrence 1)
        var targetDay = LocalDate.of(2024, 6, 14)
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        
        // Monday 6/17 (Occurrence 2)
        targetDay = LocalDate.of(2024, 6, 17)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)

        // Friday 6/21 (Occurrence 3) - LAST ONE
        targetDay = LocalDate.of(2024, 6, 21)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        
        // Week 2
        // Monday 6/24 (Occurrence 4) -> Should not occur
        targetDay = LocalDate.of(2024, 6, 24)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(0, result.size)
    }
    
    // --- Tests for MONTHLY Recurrence ---

    @Test
    fun calculateOccurrencesForDay_monthlyRecurrence_sameDayOfMonth() {
        val startDay = LocalDate.of(2024, 6, 15) // Day 15
        val reminder = createReminder(startDay, RecurrenceRule(Frequency.MONTHLY))

        // Target: Day 15 of next month
        val targetDay = LocalDate.of(2024, 7, 15)
        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
    }

    @Test
    fun calculateOccurrencesForDay_monthlyRecurrence_withIntervalAndCountLimit() {
        val startDay = LocalDate.of(2024, 6, 15)
        // Every 3 months, 2 times
        val rule = RecurrenceRule(Frequency.MONTHLY, interval = 3, count = 2) 
        val reminder = createReminder(startDay, rule)

        // Occurrence 1: June 15th
        var targetDay = LocalDate.of(2024, 6, 15)
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)

        // Skip two months: July, August

        // Target month: September (Index 2)
        targetDay = LocalDate.of(2024, 9, 15)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        
        // Target month: December (Index 3) -> Should not occur
        targetDay = LocalDate.of(2024, 12, 15)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(0, result.size)
    }

    // --- Tests for YEARLY Recurrence ---

    @Test
    fun calculateOccurrencesForDay_yearlyRecurrence() {
        val startDay = LocalDate.of(2024, 6, 15)
        val reminder = createReminder(startDay, RecurrenceRule(Frequency.YEARLY))

        // Target: Same date/month next year
        val targetDay = LocalDate.of(2025, 6, 15)
        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
    }

    @Test
    fun calculateOccurrencesForDay_yearlyRecurrence_withIntervalAndCountLimit() {
        val startDay = LocalDate.of(2024, 6, 15)
        // Every 2 years, 2 times
        val rule = RecurrenceRule(Frequency.YEARLY, interval = 2, count = 2)
        val reminder = createReminder(startDay, rule)

        // Occurrence 1: 2024
        var targetDay = LocalDate.of(2024, 6, 15)
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)

        // Skip 2025

        // Occurrence 2: 2026
        targetDay = LocalDate.of(2026, 6, 15)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(1, result.size)
        
        // Occurrence 3: 2028 -> Should not occur
        targetDay = LocalDate.of(2028, 6, 15)
        result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)
        assertEquals(0, result.size)
    }

    // --- Tests for HOURLY Recurrence ---

    @Test
    fun calculateOccurrencesForDay_hourlyRecurrence_multipleOccurrencesOnDay() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 8, 30)
        // Every 2 hours
        val rule = RecurrenceRule(Frequency.HOURLY, interval = 2)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        val targetDay = startDateTime.toLocalDate()

        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)

        // Expected times: 08:30, 10:30, 12:30, 14:30, 16:30, 18:30, 20:30, 22:30
        assertEquals(8, result.size)
        assertEquals(LocalTime.of(8, 30), result[0].toLocalTime())
        assertEquals(LocalTime.of(22, 30), result.last().toLocalTime())
    }

    @Test
    fun calculateOccurrencesForDay_hourlyRecurrence_acrossMidnight() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 23, 0)
        // Every 3 hours
        val rule = RecurrenceRule(Frequency.HOURLY, interval = 3)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        
        // Target Day: June 16th
        val targetDay = LocalDate.of(2024, 6, 16)

        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)

        // Start (23:00 on 6/15). Next occurrence is 02:00 on 6/16.
        // Expected times on 6/16: 02:00, 05:00, 08:00, 11:00, 14:00, 17:00, 20:00, 23:00
        assertEquals(8, result.size)
        assertEquals(LocalTime.of(2, 0), result.first().toLocalTime())
        assertEquals(LocalTime.of(23, 0), result.last().toLocalTime())
    }
    
    @Test
    fun calculateOccurrencesForDay_hourlyRecurrence_withCountLimit() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 20, 0)
        // Every 2 hours, 3 times
        val rule = RecurrenceRule(Frequency.HOURLY, interval = 2, count = 3)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        
        // Expected: 20:00 (1), 22:00 (2), 00:00 on 6/16 (3).

        // Target Day 1: June 15th
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 15))
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(20, 0), result[0].toLocalTime())
        assertEquals(LocalTime.of(22, 0), result[1].toLocalTime())

        // Target Day 2: June 16th
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 16))
        // The 00:00 occurrence should be the last one (index 3). Nothing should occur after.
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(0, 0), result.first().toLocalTime())
    }

    // --- Tests for MINUTE Recurrence ---

    @Test
    fun calculateOccurrencesForDay_minuteRecurrence_multipleOccurrencesOnDay() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 10, 30)
        // Every 15 minutes
        val rule = RecurrenceRule(Frequency.MINUTE, interval = 15)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        val targetDay = startDateTime.toLocalDate()

        val result = ReminderInstance.calculateOccurrencesForDay(reminder, targetDay)

        // Expected times: 10:30, 10:45, 11:00, 11:15...
        // The test is complex to check all, just check the first and last occurrence for the day
        val expectedFirst = LocalTime.of(10, 30)
        val expectedLast = LocalTime.of(23, 45) // 23:45 + 15 min = 00:00 next day, so 23:45 is last
        
        assertEquals(54, result.size) // (24 * 60 - (10 * 60 + 30)) / 15
        assertEquals(expectedFirst, result.first().toLocalTime())
        assertEquals(expectedLast, result.last().toLocalTime())
    }
    
    @Test
    fun calculateOccurrencesForDay_minuteRecurrence_acrossMidnightWithCountLimit() {
        val startDateTime = LocalDateTime.of(2024, 6, 15, 23, 58)
        // Every 2 minutes, 3 times
        val rule = RecurrenceRule(Frequency.MINUTE, interval = 2, count = 3)
        val reminder = Reminder(1, "Test", null, startDateTime, rule)
        
        // Expected: 23:58 (1), 00:00 on 6/16 (2), 00:02 on 6/16 (3).

        // Target Day 1: June 15th
        var result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 15))
        assertEquals(1, result.size)
        assertEquals(LocalTime.of(23, 58), result.first().toLocalTime())

        // Target Day 2: June 16th
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 16))
        // Occurrences at 00:00 (index 2) and 00:02 (index 3)
        assertEquals(2, result.size)
        assertEquals(LocalTime.of(0, 0), result[0].toLocalTime())
        assertEquals(LocalTime.of(0, 2), result[1].toLocalTime())

        // Target Day 3: June 17th -> Should not occur
        result = ReminderInstance.calculateOccurrencesForDay(reminder, LocalDate.of(2024, 6, 17))
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
            type = type,
            originalScheduledTime = originalTime,
            resheduledTime = rescheduledTime,
        )
    }

    @Test
    fun getNextOccurrence_noRecurrence_noActions_returnsInstance() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val reminder = Reminder(1, "Test", null, start, null)

        val next = ReminderInstance.getNextOccurrence(reminder, emptyList())

        assertEquals(
            ReminderInstance(1, "Test", null, start, start, isCompleted = false, isSnoozed = false),
            next
        )
    }

    @Test
    fun getNextOccurrence_noRecurrence_afterTime_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val reminder = Reminder(1, "Test", null, start, null)

        val next = ReminderInstance.getNextOccurrence(reminder, emptyList(), after = start.plusMinutes(1))

        assertEquals(null, next)
    }

    @Test
    fun getNextOccurrence_noRecurrence_completed_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val reminder = Reminder(1, "Test", null, start, null)
        val actions = listOf(createAction(1, ActionType.COMPLETED, start))

        val next = ReminderInstance.getNextOccurrence(reminder, actions)

        assertEquals(null, next)
    }

    @Test
    fun getNextOccurrence_noRecurrence_snoozed_returnsSnoozedInstance() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val snoozedTime = start.plusHours(1)
        val reminder = Reminder(1, "Test", null, start, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, start, snoozedTime))

        val next = ReminderInstance.getNextOccurrence(reminder, actions)

        assertEquals(
            ReminderInstance(1, "Test", null, snoozedTime, start, isCompleted = false, isSnoozed = true),
            next
        )
    }

    @Test
    fun getNextOccurrence_noRecurrence_snoozed_afterSnoozedTime_returnsNull() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val snoozedTime = start.plusHours(1)
        val reminder = Reminder(1, "Test", null, start, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, start, snoozedTime))

        val next = ReminderInstance.getNextOccurrence(reminder, actions, after = snoozedTime.plusSeconds(1))

        assertEquals(null, next)
    }


    @Test
    fun getNextOccurrence_dailyRecurrence_noActions_returnsFirst() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)

        val next = ReminderInstance.getNextOccurrence(reminder, emptyList())

        assertEquals(
            ReminderInstance(1, "Test", null, start, start, isCompleted = false, isSnoozed = false),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_afterFirst_returnsSecond() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)

        val next = ReminderInstance.getNextOccurrence(reminder, emptyList(), after = start.plusSeconds(1))

        val expectedTime = start.plusDays(1)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false),
            next
        )
    }

    @Test
    fun getNextOccurrence_dailyRecurrence_firstCompleted_returnsSecond() {
        val start = LocalDateTime.of(2024, 7, 1, 10, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Test", null, start, rule)
        val actions = listOf(createAction(1, ActionType.COMPLETED, start))

        val next = ReminderInstance.getNextOccurrence(reminder, actions)

        val expectedTime = start.plusDays(1)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false),
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
        val next = ReminderInstance.getNextOccurrence(reminder, actions)

        assertEquals(
            ReminderInstance(1, "Test", null, snoozedTime, start, isCompleted = false, isSnoozed = true),
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
        val next = ReminderInstance.getNextOccurrence(reminder, actions, after = snoozedTime.plusSeconds(1))

        val expectedTime = start.plusDays(1)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false),
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

        val next = ReminderInstance.getNextOccurrence(reminder, actions)

        assertEquals(null, next)
    }

    @Test
    fun getNextOccurrence_weeklyRecurrence_findsNextCorrectDay() {
        // Starts on a Wednesday
        val start = LocalDateTime.of(2024, 7, 3, 10, 0)
        // Recurrs on Monday, Friday
        val rule = RecurrenceRule(Frequency.WEEKLY, daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val reminder = Reminder(1, "Test", null, start, rule)

        // Find first occurrence after start
        val next = ReminderInstance.getNextOccurrence(reminder, emptyList(), after = start)

        // Expect it to be Friday, July 5th
        val expectedTime = LocalDateTime.of(2024, 7, 5, 10, 0)
        assertEquals(
            ReminderInstance(1, "Test", null, expectedTime, expectedTime, isCompleted = false, isSnoozed = false),
            next
        )
    }

    // --- Tests for getRemindersForDay ---

    @Test
    fun getRemindersForDay_noReminders_returnsEmptyList() {
        val day = LocalDate.of(2024, 7, 10)
        val instances = ReminderInstance.getRemindersForDay(day, emptyList(), emptyList())
        assertEquals(0, instances.size)
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_onDay_noActions() {
        val day = LocalDate.of(2024, 7, 10)
        val reminderTime = day.atTime(9, 0)
        val reminder = Reminder(1, "One-time", null, reminderTime, null)

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), emptyList())

        assertEquals(1, instances.size)
        assertEquals(
            ReminderInstance(1, "One-time", null, reminderTime, reminderTime, isCompleted = false, isSnoozed = false),
            instances.first()
        )
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_completed() {
        val day = LocalDate.of(2024, 7, 10)
        val reminderTime = day.atTime(9, 0)
        val reminder = Reminder(1, "One-time", null, reminderTime, null)
        val actions = listOf(createAction(1, ActionType.COMPLETED, reminderTime))

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertEquals(
            ReminderInstance(1, "One-time", null, reminderTime, reminderTime, isCompleted = true, isSnoozed = false),
            instances.first()
        )
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_snoozed_onSameDay() {
        val day = LocalDate.of(2024, 7, 10)
        val originalTime = day.atTime(9, 0)
        val snoozedTime = day.atTime(11, 0)
        val reminder = Reminder(1, "One-time", null, originalTime, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, originalTime, snoozedTime))

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertEquals(
            ReminderInstance(1, "One-time", null, snoozedTime, originalTime, isCompleted = false, isSnoozed = true),
            instances.first()
        )
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_snoozed_toDifferentDay() {
        val day = LocalDate.of(2024, 7, 10)
        val originalTime = day.atTime(9, 0)
        val snoozedTime = day.plusDays(1).atTime(9, 0)
        val reminder = Reminder(1, "One-time", null, originalTime, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, originalTime, snoozedTime))

        // Should not appear today, as it's snoozed to tomorrow
        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)
        assertEquals(0, instances.size)
    }

    @Test
    fun getRemindersForDay_snoozedFromPreviousDay() {
        val today = LocalDate.of(2024, 7, 10)
        val yesterday = today.minusDays(1)
        val originalTime = yesterday.atTime(20, 0)
        val snoozedTime = today.atTime(8, 30)

        val reminder = Reminder(1, "Snoozed In", null, originalTime, null)
        val actions = listOf(createAction(1, ActionType.SNOOZED, originalTime, snoozedTime))

        val instances = ReminderInstance.getRemindersForDay(today, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertEquals(
            ReminderInstance(1, "Snoozed In", null, snoozedTime, originalTime, isCompleted = false, isSnoozed = true),
            instances.first()
        )
    }

    @Test
    fun getRemindersForDay_dailyRecurrence_withMixedActions() {
        val day = LocalDate.of(2024, 7, 10)
        val start = LocalDateTime.of(2024, 7, 8, 14, 0)
        val rule = RecurrenceRule(Frequency.DAILY)
        val reminder = Reminder(1, "Daily", null, start, rule)

        val occurrenceToday = day.atTime(14, 0)
        val occurrenceTomorrow = day.plusDays(1).atTime(14, 0)

        val actions = listOf(
            createAction(1, ActionType.COMPLETED, start), // Day 1: Completed
            createAction(1, ActionType.SNOOZED, start.plusDays(1), occurrenceTomorrow.plusHours(1)) // Day 2: Snoozed to tomorrow at 15:00
        )

        // For 'today' (July 10th), we expect to see the original occurrence at 14:00
        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertEquals(
            ReminderInstance(1, "Daily", null, occurrenceToday, occurrenceToday, isCompleted = false, isSnoozed = false),
            instances.first()
        )
    }

    @Test
    fun getRemindersForDay_sortsCorrectly() {
        val day = LocalDate.of(2024, 7, 10)

        // Reminder 1: Occurs at 10:00
        val reminder1 = Reminder(1, "First", null, day.atTime(10, 0), null)

        // Reminder 2: Occurs at 08:00, but snoozed to 11:00
        val reminder2Time = day.atTime(8, 0)
        val reminder2Snoozed = day.atTime(11, 0)
        val reminder2 = Reminder(2, "Second Snoozed", null, reminder2Time, null)
        val action2 = createAction(2, ActionType.SNOOZED, reminder2Time, reminder2Snoozed)

        // Reminder 3: Snoozed from yesterday to 09:00 today
        val reminder3Original = day.minusDays(1).atTime(20, 0)
        val reminder3Snoozed = day.atTime(9, 0)
        val reminder3 = Reminder(3, "Third Snoozed In", null, reminder3Original, null)
        val action3 = createAction(3, ActionType.SNOOZED, reminder3Original, reminder3Snoozed)

        val reminders = listOf(reminder1, reminder2, reminder3)
        val actions = listOf(action2, action3)

        val instances = ReminderInstance.getRemindersForDay(day, reminders, actions)

        assertEquals(3, instances.size)
        // Expected order by displayTime: 09:00, 10:00, 11:00
        assertEquals(3, instances[0].reminderId)
        assertEquals(1, instances[1].reminderId)
        assertEquals(2, instances[2].reminderId)

        assertEquals(reminder3Snoozed, instances[0].displayTime)
        assertEquals(day.atTime(10, 0), instances[1].displayTime)
        assertEquals(reminder2Snoozed, instances[2].displayTime)
    }
}
