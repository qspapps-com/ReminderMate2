package com.qspapps.remindermate.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    // The anchor time. For non-recurring, this is the exact time.
    // For recurring, this is the start date and the time of day calculation.
    @Serializable(with = LocalDateTimeSerializer::class) // Custom serializer required for java.time
    val startDateTime: LocalDateTime,

    // If null, it is a one-time reminder.
    val recurrence: RecurrenceRule? = null
)
