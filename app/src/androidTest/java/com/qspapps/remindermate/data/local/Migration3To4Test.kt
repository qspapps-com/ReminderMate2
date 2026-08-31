package com.qspapps.remindermate.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.qspapps.remindermate.data.model.ActionType
import com.qspapps.remindermate.data.model.Frequency
import com.qspapps.remindermate.data.model.RecurrenceRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Exercises the v3 -> v4 migration against a database built the way v3 actually wrote them:
 * recurrence rules stored as `FREQUENCY;interval;days;count` text.
 *
 * The two schema versions have identical DDL -- only the contents of `reminders.recurrence`
 * changed -- so the v3 fixture is created with the v4 statements exported to `app/schemas`.
 */
@RunWith(AndroidJUnit4::class)
class Migration3To4Test {

    private val dbName = "migration-3-to-4-test"
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun deleteAnyLeftoverDatabase() {
        context.deleteDatabase(dbName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrationConvertsLegacyRecurrenceTextToJson() {
        createV3Database()

        val database = openMigratedDatabase()
        try {
            val reminders = runBlocking { database.reminderDao().getAll().first() }
                .associateBy { it.title }

            assertEquals(5, reminders.size)

            assertEquals(
                RecurrenceRule(Frequency.DAILY, interval = 1),
                reminders.getValue("daily").recurrence
            )
            assertEquals(
                RecurrenceRule(
                    Frequency.WEEKLY,
                    interval = 2,
                    daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)
                ),
                reminders.getValue("every other mon and wed").recurrence
            )
            assertEquals(
                RecurrenceRule(Frequency.MONTHLY, interval = 1, count = 5),
                reminders.getValue("monthly five times").recurrence
            )

            // A one-off had no rule to begin with.
            assertNull(reminders.getValue("one off").recurrence)
            // An undecodable rule loses its recurrence rather than blocking the migration or
            // leaving text that the new converter would choke on at read time.
            assertNull(reminders.getValue("corrupt rule").recurrence)

            // Actions are untouched by the migration and must still resolve against their parent.
            val actions = runBlocking { database.reminderActionDao().getAllActions().first() }
            assertEquals(1, actions.size)
            assertEquals(ActionType.COMPLETED, actions.single().type)
            assertEquals(
                LocalDateTime.parse("2026-01-01T09:00"),
                actions.single().originalScheduledTime
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun migratedDatabaseIsAtVersion4() {
        createV3Database()

        val database = openMigratedDatabase()
        try {
            assertEquals(4, database.openHelper.readableDatabase.version)
        } finally {
            database.close()
        }
    }

    private fun openMigratedDatabase(): ReminderDatabase =
        Room.databaseBuilder(context, ReminderDatabase::class.java, dbName)
            .addMigrations(MIGRATION_3_4)
            .build()
            // Opening is lazy; touching the helper forces the migration to run now so a failure
            // surfaces here rather than inside an assertion.
            .also { it.openHelper.writableDatabase }

    /** Writes a database exactly as schema version 3 left it. */
    private fun createV3Database() {
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        db.use {
            it.execSQL(
                "CREATE TABLE IF NOT EXISTS `reminders` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`title` TEXT NOT NULL, " +
                    "`description` TEXT, " +
                    "`startDateTime` TEXT NOT NULL, " +
                    "`recurrence` TEXT)"
            )
            it.execSQL(
                "CREATE TABLE IF NOT EXISTS `reminder_actions` (" +
                    "`reminderId` INTEGER NOT NULL, " +
                    "`originalScheduledTime` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`rescheduledTime` TEXT, " +
                    "PRIMARY KEY(`reminderId`, `originalScheduledTime`), " +
                    "FOREIGN KEY(`reminderId`) REFERENCES `reminders`(`id`) " +
                    "ON UPDATE NO ACTION ON DELETE CASCADE )"
            )

            // Recurrence values in the legacy format produced by RecurrenceRule.toString() in v3.
            it.insertV3Reminder(1, "daily", "DAILY;1;;")
            it.insertV3Reminder(2, "every other mon and wed", "WEEKLY;2;MONDAY,WEDNESDAY;")
            it.insertV3Reminder(3, "monthly five times", "MONTHLY;1;;5")
            it.insertV3Reminder(4, "one off", null)
            it.insertV3Reminder(5, "corrupt rule", "NOT_A_FREQUENCY;nope")

            it.execSQL(
                "INSERT INTO reminder_actions " +
                    "(reminderId, originalScheduledTime, type, rescheduledTime) " +
                    "VALUES (1, '2026-01-01T09:00', 'COMPLETED', NULL)"
            )

            // Room decides whether to migrate from the file's user_version.
            it.version = 3
        }
    }
}

private fun android.database.sqlite.SQLiteDatabase.insertV3Reminder(
    id: Long,
    title: String,
    recurrence: String?
) = execSQL(
    "INSERT INTO reminders (id, title, description, startDateTime, recurrence) " +
        "VALUES (?, ?, NULL, '2026-01-01T09:00', ?)",
    arrayOf<Any?>(id, title, recurrence)
)
