package com.cangzr.neocard.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cangzr.neocard.MainActivity
import com.cangzr.neocard.R

object NotificationHelper {
    
    private const val CHANNEL_ID = "neocard_notifications"
    private const val CHANNEL_NAME = "NeoCard Bildirimleri"
    private const val CHANNEL_DESCRIPTION = "Bağlantı istekleri ve diğer bildirimler"
    
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        type: String,
        data: Map<String, Any> = emptyMap()
    ) {
        android.util.Log.d("NotificationManager", "showNotification çağrıldı: $title")
        android.util.Log.d("NotificationManager", "Notification body: $body")
        android.util.Log.d("NotificationManager", "Notification type: $type")
        
        createNotificationChannel(context)
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        android.util.Log.d("NotificationManager", "NotificationManager alındı")
        
        // Intent oluştur - notification'a tıklandığında açılacak ekran
        val intent = Intent(context, MainActivity::class.java).apply {
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
                    data["cardId"]?.let { putExtra("cardId", it.toString()) }
                }
                else -> {
                    putExtra("navigate_to", "home")
                }
            }
            
            // Ek data'ları intent'e ekle
            data.forEach { (key, value) ->
                if (value != null) {
                    putExtra(key, value.toString())
                }
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Notification builder
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
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
                notificationBuilder.setColor(context.getColor(R.color.purple_500))
            }
            "CONNECTION_ACCEPTED" -> {
                notificationBuilder.setColor(context.getColor(R.color.teal_200))
            }
            "CARD_UPDATED" -> {
                notificationBuilder.setColor(context.getColor(R.color.purple_200))
            }
        }
        
        // Notification'ı göster
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
        android.util.Log.d("NotificationManager", "Notification gösterildi - ID: $notificationId")
    }
    
    private fun createNotificationChannel(context: Context) {
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
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
