package com.qspapps.remindermate.di

import android.content.Context
import com.qspapps.remindermate.data.BackupAndRestore
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.local.ReminderDatabase
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.utils.NotificationService
import com.qspapps.remindermate.utils.ReminderAlarmScheduler
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

    @Provides
    @Singleton
    fun provideBackupAndRestore(): BackupAndRestore {
        return BackupAndRestore()
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }
}
