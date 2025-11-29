package com.qspapps.remindermate.di

import android.content.Context
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.local.ReminderDatabase
import com.qspapps.remindermate.data.repository.ReminderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideReminderDatabase(@ApplicationContext context: Context): ReminderDatabase {
        return ReminderDatabase.getDatabase(context)
    }

    @Provides
    fun provideReminderDao(database: ReminderDatabase): ReminderDao {
        return database.reminderDao()
    }

    @Provides
    fun provideReminderActionDao(database: ReminderDatabase): ReminderActionDao {
        return database.reminderActionDao()
    }

    @Provides
    @Singleton
    fun provideReminderRepository(reminderDao: ReminderDao, reminderActionDao: ReminderActionDao): ReminderRepository {
        return ReminderRepository(reminderDao, reminderActionDao)
    }
}
