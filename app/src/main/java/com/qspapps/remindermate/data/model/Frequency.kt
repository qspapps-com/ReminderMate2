package com.qspapps.remindermate.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Frequency {
    MINUTE, HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
}
