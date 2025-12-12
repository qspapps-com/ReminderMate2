
package com.qspapps.remindermate.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {

    fun setLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            "en" to "English",
            "hi" to "Hindi",
            "es" to "Spanish",
            "de" to "German",
            "fr" to "French",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ar" to "Arabic",
            "kn" to "Kannada",
            "te" to "Telugu",
            "ta" to "Tamil",
            "ml" to "Malayalam",
            "gu" to "Gujarati",
            "bn" to "Bengali",
            "mr" to "Marathi"
        )
    }
}
