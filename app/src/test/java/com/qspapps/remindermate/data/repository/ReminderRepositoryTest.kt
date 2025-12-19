package com.qspapps.remindermate.data.repository

import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ReminderRepositoryTest {

    private val reminderDao = mockk<ReminderDao>(relaxed = true)
    private val reminderActionDao = mockk<ReminderActionDao>(relaxed = true)
    private lateinit var repository: ReminderRepository

    @Before
    fun setUp() {
        repository = ReminderRepository(reminderDao, reminderActionDao)
    }

    @Test
    fun `cleanupOldReminders should delete one-off reminder if completed before threshold`() = runBlocking {
        val threshold = LocalDateTime.now().minusDays(30)
        val reminder = Reminder(id = 1, title = "Test", startDateTime = threshold.minusDays(1))
        val actions = listOf(ReminderAction(reminderId = 1, originalScheduledTime = reminder.startDateTime, type = ActionType.COMPLETED))

        coEvery { reminderDao.getAll() } returns flowOf(listOf(reminder))
        coEvery { reminderActionDao.getAllActions() } returns flowOf(actions)

        repository.cleanupOldReminders(threshold)

        coVerify { reminderDao.deleteById(1) }
    }

    @Test
    fun `cleanupOldReminders should delete one-off reminder if deleted before threshold`() = runBlocking {
        val threshold = LocalDateTime.now().minusDays(30)
        val reminder = Reminder(id = 1, title = "Test", startDateTime = threshold.minusDays(1))
        val actions = listOf(ReminderAction(reminderId = 1, originalScheduledTime = reminder.startDateTime, type = ActionType.DELETED))

        coEvery { reminderDao.getAll() } returns flowOf(listOf(reminder))
        coEvery { reminderActionDao.getAllActions() } returns flowOf(actions)

        repository.cleanupOldReminders(threshold)

        coVerify { reminderDao.deleteById(1) }
    }

    @Test
    fun `cleanupOldReminders should not delete one-off reminder if not completed or deleted`() = runBlocking {
        val threshold = LocalDateTime.now().minusDays(30)
        val reminder = Reminder(id = 1, title = "Test", startDateTime = threshold.minusDays(1))
        val actions = emptyList<ReminderAction>()

        coEvery { reminderDao.getAll() } returns flowOf(listOf(reminder))
        coEvery { reminderActionDao.getAllActions() } returns flowOf(actions)

        repository.cleanupOldReminders(threshold)

        coVerify(exactly = 0) { reminderDao.deleteById(any()) }
    }

    @Test
    fun `cleanupOldReminders should move startDateTime for recurring reminder and delete old actions`() = runBlocking {
        val threshold = LocalDateTime.now().minusDays(30)
        val startDateTime = threshold.minusDays(40)
        val reminder = Reminder(
            id = 1,
            title = "Test Recurring",
            startDateTime = startDateTime,
            recurrence = RecurrenceRule(Frequency.DAILY)
        )
        // All occurrences before threshold are completed
        val actions = listOf(
            ReminderAction(reminderId = 1, originalScheduledTime = startDateTime, type = ActionType.COMPLETED),
            ReminderAction(reminderId = 1, originalScheduledTime = startDateTime.plusDays(1), type = ActionType.COMPLETED)
        )

        coEvery { reminderDao.getAll() } returns flowOf(listOf(reminder))
        coEvery { reminderActionDao.getAllActions() } returns flowOf(actions)

        repository.cleanupOldReminders(threshold)

        // It should move startDateTime to at least threshold
        coVerify { reminderDao.update(match { it.startDateTime.isAfter(startDateTime) }) }
        coVerify { reminderActionDao.delete(any()) }
    }

    @Test
    fun `cleanupOldReminders should not move startDateTime if first occurrence is not acted upon`() = runBlocking {
        val threshold = LocalDateTime.now().minusDays(30)
        val startDateTime = threshold.minusDays(10)
        val reminder = Reminder(
            id = 1,
            title = "Test Recurring",
            startDateTime = startDateTime,
            recurrence = RecurrenceRule(Frequency.DAILY)
        )
        val actions = emptyList<ReminderAction>()

        coEvery { reminderDao.getAll() } returns flowOf(listOf(reminder))
        coEvery { reminderActionDao.getAllActions() } returns flowOf(actions)

        repository.cleanupOldReminders(threshold)

        coVerify(exactly = 0) { reminderDao.update(any()) }
    }

    @Test
    fun `cleanupOldReminders should delete recurring reminder if no more occurrences and all past are handled`() = runBlocking {
        val threshold = LocalDateTime.now().minusDays(30)
        val startDateTime = threshold.minusDays(5)
        val reminder = Reminder(
            id = 1,
            title = "Test Recurring",
            startDateTime = startDateTime,
            recurrence = RecurrenceRule(Frequency.DAILY, count = 1) // Only one occurrence
        )
        val actions = listOf(
            ReminderAction(reminderId = 1, originalScheduledTime = startDateTime, type = ActionType.COMPLETED)
        )

        coEvery { reminderDao.getAll() } returns flowOf(listOf(reminder))
        coEvery { reminderActionDao.getAllActions() } returns flowOf(actions)

        repository.cleanupOldReminders(threshold)

        coVerify { reminderDao.deleteById(1) }
    }
}
