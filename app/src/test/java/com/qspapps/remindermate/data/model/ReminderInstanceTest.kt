package com.qspapps.remindermate.data.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderInstanceTest {

    // Helper to create a reminder
    private fun createReminder(id: Long, start: LocalDateTime, rule: RecurrenceRule? = null): Reminder {
        return Reminder(id, "Test $id", null, start, rule)
    }

    // Helper to create an action
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
    fun getRemindersForDay_noReminders_returnsEmptyList() {
        val day = LocalDate.of(2024, 7, 10)
        val instances = ReminderInstance.getRemindersForDay(day, emptyList(), emptyList())
        assertTrue(instances.isEmpty())
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_onDay_noActions() {
        val day = LocalDate.of(2024, 7, 10)
        val reminderTime = day.atTime(9, 0)
        val reminder = createReminder(1, reminderTime)

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), emptyList())

        assertEquals(1, instances.size)
        assertEquals(reminderTime, instances.first().displayTime)
        assertFalse(instances.first().isCompleted)
        assertFalse(instances.first().isSnoozed)
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_completed() {
        val day = LocalDate.of(2024, 7, 10)
        val reminderTime = day.atTime(9, 0)
        val reminder = createReminder(1, reminderTime)
        val actions = listOf(createAction(1, ActionType.COMPLETED, reminderTime))

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertTrue(instances.first().isCompleted)
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_snoozed_onSameDay() {
        val day = LocalDate.of(2024, 7, 10)
        val originalTime = day.atTime(9, 0)
        val snoozedTime = day.atTime(11, 0)
        val reminder = createReminder(1, originalTime)
        val actions = listOf(createAction(1, ActionType.SNOOZED, originalTime, snoozedTime))

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertEquals(snoozedTime, instances.first().displayTime)
        assertTrue(instances.first().isSnoozed)
    }

    @Test
    fun getRemindersForDay_oneTimeReminder_snoozed_toDifferentDay() {
        val day = LocalDate.of(2024, 7, 10)
        val originalTime = day.atTime(9, 0)
        val snoozedTime = day.plusDays(1).atTime(9, 0)
        val reminder = createReminder(1, originalTime)
        val actions = listOf(createAction(1, ActionType.SNOOZED, originalTime, snoozedTime))

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        // The original is replaced by the snoozed one, which is not on this day
        assertTrue(instances.isEmpty())
    }

    @Test
    fun getRemindersForDay_snoozedFromPreviousDay() {
        val today = LocalDate.of(2024, 7, 10)
        val yesterday = today.minusDays(1)
        val originalTime = yesterday.atTime(20, 0)
        val snoozedTime = today.atTime(8, 30)
        val reminder = createReminder(1, originalTime) // Original start was yesterday
        val actions = listOf(createAction(1, ActionType.SNOOZED, originalTime, snoozedTime))

        val instances = ReminderInstance.getRemindersForDay(today, listOf(reminder), actions)

        assertEquals(1, instances.size)
        assertEquals(snoozedTime, instances.first().displayTime)
        assertTrue(instances.first().isSnoozed)
    }

    @Test
    fun getRemindersForDay_dailyRecurrence_withMixedActions() {
        val day = LocalDate.of(2024, 7, 10)
        val start = LocalDateTime.of(2024, 7, 8, 14, 0)
        val reminder = createReminder(1, start, RecurrenceRule(Frequency.DAILY))

        val actions = listOf(
            createAction(1, ActionType.COMPLETED, start), // Day 1 (7/8) is completed
            // Day 2 (7/9) is snoozed to a different day, so it shouldn't appear on 7/9
            createAction(1, ActionType.SNOOZED, start.plusDays(1), start.plusDays(2).plusHours(1))
        )

        // For 'today' (July 10th), we expect the original, unaltered occurrence
        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder), actions)

        assertEquals(2, instances.size)
        assertEquals(day.atTime(14, 0), instances.first().displayTime)
        assertFalse(instances.first().isCompleted)
        assertFalse(instances.first().isSnoozed)
    }

    @Test
    fun getRemindersForDay_sortsCorrectly() {
        val day = LocalDate.of(2024, 7, 10)

        val reminder1 = createReminder(1, day.atTime(10, 0)) // 10:00
        val reminder2 = createReminder(2, day.atTime(8, 0)) // Snoozed to 11:00
        val reminder3 = createReminder(3, day.minusDays(1).atTime(20, 0)) // Snoozed from yesterday to 09:00

        val actions = listOf(
            createAction(2, ActionType.SNOOZED, reminder2.startDateTime, day.atTime(11, 0)),
            createAction(3, ActionType.SNOOZED, reminder3.startDateTime, day.atTime(9, 0))
        )

        val instances = ReminderInstance.getRemindersForDay(day, listOf(reminder1, reminder2, reminder3), actions)

        assertEquals(3, instances.size)
        // Expected order by displayTime: 09:00, 10:00, 11:00
        assertEquals(3, instances[0].reminderId)
        assertEquals(1, instances[1].reminderId)
        assertEquals(2, instances[2].reminderId)
    }
}