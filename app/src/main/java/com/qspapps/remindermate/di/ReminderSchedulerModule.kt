package com.qspapps.remindermate.di

import com.qspapps.remindermate.data.model.ReminderScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReminderSchedulerModule {

    @Provides
    @Singleton
    fun provideReminderScheduler(): ReminderScheduler {
        return ReminderScheduler()
    }
}
