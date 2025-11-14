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
    fun isAdmin(userId: String?): Boolean {
        val adminUserIds = listOf(
            "bhEx5ZPVyOY4YJ61FdaboFhfy1B2", // Örnek admin UID
        )
        return userId != null && adminUserIds.contains(userId)
    }
    
    fun deleteAccount(context: Context, onResult: (Boolean, String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val firestore = FirebaseFirestore.getInstance()
        val storageManager = FirebaseStorageManager.getInstance()

        if (user != null) {
            val uid = user.uid
            
            deleteUserData(uid, firestore, storageManager, context) { success, message ->
                if (success) {
                    auth.signOut()
                    onResult(true, context.getString(R.string.account_deleted_successfully))
                } else {
                    onResult(false, message)
                }
            }
        } else {
            onResult(false, context.getString(R.string.no_logged_in_user))
        }
    }
    
    private fun deleteUserData(
        uid: String,
        firestore: FirebaseFirestore,
        storageManager: FirebaseStorageManager,
        context: Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        firestore.collection("users").document(uid)
            .collection("cards").get()
            .addOnSuccessListener { cardsSnapshot ->
                val batch = firestore.batch()
                
                for (cardDoc in cardsSnapshot.documents) {
                    batch.delete(cardDoc.reference)
                    
                    val cardId = cardDoc.id
                    val publicCardRef = firestore.collection("public_cards").document(cardId)
                    batch.delete(publicCardRef)
                }
                
                firestore.collection("users").document(uid)
                    .collection("notifications").get()
                    .addOnSuccessListener { notificationsSnapshot ->
                        for (notifDoc in notificationsSnapshot.documents) {
                            batch.delete(notifDoc.reference)
                        }
                        
                        firestore.collection("jobPosts")
                            .whereEqualTo("userId", uid)
                            .get()
                            .addOnSuccessListener { jobPostsSnapshot ->
                                for (jobDoc in jobPostsSnapshot.documents) {
                                    batch.delete(jobDoc.reference)
                                }
                                
                                batch.delete(firestore.collection("users").document(uid))
                                
                                batch.commit()
                                    .addOnSuccessListener {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                storageManager.deleteAllUserImages(uid)
                                                withContext(Dispatchers.Main) {
                                                    onComplete(true, context.getString(R.string.account_deleted_successfully))
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    onComplete(true, context.getString(R.string.account_deleted_successfully))
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false, context.getString(R.string.account_deleted_firestore_error, e.localizedMessage ?: ""))
                                    }
                            }
                            .addOnFailureListener { e ->
                                onComplete(false, context.getString(R.string.account_deleted_jobs_error, e.localizedMessage ?: ""))
                            }
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, "Bildirimler silinirken hata: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false, context.getString(R.string.account_deleted_cards_error, e.localizedMessage ?: ""))
            }
    }
}
