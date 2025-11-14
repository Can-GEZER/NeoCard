package com.cangzr.neocard.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationSyncWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Background notification sync başlatıldı")
            
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Log.d(TAG, "Kullanıcı giriş yapmamış, sync iptal edildi")
                return Result.success()
            }
            
            val firestore = FirebaseFirestore.getInstance()
            val unreadNotifications = firestore.collection("users")
                .document(currentUser.uid)
                .collection("notifications")
                .whereEqualTo("received", false)
                .get()
                .await()
            
            Log.d(TAG, "Okunmamış bildirim sayısı: ${unreadNotifications.size()}")
            
            unreadNotifications.documents.forEach { doc ->
                val notification = doc.data
                if (notification != null) {
                    val title = notification["title"] as? String ?: "NeoCard"
                    val body = notification["body"] as? String ?: ""
                    val type = notification["type"] as? String ?: "DEFAULT"
                    
                    Log.d(TAG, "Background notification gösteriliyor: $title")
                    
                    NotificationHelper.showNotification(
                        context = applicationContext,
                        title = title,
                        body = body,
                        type = type,
                        data = notification
                    )
                    
                    doc.reference.update("received", true)
                }
            }
            
            Log.d(TAG, "Background notification sync tamamlandı")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Background notification sync hatası", e)
            Result.retry()
        }
    }
}
