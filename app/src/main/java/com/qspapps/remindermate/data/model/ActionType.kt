package com.qspapps.remindermate.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    COMPLETED, SNOOZED
}
