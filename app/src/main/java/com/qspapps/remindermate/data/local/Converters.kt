package com.qspapps.remindermate.data.local

import androidx.room.TypeConverter
import com.qspapps.remindermate.data.model.RecurrenceRule
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun fromRecurrenceRule(value: String?): RecurrenceRule? {
        return value?.let { RecurrenceRule.fromString(it) }
    }

    @TypeConverter
    fun recurrenceRuleToString(recurrenceRule: RecurrenceRule?): String? {
        return recurrenceRule?.toString()
    }
}
