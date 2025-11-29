package com.qspapps.remindermate.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
@Entity(
    tableName = "reminder_actions",
    primaryKeys = ["reminderId", "originalScheduledTime"],
    foreignKeys = [
        ForeignKey(
            entity = Reminder::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderAction(
    val reminderId: Long,
    @Serializable(with = LocalDateTimeSerializer::class)
    val originalScheduledTime: LocalDateTime,
    val type: ActionType,
    @Serializable(with = LocalDateTimeSerializer::class)
    val resheduledTime: LocalDateTime? = null
)
