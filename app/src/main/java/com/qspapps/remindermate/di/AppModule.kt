package com.qspapps.remindermate.di

import android.content.Context
import androidx.room.Room
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.local.ReminderDatabase
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.notifications.NotificationService
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideReminderDatabase(@ApplicationContext context: Context): ReminderDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            ReminderDatabase::class.java,
            "reminder_database"
        )
            .fallbackToDestructiveMigration(true)
            .build()
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

    @Provides
    @Singleton
    fun provideReminderAlarmScheduler(@ApplicationContext context: Context, reminderRepository: ReminderRepository): ReminderAlarmScheduler {
        return ReminderAlarmScheduler(context, reminderRepository)
    }

    @Provides
    @Singleton
    fun provideNotificationService(@ApplicationContext context: Context): NotificationService {
        return NotificationService(context)
    }

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

}
