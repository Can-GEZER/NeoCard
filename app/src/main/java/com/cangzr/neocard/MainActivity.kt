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
import com.cangzr.neocard.utils.NetworkUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.cangzr.neocard.notifications.NotificationManager
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.work.*
import java.util.concurrent.TimeUnit
import com.cangzr.neocard.notifications.NotificationSyncWorker

@AndroidEntryPoint
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
        
        // NetworkUtils örneğini başlat (uygulama genelinde kullanılabilmesi için)
        NetworkUtils.getInstance(this)
        
        // FCM token'ını initialize et
        initializeFCM()
        
        // Notification permission'ını kontrol et
        if (!NotificationManager.hasNotificationPermission(this)) {
            NotificationManager.requestNotificationPermission(this)
        }
        
        // Notification listener'ı başlat
        initializeNotificationListener()
        
        // Deep link ile gelen veriyi kontrol et
        val initialCardId = handleIntent(intent)
        
        // Notification'dan gelen navigation'ı kontrol et
        val navigationTarget = intent.getStringExtra("navigate_to")
        
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
    
    private fun initializeFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("NotificationManager", "FCM token alınamadı", task.exception)
                return@addOnCompleteListener
            }

            // FCM token'ını al
            val token = task.result
            Log.d("NotificationManager", "FCM Token: $token")
            
            // Token'ı Firestore'a kaydet (kullanıcı giriş yaptığında)
            // Bu işlem FCMService'de de yapılıyor, ancak burada da kontrol ediyoruz
        }
    }
    
    private fun initializeNotificationListener() {
        Log.d("NotificationManager", "initializeNotificationListener çağrıldı")
        
        // Kullanıcı giriş yaptığında notification listener'ı başlat
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentUser = auth.currentUser
            Log.d("NotificationManager", "Auth state değişti. CurrentUser: ${currentUser?.uid}")
            
            if (currentUser != null) {
                Log.d("NotificationManager", "Kullanıcı giriş yaptı, notification listener başlatılıyor: ${currentUser.uid}")
                
                // Notification listener'ı başlat
                NotificationManager.listenToNotifications(currentUser.uid) { notification ->
                    Log.d("NotificationManager", "Notification callback çağrıldı: ${notification["title"]}")
                    // Yeni bildirim geldiğinde local notification göster
                    showLocalNotification(notification)
                }
                
                // FCM token'ı güncelle
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("NotificationManager", "FCM Token alındı: $token")
                        lifecycleScope.launch {
                            val result = NotificationManager.updateUserFCMToken(currentUser.uid, token)
                            Log.d("NotificationManager", "FCM Token güncelleme sonucu: $result")
                        }
                    } else {
                        Log.e("NotificationManager", "FCM Token alınamadı", task.exception)
                    }
                }
                
                // Background notification sync'i başlat
                startBackgroundNotificationSync()
            } else {
                Log.d("NotificationManager", "Kullanıcı giriş yapmamış")
            }
        }
    }
    
    private fun showLocalNotification(notification: Map<String, Any>) {
        val title = notification["title"] as? String ?: "NeoCard"
        val body = notification["body"] as? String ?: ""
        val type = notification["type"] as? String ?: "DEFAULT"
        
        Log.d("NotificationManager", "Local notification gösteriliyor: $title")
        
        // NotificationHelper kullanarak notification göster
        com.cangzr.neocard.notifications.NotificationHelper.showNotification(
            context = this,
            title = title,
            body = body,
            type = type,
            data = notification
        )
    }
    
    private fun startBackgroundNotificationSync() {
        Log.d("NotificationManager", "Background notification sync başlatılıyor")
        
        // Periyodik WorkManager task'ı oluştur
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val notificationSyncRequest = PeriodicWorkRequestBuilder<NotificationSyncWorker>(
            15, TimeUnit.MINUTES // Her 15 dakikada bir kontrol et
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                10000L, // 10 saniye backoff
                TimeUnit.MILLISECONDS
            )
            .build()
        
        // WorkManager'a task'ı ekle
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "notification_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            notificationSyncRequest
        )
        
        Log.d("NotificationManager", "Background notification sync planlandı")
    }
    
    private fun handleIntent(intent: Intent): String? {
        // Notification'dan gelen navigation'ı kontrol et
        val navigationTarget = intent.getStringExtra("navigate_to")
        if (navigationTarget != null) {
            Log.d("NotificationManager", "Notification'dan navigation: $navigationTarget")
            // Bu bilgiyi NeoCardApp'e geçirmek için intent'e ekle
            intent.putExtra("initial_route", navigationTarget)
        }
        
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
