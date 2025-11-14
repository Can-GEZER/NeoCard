package com.cangzr.neocard.ui.screens.home.utils

import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.google.firebase.firestore.FirebaseFirestore

fun loadCards(
    userId: String,
    firestore: FirebaseFirestore,
    pageSize: Int,
    lastCardId: String?,
    onSuccess: (List<UserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    var query = firestore.collection("users").document(userId).collection("cards")
        .limit(pageSize.toLong())
    
    if (lastCardId != null) {
        firestore.collection("users").document(userId).collection("cards")
            .document(lastCardId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    query = firestore.collection("users").document(userId).collection("cards")
                        .orderBy("id")
                        .startAfter(documentSnapshot)
                        .limit(pageSize.toLong())
                    
                    executeQuery(query, pageSize, onSuccess, onError)
                } else {
                    onError()
                }
            }
            .addOnFailureListener {
                onError()
            }
    } else {
        executeQuery(query, pageSize, onSuccess, onError)
    }
}

private fun executeQuery(
    query: com.google.firebase.firestore.Query,
    pageSize: Int,
    onSuccess: (List<UserCard>, String?, Boolean) -> Unit,
    onError: () -> Unit
) {
    query.get()
        .addOnSuccessListener { querySnapshot ->
            try {
                val cards = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val card = doc.toObject(UserCard::class.java)?.copy(
                            id = doc.id, 
                            cardType = doc.getString("cardType") ?: "Genel"
                        )

                        val textStylesMap = doc.get("textStyles") as? Map<String, Any>
                        
                        if (card != null && textStylesMap != null) {
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
                            
                            card.copy(textStyles = parsedTextStyles)
                        } else {
                            card // Stil bilgisi yoksa orijinal kartı döndür
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                val lastCardId = if (cards.isNotEmpty()) cards.last().id else null
                val hasMoreCards = cards.size >= pageSize
                
                onSuccess(cards, lastCardId, hasMoreCards)
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
