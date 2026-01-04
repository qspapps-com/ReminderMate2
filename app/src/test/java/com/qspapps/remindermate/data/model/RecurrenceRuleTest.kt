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
        // Note: Set order might matter for joinToString, but usually it's stable (LinkedHashSet)
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
}
