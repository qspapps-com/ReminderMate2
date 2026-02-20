package com.qspapps.remindermate.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.qspapps.remindermate.data.model.Reminder
import com.qspapps.remindermate.data.model.ReminderAction

@Database(entities = [Reminder::class, ReminderAction::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun reminderActionDao(): ReminderActionDao
}
