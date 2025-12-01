package com.qspapps.remindermate.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.qspapps.remindermate.data.model.ReminderAction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminderAction: ReminderAction)

    @Delete
    suspend fun delete(action: ReminderAction)

    @Query("SELECT * FROM reminder_actions WHERE reminderId = :reminderId")
    fun getActionsForReminder(reminderId: Long): Flow<List<ReminderAction>>

    @Query("SELECT * FROM reminder_actions")
    fun getAllActions(): Flow<List<ReminderAction>>
}
