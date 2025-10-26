package com.cangzr.neocard.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cangzr.neocard.MainActivity
import com.cangzr.neocard.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "neocard_notifications"
        private const val CHANNEL_NAME = "NeoCard Bildirimleri"
        private const val CHANNEL_DESCRIPTION = "Bağlantı istekleri ve diğer bildirimler"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Yeni FCM token: $token")
        
        // Token'ı Firestore'a kaydet
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Mesaj alındı: ${remoteMessage.from}")
        
        // Notification payload'ı kontrol et
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "NeoCard"
            val body = notification.body ?: ""
            val type = remoteMessage.data["type"] ?: "DEFAULT"
            
            showNotification(title, body, type, remoteMessage.data)
        }
        
        // Data payload'ı kontrol et
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Mesaj data payload: ${remoteMessage.data}")
            
            val title = remoteMessage.data["title"] ?: "NeoCard"
            val body = remoteMessage.data["message"] ?: ""
            val type = remoteMessage.data["type"] ?: "DEFAULT"
            
            showNotification(title, body, type, remoteMessage.data)
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val firestore = FirebaseFirestore.getInstance()
            
            firestore.collection("users").document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token başarıyla kaydedildi")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM token kaydedilemedi", e)
                }
        }
    }

    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Intent oluştur - notification'a tıklandığında açılacak ekran
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Notification tipine göre farklı ekranlara yönlendir
            when (type) {
                "CONNECTION_REQUEST" -> {
                    putExtra("navigate_to", "connection_requests")
                }
                "CONNECTION_ACCEPTED" -> {
                    putExtra("navigate_to", "connections")
                }
                "CARD_UPDATED" -> {
                    putExtra("navigate_to", "connections")
                    data["cardId"]?.let { putExtra("cardId", it) }
                }
                else -> {
                    putExtra("navigate_to", "home")
                }
            }
            
            // Ek data'ları intent'e ekle
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Notification builder
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo3)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        
        // Notification tipine göre özel ayarlar
        when (type) {
            "CONNECTION_REQUEST" -> {
                notificationBuilder.setColor(getColor(R.color.purple_500))
            }
            "CONNECTION_ACCEPTED" -> {
                notificationBuilder.setColor(getColor(R.color.teal_200))
            }
            "CARD_UPDATED" -> {
                notificationBuilder.setColor(getColor(R.color.purple_200))
            }
        }
        
        // Notification'ı göster
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
