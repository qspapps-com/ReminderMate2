package com.qspapps.remindermate.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qspapps.remindermate.data.local.ReminderDao
import com.qspapps.remindermate.notifications.ReminderAlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var scheduler: ReminderAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminders = reminderDao.getAll().first()
                    reminders.forEach { scheduler.schedule(it) }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
