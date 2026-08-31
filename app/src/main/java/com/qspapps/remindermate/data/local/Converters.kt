package com.qspapps.remindermate.data.local

import androidx.room.TypeConverter
import com.qspapps.remindermate.data.model.RecurrenceRule
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let(LocalDateTime::parse)

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? = dateTime?.toString()

    @TypeConverter
    fun toRecurrenceRule(value: String?): RecurrenceRule? = value?.let(Json::decodeFromString)

    @TypeConverter
    fun fromRecurrenceRule(rule: RecurrenceRule?): String? = rule?.let(Json::encodeToString)
}
