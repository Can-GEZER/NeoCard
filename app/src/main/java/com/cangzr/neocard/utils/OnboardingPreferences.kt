package com.cangzr.neocard.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Onboarding durumunu ve kullanıcı tercihlerini yönetmek için yardımcı sınıf
 */
class OnboardingPreferences(private val context: Context) {

    // Onboarding durumu için DataStore
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_preferences")
        private val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        
        @Volatile
        private var instance: OnboardingPreferences? = null
        
        fun getInstance(context: Context): OnboardingPreferences {
            return instance ?: synchronized(this) {
                instance ?: OnboardingPreferences(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Onboarding tamamlandı mı kontrolü
     */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] ?: false
        }

    /**
     * Onboarding durumunu kaydet
     */
    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_ONBOARDING_COMPLETED] = completed
        }
    }
}
