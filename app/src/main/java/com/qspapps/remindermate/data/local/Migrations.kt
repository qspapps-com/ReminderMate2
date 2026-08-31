package com.qspapps.remindermate.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.qspapps.remindermate.data.model.RecurrenceRule
import kotlinx.serialization.json.Json

/**
 * Rewrites `reminders.recurrence` from the legacy `FREQUENCY;interval;days;count` text into JSON.
 * The column type is unchanged, so only the row contents are migrated. Rows that cannot be
 * decoded lose their recurrence rather than blocking the migration.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Read every row up front: the cursor must not be open while we write.
        val legacyRules = db.readLegacyRecurrences()
        for ((id, legacy) in legacyRules) {
            val encoded = RecurrenceRule.decodeLegacy(legacy)?.let { Json.encodeToString(it) }
            db.execSQL("UPDATE reminders SET recurrence = ? WHERE id = ?", arrayOf<Any?>(encoded, id))
        }
    }
}

private fun SupportSQLiteDatabase.readLegacyRecurrences(): List<Pair<Long, String>> =
    query("SELECT id, recurrence FROM reminders WHERE recurrence IS NOT NULL").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(cursor.getLong(0) to cursor.getString(1))
            }
        }
    }
