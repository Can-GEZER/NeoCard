package com.cangzr.neocard.data.repository

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard

interface CardRepository {
    
    suspend fun getCards(
        userId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>>
    
    suspend fun getCardById(
        userId: String,
        cardId: String
    ): Resource<UserCard?>
    
    suspend fun saveCard(
        userId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<String>
    
    suspend fun updateCard(
        userId: String,
        cardId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<Unit>
    
    suspend fun deleteCard(
        userId: String,
        cardId: String,
        profileImageUrl: String
    ): Resource<Unit>
    
    suspend fun getExploreCards(
        currentUserId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>>
    
    suspend fun getPublicCardById(
        cardId: String
    ): Resource<UserCard?>
}

