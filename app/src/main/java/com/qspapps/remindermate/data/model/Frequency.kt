package com.qspapps.remindermate.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Frequency {
    HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY
}