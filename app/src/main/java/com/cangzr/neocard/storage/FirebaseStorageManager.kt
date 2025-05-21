package com.cangzr.neocard.storage

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

// Firebase Storage yönetimi için yardımcı sınıf
class FirebaseStorageManager {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    // Kullanıcının tüm resimlerini sil
    suspend fun deleteAllUserImages(userId: String): Boolean {
        return try {
            // Kullanıcının resimlerinin bulunduğu klasörü bul
            val listResult = storage.reference.child("user_uploads/$userId").listAll().await()
            
            // Tüm dosyaları sil
            listResult.items.forEach { item ->
                item.delete().await()
            }
            
            // Kullanıcının yüklediği iş ilanı logolarını da sil
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
    
    // Belirli bir kartvizite ait resmi sil
    suspend fun deleteCardImage(userId: String, imageUrl: String): Boolean {
        return try {
            if (imageUrl.isNotEmpty() && imageUrl.contains("user_uploads/$userId")) {
                // URL'den dosya yolunu çıkar
                val path = imageUrl.substringAfter("firebasestorage.googleapis.com/v0/b/")
                    .substringAfter("/o/")
                    .substringBefore("?")
                    .replace("%2F", "/")
                
                storage.reference.child(path).delete().await()
                true
            } else {
                // Resim URL'si boş veya kullanıcıya ait değil
                true
            }
        } catch (e: Exception) {
            println("❌ Kart resmi silinirken hata: ${e.message}")
            false
        }
    }
    
    // Kullanılmayan resimleri temizle (belirli bir süreden eski olan ve hiçbir kartvizitte kullanılmayan resimler)
    suspend fun cleanUnusedImages() {
        try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7) // 7 günden eski
            
            // Tüm kullanıcıları ve kartlarını al
            val usersSnapshot = firestore.collection("users").get().await()
            val allImageUrls = mutableSetOf<String>()
            
            // Tüm kullanıcıların kartlarındaki resim URL'lerini topla
            for (userDoc in usersSnapshot.documents) {
                val cardsSnapshot = userDoc.reference.collection("cards").get().await()
                for (cardDoc in cardsSnapshot.documents) {
                    val imageUrl = cardDoc.getString("profileImageUrl") ?: ""
                    if (imageUrl.isNotEmpty()) {
                        allImageUrls.add(imageUrl)
                    }
                }
            }
            
            // İş ilanlarındaki logoları da ekle
            val jobPostsSnapshot = firestore.collection("jobPosts").get().await()
            for (jobDoc in jobPostsSnapshot.documents) {
                val logoUrl = jobDoc.getString("logoUrl") ?: ""
                if (logoUrl.isNotEmpty()) {
                    allImageUrls.add(logoUrl)
                }
            }
            
            // Tüm kullanıcı yüklemelerini kontrol et
            val allUploadsResult = storage.reference.child("user_uploads").listAll().await()
            
            for (userFolder in allUploadsResult.prefixes) {
                val userUploads = userFolder.listAll().await()
                
                for (item in userUploads.items) {
                    // Dosya meta verilerini al
                    val metadata = item.metadata.await()
                    val creationTime = metadata.creationTimeMillis
                    
                    // Dosya URL'sini al
                    val downloadUrl = item.downloadUrl.await().toString()
                    
                    // Dosya eski ve hiçbir kartta kullanılmıyorsa sil
                    if (creationTime < cutoffTime && !allImageUrls.contains(downloadUrl)) {
                        item.delete().await()
                        println("✅ Kullanılmayan resim silindi: ${item.path}")
                    }
                }
            }
            
            // İş logoları için de aynı işlemi yap
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