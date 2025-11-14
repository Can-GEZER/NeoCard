package com.cangzr.neocard.data.repository.impl

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.common.safeApiCall
import com.cangzr.neocard.data.model.TextStyleDTO
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCardRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : CardRepository {

    override suspend fun getCards(
        userId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>> = safeApiCall {
        var query = firestore.collection("users")
            .document(userId)
            .collection("cards")
            .limit(pageSize.toLong())

        if (lastCardId != null) {
            val lastDoc = firestore.collection("users")
                .document(userId)
                .collection("cards")
                .document(lastCardId)
                .get()
                .await()

            if (lastDoc.exists()) {
                query = firestore.collection("users")
                    .document(userId)
                    .collection("cards")
                    .orderBy("id")
                    .startAfter(lastDoc)
                    .limit(pageSize.toLong())
            }
        }

        val querySnapshot = query.get().await()
        val cards = querySnapshot.documents.mapNotNull { doc ->
            try {
                parseUserCard(doc.id, doc.data)
            } catch (e: Exception) {
                null
            }
        }

        val lastId = if (cards.isNotEmpty()) cards.last().id else null
        val hasMore = cards.size >= pageSize

        Triple(cards, lastId, hasMore)
    }

    override suspend fun getCardById(
        userId: String,
        cardId: String
    ): Resource<UserCard?> = safeApiCall {
        val document = firestore.collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)
            .get()
            .await()

        if (document.exists()) {
            parseUserCard(document.id, document.data)
        } else {
            null
        }
    }

    override suspend fun saveCard(
        userId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<String> = safeApiCall {
        var cardData = card.toMap()

        if (imageUri != null) {
            val imageUrl = uploadProfileImage(userId, imageUri)
            cardData = cardData.toMutableMap().apply {
                put("profileImageUrl", imageUrl)
            }
        }

        val cardRef = firestore.collection("users")
            .document(userId)
            .collection("cards")
            .add(cardData)
            .await()

        val cardId = cardRef.id

        if (card.isPublic) {
            val publicCardData = cardData.toMutableMap().apply {
                put("id", cardId)
                put("userId", userId)
                put("isPublic", true)
            }

            firestore.collection("public_cards")
                .document(cardId)
                .set(publicCardData)
                .await()
        }

        cardId
    }

    override suspend fun updateCard(
        userId: String,
        cardId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<Unit> = safeApiCall {
        var cardData = card.toMap()

        if (imageUri != null) {
            val imageUrl = uploadProfileImage(userId, imageUri)
            cardData = cardData.toMutableMap().apply {
                put("profileImageUrl", imageUrl)
            }
        }

        firestore.collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)
            .set(cardData)
            .await()

        val publicCardData = cardData.toMutableMap().apply {
            put("userId", userId)
            put("id", cardId)
            put("isPublic", card.isPublic)
        }

        firestore.collection("public_cards")
            .document(cardId)
            .set(publicCardData)
            .await()
    }

    override suspend fun deleteCard(
        userId: String,
        cardId: String,
        profileImageUrl: String
    ): Resource<Unit> = safeApiCall {
        firestore.collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)
            .delete()
            .await()

        firestore.collection("public_cards")
            .document(cardId)
            .delete()
            .await()

        if (profileImageUrl.isNotEmpty()) {
            try {
                val storageRef = storage.getReferenceFromUrl(profileImageUrl)
                storageRef.delete().await()
            } catch (e: Exception) {
            }
        }
    }

    override suspend fun getExploreCards(
        currentUserId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>> = safeApiCall {
        val userDoc = firestore.collection("users")
            .document(currentUserId)
            .get()
            .await()

        val connectedList = userDoc.get("connected") as? List<Map<String, String>> ?: emptyList()
        val connectedCardIds = connectedList.mapNotNull { it["cardId"] }.toSet()

        val queryLimit = if (lastCardId == null) (pageSize * 5).toLong() else pageSize.toLong()

        var query = firestore.collection("public_cards")
            .whereEqualTo("isPublic", true)
            .limit(queryLimit)

        if (lastCardId != null) {
            val lastDoc = firestore.collection("public_cards")
                .document(lastCardId)
                .get()
                .await()

            if (lastDoc.exists()) {
                query = firestore.collection("public_cards")
                    .whereEqualTo("isPublic", true)
                    .orderBy("id")
                    .startAfter(lastDoc)
                    .limit(queryLimit)
            }
        }

        val querySnapshot = query.get().await()

        val filteredDocs = querySnapshot.documents.filter { doc ->
            val userId = doc.getString("userId") ?: ""
            val cardId = doc.id
            userId.isNotEmpty() && userId != currentUserId && !connectedCardIds.contains(cardId)
        }

        val cards = filteredDocs.mapNotNull { doc ->
            try {
                val userId = doc.getString("userId") ?: return@mapNotNull null

                val cardDoc = firestore.collection("users")
                    .document(userId)
                    .collection("cards")
                    .document(doc.id)
                    .get()
                    .await()

                if (cardDoc.exists()) {
                    parseUserCard(cardDoc.id, cardDoc.data)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        val finalCards = if (lastCardId == null) {
            cards.shuffled().take(pageSize)
        } else {
            cards.take(pageSize)
        }

        val lastId = if (cards.isNotEmpty()) cards.last().id else null
        val hasMore = cards.size >= pageSize

        Triple(finalCards, lastId, hasMore)
    }

    override suspend fun getPublicCardById(
        cardId: String
    ): Resource<UserCard?> = safeApiCall {
        val publicDoc = firestore.collection("public_cards")
            .document(cardId)
            .get()
            .await()

        if (!publicDoc.exists()) {
            return@safeApiCall null
        }

        val userId = publicDoc.getString("userId") ?: return@safeApiCall null

        val cardDoc = firestore.collection("users")
            .document(userId)
            .collection("cards")
            .document(cardId)
            .get()
            .await()

        if (cardDoc.exists()) {
            parseUserCard(cardDoc.id, cardDoc.data)
        } else {
            null
        }
    }

    private suspend fun uploadProfileImage(userId: String, imageUri: Uri): String {
        val filename = "profile_${userId}_${System.currentTimeMillis()}.jpg"
        val storageRef = storage.reference.child("user_uploads/$userId/$filename")

        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    private fun parseUserCard(cardId: String, data: Map<String, Any?>?): UserCard? {
        if (data == null) return null

        try {
            val nonNullData = data.mapValues { (_, value) -> value ?: "" }
            return UserCard.fromMap(cardId, nonNullData as Map<String, Any>)
        } catch (e: Exception) {
            return null
        }
    }
}

