package com.cangzr.neocard.ui.screens.home.utils

import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.google.firebase.firestore.FirebaseFirestore

fun loadExploreCards(
    firestore: FirebaseFirestore,
    currentUserId: String?,
    pageSize: Int,
    lastCardId: String?,
    onSuccess: (List<UserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    if (currentUserId == null) {
        onError()
        return
    }
    
    firestore.collection("users").document(currentUserId).get()
        .addOnSuccessListener { userDoc ->
            val connectedList = userDoc.get("connected") as? List<Map<String, String>> ?: emptyList()
            val connectedCardIds = connectedList.mapNotNull { it["cardId"] }.toSet()
            
            val queryLimit = if (lastCardId == null) (pageSize * 5).toLong() else pageSize.toLong()
            
            var query = firestore.collection("public_cards")
                .whereEqualTo("isPublic", true)
                .limit(queryLimit)
            
            if (lastCardId != null) {
                firestore.collection("public_cards")
                    .document(lastCardId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        if (documentSnapshot.exists()) {
                            query = firestore.collection("public_cards")
                                .whereEqualTo("isPublic", true)
                                .orderBy("id")
                                .startAfter(documentSnapshot)
                                .limit(queryLimit)
                            
                            executeExploreQuery(query, pageSize, currentUserId, connectedCardIds, lastCardId != null, onSuccess, onError)
                        } else {
                            onError()
                        }
                    }
                    .addOnFailureListener {
                        onError()
                    }
            } else {
                executeExploreQuery(query, pageSize, currentUserId, connectedCardIds, false, onSuccess, onError)
            }
        }
        .addOnFailureListener {
            onError()
        }
}

private fun executeExploreQuery(
    query: com.google.firebase.firestore.Query,
    pageSize: Int,
    currentUserId: String?,
    connectedCardIds: Set<String>,
    isPaginating: Boolean,
    onSuccess: (List<UserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    query.get()
        .addOnSuccessListener { querySnapshot ->
            try {
                val tasks = mutableListOf<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>>()
                val publicDocs = mutableListOf<com.google.firebase.firestore.DocumentSnapshot>()
                querySnapshot.documents.forEach { doc ->
                    val userId = doc.getString("userId") ?: ""
                    val cardId = doc.id
                    if (userId.isNotEmpty() && userId != currentUserId && !connectedCardIds.contains(cardId)) {
                        publicDocs.add(doc)
                        tasks.add(
                            FirebaseFirestore.getInstance()
                                .collection("users").document(userId)
                                .collection("cards").document(cardId).get()
                        )
                    }
                }

                com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        val resultCards = mutableListOf<UserCard>()
                        tasks.forEach { task ->
                            try {
                                val snapshot = task.result
                                if (snapshot != null && snapshot.exists()) {
                                    val base = snapshot.toObject(UserCard::class.java)?.copy(
                                        id = snapshot.id,
                                        cardType = snapshot.getString("cardType") ?: "Genel"
                                    )
                                    val textStylesMap = snapshot.get("textStyles") as? Map<String, Any>
                                    val parsed = if (base != null && textStylesMap != null) {
                                        val parsedTextStyles = mutableMapOf<String, TextStyleDTO>()
                                        textStylesMap.forEach { (key, value) ->
                                            val styleMap = value as? Map<String, Any>
                                            if (styleMap != null) {
                                                val textStyle = TextStyleDTO(
                                                    isBold = styleMap["isBold"] as? Boolean ?: false,
                                                    isItalic = styleMap["isItalic"] as? Boolean ?: false,
                                                    isUnderlined = styleMap["isUnderlined"] as? Boolean ?: false,
                                                    fontSize = (styleMap["fontSize"] as? Number)?.toFloat() ?: 16f,
                                                    color = styleMap["color"] as? String ?: "#000000"
                                                )
                                                parsedTextStyles[key] = textStyle
                                            }
                                        }
                                        base.copy(textStyles = parsedTextStyles)
                                    } else base

                                    parsed?.let { resultCards.add(it) }
                                }
                            } catch (_: Exception) {}
                        }

                        val finalCards = if (isPaginating) {
                            resultCards
                        } else {
                            resultCards.shuffled()
                        }
                        
                        val limitedCards = finalCards.take(pageSize)
                        
                        val lastOriginalCardId = if (resultCards.isNotEmpty()) resultCards.last().id else null
                        val hasMoreCards = resultCards.size >= pageSize
                        onSuccess(limitedCards, lastOriginalCardId, hasMoreCards)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
        .addOnFailureListener {
            it.printStackTrace()
            onError()
        }
}
