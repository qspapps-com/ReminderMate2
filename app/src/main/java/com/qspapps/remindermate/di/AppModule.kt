package com.qspapps.remindermate.di

import android.content.Context
import androidx.room.Room
import com.qspapps.remindermate.data.local.MIGRATION_3_4
import com.qspapps.remindermate.data.local.ReminderActionDao
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.data.local.ReminderDatabase
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
    fun provideReminderDatabase(@ApplicationContext context: Context): ReminderDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            ReminderDatabase::class.java,
            "reminder_database"
        )
            .addMigrations(MIGRATION_3_4)
            // Schema versions 1 and 2 predate the destructive fallback being removed, so no
            // install can still be on them. Everything from v3 on migrates without data loss.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
            .build()

    @Provides
    fun provideReminderDao(database: ReminderDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideReminderActionDao(database: ReminderDatabase): ReminderActionDao =
        database.reminderActionDao()

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
