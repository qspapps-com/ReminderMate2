package com.qspapps.remindermate.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qspapps.remindermate.data.local.ReminderDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var scheduler: ReminderAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            runBlocking {
                val reminders = reminderDao.getAll().first()
                reminders.forEach { scheduler.schedule(it) }
            }
        }
    }
}
