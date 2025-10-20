package com.cangzr.neocard.utils

import android.content.Context
import com.cangzr.neocard.R
import com.cangzr.neocard.storage.FirebaseStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object UserUtils {
    // Admin kullanıcı kontrolü
    fun isAdmin(userId: String?): Boolean {
        // Admin kullanıcı ID'leri - buraya admin kullanıcıların UID'lerini ekleyin
        val adminUserIds = listOf(
            "bhEx5ZPVyOY4YJ61FdaboFhfy1B2", // Örnek admin UID
        )
        return userId != null && adminUserIds.contains(userId)
    }
    
    fun deleteAccount(context: Context, onResult: (Boolean, String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val firestore = FirebaseFirestore.getInstance()
    val storageManager = FirebaseStorageManager.getInstance()

    if (user != null) {
        val uid = user.uid
        
        // Google ile giriş yapan kullanıcılar için özel işlem
        if (user.providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID }) {
            // Google ile giriş yapan kullanıcılar için yeniden kimlik doğrulama gerekli
            // Bu durumda kullanıcıyı bilgilendir
            onResult(false, context.getString(R.string.google_account_reauth_required))
            return
        }
        
        // Standart hesap silme işlemi
        user.delete()
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    // Kullanıcının verilerini silmeden önce kartlarını al
                    firestore.collection("users").document(uid)
                        .collection("cards").get()
                        .addOnSuccessListener { cardsSnapshot ->
                            // Batch işlemi başlat
                            val batch = firestore.batch()
                            
                            // Tüm kartları public_cards koleksiyonundan da sil
                            for (cardDoc in cardsSnapshot.documents) {
                                val cardId = cardDoc.id
                                val publicCardRef = firestore.collection("public_cards").document(cardId)
                                batch.delete(publicCardRef)
                            }
                            
                            // Kullanıcının iş ilanlarını da sil
                            firestore.collection("jobPosts")
                                .whereEqualTo("userId", uid)
                                .get()
                                .addOnSuccessListener { jobPostsSnapshot ->
                                    for (jobDoc in jobPostsSnapshot.documents) {
                                        batch.delete(jobDoc.reference)
                                    }
                                    
                                    // Kullanıcı belgesini sil
                                    batch.delete(firestore.collection("users").document(uid))
                                    
                                    // Batch işlemini uygula
                                    batch.commit().addOnSuccessListener {
                                        // Kullanıcının Storage'daki tüm resimlerini sil
                                        CoroutineScope(Dispatchers.IO).launch {
                                            storageManager.deleteAllUserImages(uid)
                                            withContext(Dispatchers.Main) {
                                onResult(true, context.getString(R.string.account_deleted_successfully))
                            }
                                        }
                                    }.addOnFailureListener { e ->
                            onResult(false, context.getString(R.string.account_deleted_firestore_error, e.localizedMessage ?: ""))
                        }
                                }
                                .addOnFailureListener { e ->
                                    onResult(false, context.getString(R.string.account_deleted_jobs_error, e.localizedMessage ?: ""))
                                }
                        }
                        .addOnFailureListener { e ->
                            onResult(false, context.getString(R.string.account_deleted_cards_error, e.localizedMessage ?: ""))
                        }
                } else {
                    // Kimlik doğrulama gerekiyorsa
                    if (authTask.exception is FirebaseAuthRecentLoginRequiredException) {
                        onResult(false, context.getString(R.string.reauth_required))
                } else {
                    onResult(false, context.getString(R.string.account_delete_error, authTask.exception?.localizedMessage ?: ""))
                    }
                }
            }
    } else {
        onResult(false, context.getString(R.string.no_logged_in_user))
    }
}
}
