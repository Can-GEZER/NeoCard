package com.cangzr.neocard.ui.screens.carddetail.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cangzr.neocard.R
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.notifications.NotificationManager
import com.cangzr.neocard.storage.FirebaseStorageManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object CardDetailRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storageManager = FirebaseStorageManager.getInstance()
    
    fun saveCardWithImage(
        cardId: String,
        card: UserCard,
        imageUri: Uri?,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError(context.getString(R.string.no_logged_in_user))
            return
        }
        
        if (imageUri != null) {
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("profile_images/${currentUser.uid}_${System.currentTimeMillis()}.jpg")
            
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val cardWithImage = card.copy(profileImageUrl = downloadUri.toString())
                        saveCardToFirestore(cardId, cardWithImage, currentUser.uid, context, onSuccess, onError)
                    }
                }
                .addOnFailureListener { e ->
                    onError(context.getString(R.string.profile_image_upload_error, e.localizedMessage))
                }
        } else {
            saveCardToFirestore(cardId, card, currentUser.uid, context, onSuccess, onError)
        }
    }
    
    private fun saveCardToFirestore(
        cardId: String,
        card: UserCard,
        userId: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)
            .set(card)
            .addOnSuccessListener {
                val publicCardData = card.toMap().toMutableMap().apply {
                    put("userId", userId)
                    put("id", cardId)
                    put("isPublic", card.isPublic)
                }
                
                firestore.collection("public_cards")
                    .document(cardId)
                    .set(publicCardData)
                    .addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val userName = card.name
                                val userSurname = card.surname
                                
                                NotificationManager.sendCardUpdatedNotification(
                                    cardOwnerId = userId,
                                    cardOwnerName = userName,
                                    cardOwnerSurname = userSurname,
                                    cardId = cardId
                                )
                                
                                Log.d("CardDetailRepository", "Kartvizit güncelleme bildirimi gönderildi: $userName $userSurname")
                            } catch (e: Exception) {
                                Log.e("CardDetailRepository", "Bildirim gönderilirken hata oluştu", e)
                            }
                        }
                        
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError(context.getString(R.string.update_error, e.localizedMessage))
                    }
            }
            .addOnFailureListener { e ->
                onError(context.getString(R.string.update_error, e.localizedMessage))
            }
    }
    
    fun deleteCardWithImage(
        cardId: String,
        profileImageUrl: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onError(context.getString(R.string.no_logged_in_user))
            return
        }
        
        firestore.collection("users")
            .document(currentUser.uid)
            .collection("cards")
            .document(cardId)
            .delete()
            .addOnSuccessListener {
                firestore.collection("public_cards")
                    .document(cardId)
                    .delete()
                    .addOnSuccessListener {
                        if (profileImageUrl.isNotEmpty()) {
                            CoroutineScope(Dispatchers.IO).launch {
                                storageManager.deleteCardImage(currentUser.uid, profileImageUrl)
                                withContext(Dispatchers.Main) {
                                    onSuccess()
                                }
                            }
                        } else {
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        onError("Kartvizit silindi ancak dış kaynaktan silinemedi: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Kartvizit silinirken hata oluştu: ${e.localizedMessage}")
            }
    }
}
