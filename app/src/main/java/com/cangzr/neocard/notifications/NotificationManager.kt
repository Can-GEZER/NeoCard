package com.cangzr.neocard.notifications

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object NotificationManager {
    
    private const val TAG = "NotificationManager"
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Bağlantı isteği bildirimi gönderir (Basit Firestore yaklaşımı)
     */
    suspend fun sendConnectionRequestNotification(
        targetUserId: String,
        senderName: String,
        senderSurname: String,
        cardId: String
    ): Boolean {
        return try {
            // Firestore'a bildirim ekle (uygulama açıldığında görmek için)
            val notificationData = mapOf(
                "type" to "CONNECTION_REQUEST",
                "title" to "Yeni Bağlantı İsteği",
                "body" to "$senderName $senderSurname size bağlantı isteği gönderdi",
                "senderId" to targetUserId,
                "senderName" to senderName,
                "senderSurname" to senderSurname,
                "cardId" to cardId,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "read" to false,
                "received" to false
            )
            
            firestore.collection("users")
                .document(targetUserId)
                .collection("notifications")
                .add(notificationData)
                .await()
            
            updateUnreadNotificationCount(targetUserId)
            Log.d(TAG, "Bildirim Firestore'a eklendi: $targetUserId")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim eklenirken hata oluştu", e)
            false
        }
    }
    
    /**
     * FCM HTTP API ile push notification gönderir
     */
    private suspend fun sendFCMNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean {
        return try {
            val url = "https://fcm.googleapis.com/fcm/send"
            val serverKey = "AAAAyour_server_key_here" // Firebase Console'dan alınacak
            
            val payload = mapOf(
                "to" to token,
                "notification" to mapOf(
                    "title" to title,
                    "body" to body,
                    "sound" to "default"
                ),
                "data" to data,
                "priority" to "high"
            )
            
            // HTTP request gönder (OkHttp veya Retrofit kullanabilirsiniz)
            Log.d(TAG, "FCM HTTP API çağrısı yapılacak")
            // Bu kısım HTTP client implementasyonu gerektirir
            
            true // Şimdilik true döndürüyoruz
            
        } catch (e: Exception) {
            Log.e(TAG, "FCM HTTP API hatası", e)
            false
        }
    }
    
    /**
     * Kullanıcının okunmamış bildirim sayısını günceller
     */
    private suspend fun updateUnreadNotificationCount(userId: String) {
        try {
            val unreadNotifications = firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .await()
            
            val unreadCount = unreadNotifications.size()
            
            firestore.collection("users")
                .document(userId)
                .update("unreadNotificationCount", unreadCount)
                .await()
            
            Log.d(TAG, "Okunmamış bildirim sayısı güncellendi: $userId -> $unreadCount")
            
        } catch (e: Exception) {
            Log.e(TAG, "Okunmamış bildirim sayısı güncellenemedi", e)
        }
    }
    
    /**
     * Bağlantı isteği kabul edilince gönderilen bildirim
     */
    suspend fun sendConnectionAcceptedNotification(
        targetUserId: String,
        accepterUserId: String,
        accepterName: String,
        accepterSurname: String
    ): Boolean {
        return try {
            // Firestore'a bildirim ekle
            val notificationData = mapOf(
                "type" to "CONNECTION_ACCEPTED",
                "title" to "Bağlantı İsteği Kabul Edildi",
                "body" to "$accepterName $accepterSurname bağlantı isteğinizi kabul etti",
                "senderId" to accepterUserId,
                "senderName" to accepterName,
                "senderSurname" to accepterSurname,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "read" to false,
                "received" to false
            )
            
            firestore.collection("users")
                .document(targetUserId)
                .collection("notifications")
                .add(notificationData)
                .await()
            
            updateUnreadNotificationCount(targetUserId)
            Log.d(TAG, "Bağlantı kabul bildirimi gönderildi: $targetUserId")
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Bağlantı kabul bildirimi gönderilemedi", e)
            false
        }
    }
    
    /**
     * Kartvizit güncellenince bağlantılı kullanıcılara bildirim gönderir
     */
    suspend fun sendCardUpdatedNotification(
        cardOwnerId: String,
        cardOwnerName: String,
        cardOwnerSurname: String,
        cardId: String
    ): Boolean {
        return try {
            // Kartvizit sahibinin bağlantılarını al
            val ownerDoc = firestore.collection("users")
                .document(cardOwnerId)
                .get()
                .await()
            
            val connections = ownerDoc.get("connected") as? List<Map<String, String>> ?: emptyList()
            
            // Bu kartviziti bağlantılarında olan kullanıcıları filtrele
            val usersWithThisCard = connections
                .filter { it["cardId"] == cardId }
                .mapNotNull { it["userId"] }
            
            Log.d(TAG, "Kartvizit güncelleme bildirimi gönderilecek kullanıcı sayısı: ${usersWithThisCard.size}")
            
            // Her bir kullanıcıya bildirim gönder
            usersWithThisCard.forEach { userId ->
                try {
                    val notificationData = mapOf(
                        "type" to "CARD_UPDATED",
                        "title" to "Kartvizit Güncellendi",
                        "body" to "$cardOwnerName $cardOwnerSurname kartvizitini güncelledi",
                        "senderId" to cardOwnerId,
                        "senderName" to cardOwnerName,
                        "senderSurname" to cardOwnerSurname,
                        "cardId" to cardId,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "read" to false,
                        "received" to false
                    )
                    
                    firestore.collection("users")
                        .document(userId)
                        .collection("notifications")
                        .add(notificationData)
                        .await()
                    
                    updateUnreadNotificationCount(userId)
                    Log.d(TAG, "Kartvizit güncelleme bildirimi gönderildi: $userId")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Kullanıcıya bildirim gönderilemedi: $userId", e)
                }
            }
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Kartvizit güncelleme bildirimi gönderilemedi", e)
            false
        }
    }
    
    /**
     * Kullanıcının FCM token'ını günceller
     */
    suspend fun updateUserFCMToken(userId: String, token: String): Boolean {
        return try {
            firestore.collection("users").document(userId)
                .update("fcmToken", token)
                .await()
            
            Log.d(TAG, "FCM token güncellendi: $userId")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "FCM token güncellenemedi", e)
            false
        }
    }
    
    /**
     * Kullanıcının bildirimlerini dinler (Real-time)
     */
    fun listenToNotifications(
        userId: String,
        onNotificationReceived: (Map<String, Any>) -> Unit
    ) {
        Log.d(TAG, "Notification listener başlatılıyor: $userId")
        
        firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .whereEqualTo("received", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Bildirim dinleme hatası: $error", error)
                    return@addSnapshotListener
                }
                
                Log.d(TAG, "Snapshot alındı. Doküman sayısı: ${snapshots?.size()}")
                
                if (snapshots != null) {
                    Log.d(TAG, "Snapshot boş değil. DocumentChanges sayısı: ${snapshots.documentChanges.size}")
                    
                    snapshots.documentChanges.forEach { change ->
                        Log.d(TAG, "DocumentChange tipi: ${change.type}")
                        
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            val notification = change.document.data
                            Log.d(TAG, "Yeni bildirim alındı: ${notification["title"]}")
                            Log.d(TAG, "Bildirim içeriği: $notification")
                            
                            // Bildirimi received olarak işaretle
                            change.document.reference.update("received", true)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Bildirim received olarak işaretlendi")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Bildirim received olarak işaretlenemedi", e)
                                }
                            
                            // Callback'i çağır
                            onNotificationReceived(notification)
                        }
                    }
                } else {
                    Log.d(TAG, "Snapshots null")
                }
            }
    }
    
    /**
     * Bildirimi okundu olarak işaretle
     */
    suspend fun markNotificationAsRead(userId: String, notificationId: String): Boolean {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("read", true)
                .await()
            
            // Okunmamış sayıyı güncelle
            updateUnreadNotificationCount(userId)
            
            Log.d(TAG, "Bildirim okundu olarak işaretlendi: $notificationId")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim okundu olarak işaretlenemedi", e)
            false
        }
    }
    
    /**
     * Kullanıcının bildirim izinlerini kontrol eder
     */
    fun hasNotificationPermission(context: android.content.Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13 öncesi için izin gerekli değil
        }
    }
    
    /**
     * Bildirim izni ister
     */
    fun requestNotificationPermission(activity: androidx.activity.ComponentActivity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }
    
    /**
     * Test bildirimi oluşturur (Debug için)
     */
    suspend fun createTestNotification(userId: String): Boolean {
        return try {
            Log.d(TAG, "Test bildirimi oluşturuluyor: $userId")
            
            val notificationData = mapOf(
                "type" to "CONNECTION_REQUEST",
                "title" to "Test Bildirimi",
                "body" to "Bu bir test bildirimidir",
                "senderId" to "test_sender",
                "senderName" to "Test",
                "senderSurname" to "User",
                "cardId" to "test_card",
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "read" to false,
                "received" to false
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("notifications")
                .add(notificationData)
                .await()
            
            Log.d(TAG, "Test bildirimi oluşturuldu")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Test bildirimi oluşturulamadı", e)
            false
        }
    }
}
