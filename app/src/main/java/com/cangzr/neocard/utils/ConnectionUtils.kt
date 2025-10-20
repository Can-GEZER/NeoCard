package com.cangzr.neocard.utils

import android.content.Context
import android.widget.Toast
import com.cangzr.neocard.data.model.User
import com.google.firebase.firestore.FirebaseFirestore

object ConnectionUtils {
    fun fetchUsersByIds(
        userIds: List<String>,
        onComplete: (List<User>) -> Unit
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val users = mutableListOf<User>()

        if (userIds.isEmpty()) {
            onComplete(emptyList())
            return
        }

        var completedCount = 0
        userIds.forEach { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val userData = document.data
                        if (userData != null) {
                            val user = User(
                                id = document.id,
                                displayName = userData["displayName"] as? String ?: "",
                                email = userData["email"] as? String ?: ""
                            )
                            users.add(user)
                        }
                    }
                    completedCount++
                    if (completedCount == userIds.size) {
                        onComplete(users)
                    }
                }
                .addOnFailureListener {
                    completedCount++
                    if (completedCount == userIds.size) {
                        onComplete(users)
                    }
                }
        }
    }
    
    // Bağlantı isteğini kabul etme fonksiyonu
    fun acceptConnectionRequest(
        currentUserId: String?,
        requestUserId: String?,
        cardId: String?,
        onComplete: () -> Unit
    ) {
        if (currentUserId == null || requestUserId == null || cardId == null) return

        val firestore = FirebaseFirestore.getInstance()
        val currentUserRef = firestore.collection("users").document(currentUserId)
        val requestUserRef = firestore.collection("users").document(requestUserId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val requestUserSnapshot = transaction.get(requestUserRef)

            val currentRequests = currentUserSnapshot.get("connectRequests") as? MutableList<Map<String, String>> ?: mutableListOf()
            val currentConnected = currentUserSnapshot.get("connected") as? MutableList<Map<String, String>> ?: mutableListOf()
            val requestUserConnected = requestUserSnapshot.get("connected") as? MutableList<Map<String, String>> ?: mutableListOf()

            // İsteği kaldır
            currentRequests.removeAll { it["userId"] == requestUserId && it["cardId"] == cardId }

            // Bağlantıyı ekle
            currentConnected.add(mapOf("userId" to requestUserId, "cardId" to cardId))
            requestUserConnected.add(mapOf("userId" to currentUserId, "cardId" to cardId))

            // Güncelle
            transaction.update(currentUserRef, "connectRequests", currentRequests)
            transaction.update(currentUserRef, "connected", currentConnected)
            transaction.update(requestUserRef, "connected", requestUserConnected)
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener {
            // Hata durumunda da callback'i çağır
            onComplete()
        }
    }
    
    // Bağlantı isteğini reddetme fonksiyonu
    fun rejectConnectionRequest(
        currentUserId: String?,
        requestUserId: String?,
        cardId: String?,
        onComplete: () -> Unit
    ) {
        if (currentUserId == null || requestUserId == null || cardId == null) return

        val firestore = FirebaseFirestore.getInstance()
        val currentUserRef = firestore.collection("users").document(currentUserId)

        firestore.runTransaction { transaction ->
            val currentUserSnapshot = transaction.get(currentUserRef)
            val currentRequests = currentUserSnapshot.get("connectRequests") as? MutableList<Map<String, String>> ?: mutableListOf()

            // İsteği kaldır
            currentRequests.removeAll { it["userId"] == requestUserId && it["cardId"] == cardId }

            // Güncelle
            transaction.update(currentUserRef, "connectRequests", currentRequests)
        }.addOnSuccessListener {
            onComplete()
        }.addOnFailureListener {
            // Hata durumunda da callback'i çağır
            onComplete()
        }
    }
}