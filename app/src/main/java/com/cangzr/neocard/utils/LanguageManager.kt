package com.cangzr.neocard.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {
    private const val PREF_NAME = "language_pref"
    private const val PREF_LANGUAGE_KEY = "selected_language"

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_LANGUAGE_KEY, "") ?: ""
    }

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LANGUAGE_KEY, languageCode).apply()

        applyLanguage(languageCode)
    }

    fun applyLanguage(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            LocaleListCompat.getDefault()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }

        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun applyLanguageFromPreference(context: Context) {
        val languageCode = getSelectedLanguage(context)
        applyLanguage(languageCode)
    }

    fun updateResourcesLegacy(context: Context): Context {
        val languageCode = getSelectedLanguage(context)
        if (languageCode.isEmpty()) {
            return context
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)

        return context.createConfigurationContext(configuration)
    }
}
