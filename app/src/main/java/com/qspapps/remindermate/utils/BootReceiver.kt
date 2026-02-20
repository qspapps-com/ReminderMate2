package com.qspapps.remindermate.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qspapps.remindermate.data.repository.ReminderRepository
import com.qspapps.remindermate.data.repository.UserPreferencesRepository
import com.qspapps.remindermate.di.ApplicationScope
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var scheduler: ReminderAlarmScheduler

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            applicationScope.launch {
                try {
                    val reminders = reminderRepository.getAllReminders().first()
                    reminders.forEach { scheduler.schedule(it) }
                } catch (e: Exception) {
                    userPreferencesRepository.saveError("BootReceiver Error: ${e.localizedMessage}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
