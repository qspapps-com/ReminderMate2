package com.qspapps.remindermate.ui.core

import androidx.annotation.StringRes
import com.qspapps.remindermate.R
import com.qspapps.remindermate.data.model.Frequency

/** The plain name of this frequency, e.g. "Daily". Shared by the filter and the repeat picker. */
@get:StringRes
val Frequency.labelRes: Int
    get() = when (this) {
        Frequency.MINUTE -> R.string.repeat_option_minute
        Frequency.HOURLY -> R.string.repeat_option_hourly
        Frequency.DAILY -> R.string.repeat_option_daily
        Frequency.WEEKLY -> R.string.repeat_option_weekly
        Frequency.MONTHLY -> R.string.repeat_option_monthly
        Frequency.YEARLY -> R.string.repeat_option_yearly
    }
