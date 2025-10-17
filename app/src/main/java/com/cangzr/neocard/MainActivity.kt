package com.cangzr.neocard

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cangzr.neocard.NeoCardApp
import com.cangzr.neocard.ui.theme.NeoCardTheme
import com.cangzr.neocard.utils.LanguageManager
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // Apply language settings for older Android versions
        val context = LanguageManager.updateResourcesLegacy(newBase)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Firebase'i başlat
        FirebaseApp.initializeApp(this)
        
        // Crashlytics'i başlat
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Apply language settings
        LanguageManager.applyLanguageFromPreference(this)
        
        // Deep link ile gelen veriyi kontrol et
        val initialCardId = handleIntent(intent)
        
        setContent {
            NeoCardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NeoCardApp(initialCardId)
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Yeni intent geldiğinde de kontrol et
        handleIntent(intent)?.let { cardId ->
            setContent {
                NeoCardTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NeoCardApp(cardId)
                    }
                }
            }
        }
    }
    
    private fun handleIntent(intent: Intent): String? {
        // Deep link ile gelen veriyi işle
        val uri = intent.data ?: return null
        
        // URL şemasını kontrol et
        return when {
            // https://neocardapp.com/card/{cardId}
            uri.host == "neocardapp.com" && uri.pathSegments.size > 1 && uri.pathSegments[0] == "card" -> {
                uri.pathSegments[1]
            }
            // neocard://card/{cardId}
            uri.scheme == "neocard" && uri.host == "card" -> {
                uri.pathSegments.firstOrNull()
            }
            else -> null
        }
    }
}
