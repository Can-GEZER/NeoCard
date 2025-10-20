package com.cangzr.neocard.ui.screens.home.utils

import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.google.firebase.firestore.FirebaseFirestore

// Kartları sayfalı olarak yükleyen fonksiyon
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
    
    // Eğer son kart ID'si varsa, o karttan sonrasını getir
    if (lastCardId != null) {
        // Önce son kartın referansını al
        firestore.collection("users").document(userId).collection("cards")
            .document(lastCardId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Son karttan sonraki kartları getir
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
        // İlk sayfayı getir
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
                        // Temel UserCard nesnesini oluştur
                        val card = doc.toObject(UserCard::class.java)?.copy(
                            id = doc.id, 
                            cardType = doc.getString("cardType") ?: "Genel"
                        )

                        // textStyles alanını manuel olarak parse et
                        val textStylesMap = doc.get("textStyles") as? Map<String, Any>
                        
                        if (card != null && textStylesMap != null) {
                            // Her bir text stili için TextStyleDTO oluştur
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
                            
                            // Doğru yapılandırılmış textStyles ile yeni bir UserCard döndür
                            card.copy(textStyles = parsedTextStyles)
                        } else {
                            card // Stil bilgisi yoksa orijinal kartı döndür
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                
                // Son kartın ID'sini ve daha fazla kart olup olmadığını belirle
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
