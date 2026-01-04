package com.qspapps.remindermate.data.model

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class RecurrenceRuleTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `toString with only mandatory fields`() {
        val rule = RecurrenceRule(Frequency.DAILY, 1)
        assertEquals("DAILY;1;;", rule.toString())
    }

    @Test
    fun `toString with interval and daysOfWeek`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 2, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        val expected = "WEEKLY;2;MONDAY,WEDNESDAY;"
        assertEquals(expected, rule.toString())
    }

    @Test
    fun `toString with count`() {
        val rule = RecurrenceRule(Frequency.MONTHLY, 1, count = 5)
        assertEquals("MONTHLY;1;;5", rule.toString())
    }

    @Test
    fun `toString with all fields`() {
        val rule = RecurrenceRule(Frequency.YEARLY, 3, setOf(DayOfWeek.FRIDAY), 10)
        assertEquals("YEARLY;3;FRIDAY;10", rule.toString())
    }

    @Test
    fun `fromString happy path mandatory fields`() {
        val ruleString = "DAILY;1;;"
        val expected = RecurrenceRule(Frequency.DAILY, 1)
        assertEquals(expected, RecurrenceRule.fromString(ruleString))
    }

    @Test
    fun `fromString happy path with daysOfWeek`() {
        val ruleString = "WEEKLY;2;MONDAY,WEDNESDAY;"
        val expected = RecurrenceRule(Frequency.WEEKLY, 2, setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(expected, RecurrenceRule.fromString(ruleString))
    }

    @Test
    fun `fromString happy path with count`() {
        val ruleString = "MONTHLY;1;;5"
        val expected = RecurrenceRule(Frequency.MONTHLY, 1, count = 5)
        assertEquals(expected, RecurrenceRule.fromString(ruleString))
    }

    @Test
    fun `fromString happy path all fields`() {
        val ruleString = "YEARLY;3;FRIDAY;10"
        val expected = RecurrenceRule(Frequency.YEARLY, 3, setOf(DayOfWeek.FRIDAY), 10)
        assertEquals(expected, RecurrenceRule.fromString(ruleString))
    }

    @Test
    fun `fromString with minimum parts`() {
        val ruleString = "DAILY;2"
        val expected = RecurrenceRule(Frequency.DAILY, 2)
        assertEquals(expected, RecurrenceRule.fromString(ruleString))
    }

    @Test
    fun `fromString returns null for empty string`() {
        assertNull(RecurrenceRule.fromString(""))
    }

    @Test
    fun `fromString returns null for too few parts`() {
        assertNull(RecurrenceRule.fromString("DAILY"))
    }

    @Test
    fun `fromString returns null for invalid frequency`() {
        assertNull(RecurrenceRule.fromString("INVALID;1"))
    }

    @Test
    fun `fromString returns null for invalid interval`() {
        assertNull(RecurrenceRule.fromString("DAILY;abc"))
    }

    @Test
    fun `fromString returns null for invalid day of week`() {
        assertNull(RecurrenceRule.fromString("WEEKLY;1;NOT_A_DAY"))
    }

    @Test
    fun `fromString returns null for invalid count`() {
        assertNull(RecurrenceRule.fromString("DAILY;1;;abc"))
    }

    @Test
    fun `getNextOccurrence MINUTE with interval 1`() {
        val rule = RecurrenceRule(Frequency.MINUTE, 1)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2023, 1, 1, 10, 1)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence HOURLY with interval 2`() {
        val rule = RecurrenceRule(Frequency.HOURLY, 2)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2023, 1, 1, 12, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence DAILY with interval 1`() {
        val rule = RecurrenceRule(Frequency.DAILY, 1)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2023, 1, 2, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence DAILY with interval 3`() {
        val rule = RecurrenceRule(Frequency.DAILY, 3)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2023, 1, 4, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence WEEKLY no daysOfWeek`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 1)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0) // Sunday
        val expected = LocalDateTime.of(2023, 1, 8, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence WEEKLY with daysOfWeek same week`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val from = LocalDateTime.of(2023, 1, 2, 10, 0) // Monday
        val expected = LocalDateTime.of(2023, 1, 6, 10, 0) // Friday
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence WEEKLY with daysOfWeek next week`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 1, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val from = LocalDateTime.of(2023, 1, 6, 10, 0) // Friday
        val expected = LocalDateTime.of(2023, 1, 9, 10, 0) // Monday
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence WEEKLY with daysOfWeek and interval 2 same week`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 2, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val from = LocalDateTime.of(2023, 1, 2, 10, 0) // Monday, Jan 2
        val expected = LocalDateTime.of(2023, 1, 6, 10, 0) // Friday, Jan 6
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence WEEKLY with daysOfWeek and interval 2 skip week`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 2, setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val from = LocalDateTime.of(2023, 1, 6, 10, 0) // Friday, Jan 6
        // Next Monday is Jan 9. Start of week for Jan 6 is Jan 2 (Monday). 
        // Start of week for Jan 9 is Jan 9 (Monday).
        // Weeks between Jan 2 and Jan 9 is 1. 1 % 2 != 0.
        // So it skips to next interval: 1 + (2 - 1) = 2 weeks skip from Jan 2.
        // Expected: Monday, Jan 16
        val expected = LocalDateTime.of(2023, 1, 16, 10, 0) // Monday, Jan 16
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence WEEKLY with daysOfWeek including Sunday`() {
        val rule = RecurrenceRule(Frequency.WEEKLY, 1, setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        val from = LocalDateTime.of(2023, 1, 1, 10, 0) // Sunday, Jan 1
        val expected = LocalDateTime.of(2023, 1, 7, 10, 0) // Saturday, Jan 7
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence MONTHLY with interval 1`() {
        val rule = RecurrenceRule(Frequency.MONTHLY, 1)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2023, 2, 1, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence MONTHLY with interval 2`() {
        val rule = RecurrenceRule(Frequency.MONTHLY, 2)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2023, 3, 1, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence MONTHLY on Jan 31st with interval 1`() {
        val rule = RecurrenceRule(Frequency.MONTHLY, 1)
        val from = LocalDateTime.of(2023, 1, 31, 10, 0)
        val expected = LocalDateTime.of(2023, 2, 28, 10, 0) // Feb 28 in 2023
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence MONTHLY on Jan 31st with interval 1 in leap year`() {
        val rule = RecurrenceRule(Frequency.MONTHLY, 1)
        val from = LocalDateTime.of(2024, 1, 31, 10, 0)
        val expected = LocalDateTime.of(2024, 2, 29, 10, 0) // Feb 29 in 2024
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence YEARLY with interval 1`() {
        val rule = RecurrenceRule(Frequency.YEARLY, 1)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2024, 1, 1, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }

    @Test
    fun `getNextOccurrence YEARLY with interval 2`() {
        val rule = RecurrenceRule(Frequency.YEARLY, 2)
        val from = LocalDateTime.of(2023, 1, 1, 10, 0)
        val expected = LocalDateTime.of(2025, 1, 1, 10, 0)
        assertEquals(expected, rule.getNextOccurrence(from))
    }
}
