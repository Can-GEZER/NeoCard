package com.cangzr.neocard.storage

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    suspend fun deleteAllUserImages(userId: String): Boolean {
        return try {
            val listResult = storage.reference.child("user_uploads/$userId").listAll().await()
            
            listResult.items.forEach { item ->
                item.delete().await()
            }
            
            val jobLogos = storage.reference.child("job_logos").listAll().await()
            jobLogos.items.forEach { item ->
                if (item.name.contains(userId)) {
                    item.delete().await()
                }
            }
            
            true
        } catch (e: Exception) {
            println("❌ Kullanıcı resimleri silinirken hata: ${e.message}")
            false
        }
    }
    
    suspend fun deleteCardImage(userId: String, imageUrl: String): Boolean {
        return try {
            if (imageUrl.isNotEmpty() && imageUrl.contains("user_uploads/$userId")) {
                val path = imageUrl.substringAfter("firebasestorage.googleapis.com/v0/b/")
                    .substringAfter("/o/")
                    .substringBefore("?")
                    .replace("%2F", "/")
                
                storage.reference.child(path).delete().await()
                true
            } else {
                true
            }
        } catch (e: Exception) {
            println("❌ Kart resmi silinirken hata: ${e.message}")
            false
        }
    }
    
    suspend fun cleanUnusedImages() {
        try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7) // 7 günden eski
            
            val usersSnapshot = firestore.collection("users").get().await()
            val allImageUrls = mutableSetOf<String>()
            
            for (userDoc in usersSnapshot.documents) {
                val cardsSnapshot = userDoc.reference.collection("cards").get().await()
                for (cardDoc in cardsSnapshot.documents) {
                    val imageUrl = cardDoc.getString("profileImageUrl") ?: ""
                    if (imageUrl.isNotEmpty()) {
                        allImageUrls.add(imageUrl)
                    }
                }
            }
            
            val jobPostsSnapshot = firestore.collection("jobPosts").get().await()
            for (jobDoc in jobPostsSnapshot.documents) {
                val logoUrl = jobDoc.getString("logoUrl") ?: ""
                if (logoUrl.isNotEmpty()) {
                    allImageUrls.add(logoUrl)
                }
            }
            
            val allUploadsResult = storage.reference.child("user_uploads").listAll().await()
            
            for (userFolder in allUploadsResult.prefixes) {
                val userUploads = userFolder.listAll().await()
                
                for (item in userUploads.items) {
                    val metadata = item.metadata.await()
                    val creationTime = metadata.creationTimeMillis
                    
                    val downloadUrl = item.downloadUrl.await().toString()
                    
                    if (creationTime < cutoffTime && !allImageUrls.contains(downloadUrl)) {
                        item.delete().await()
                        println("✅ Kullanılmayan resim silindi: ${item.path}")
                    }
                }
            }
            
            val jobLogosResult = storage.reference.child("job_logos").listAll().await()
            for (item in jobLogosResult.items) {
                val metadata = item.metadata.await()
                val creationTime = metadata.creationTimeMillis
                val downloadUrl = item.downloadUrl.await().toString()
                
                if (creationTime < cutoffTime && !allImageUrls.contains(downloadUrl)) {
                    item.delete().await()
                    println("✅ Kullanılmayan iş logosu silindi: ${item.path}")
                }
            }
            
        } catch (e: Exception) {
            println("❌ Kullanılmayan resimler temizlenirken hata: ${e.message}")
        }
    }
    
    companion object {
        @Volatile
        private var instance: FirebaseStorageManager? = null
        
        fun getInstance(): FirebaseStorageManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseStorageManager().also { instance = it }
            }
        }
    }
} 
