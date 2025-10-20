package com.cangzr.neocard.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * A utility class to manage language settings in the app
 */
object LanguageManager {
    private const val PREF_NAME = "language_pref"
    private const val PREF_LANGUAGE_KEY = "selected_language"

    /**
     * Get the currently selected language code
     */
    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Default to system language if not set
        return prefs.getString(PREF_LANGUAGE_KEY, "") ?: ""
    }

    /**
     * Set the app language
     * @param languageCode The language code (e.g., "en", "tr")
     */
    fun setLanguage(context: Context, languageCode: String) {
        // Save the selected language
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_LANGUAGE_KEY, languageCode).apply()

        // Apply the language change
        applyLanguage(languageCode)
    }

    /**
     * Apply the language change to the app
     */
    fun applyLanguage(languageCode: String) {
        val localeList = if (languageCode.isEmpty()) {
            // Use system default
            LocaleListCompat.getDefault()
        } else {
            // Use selected language
            LocaleListCompat.forLanguageTags(languageCode)
        }

        // Apply to app
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Apply the saved language preference
     */
    fun applyLanguageFromPreference(context: Context) {
        val languageCode = getSelectedLanguage(context)
        applyLanguage(languageCode)
    }

    /**
     * Update the configuration with the selected language
     * This is used for older Android versions
     */
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
