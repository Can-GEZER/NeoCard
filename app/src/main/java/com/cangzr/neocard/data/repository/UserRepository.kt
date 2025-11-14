package com.cangzr.neocard.data.repository

import com.cangzr.neocard.data.model.User

interface UserRepository {
    
    suspend fun getUserProfile(
        userId: String
    ): Result<User?>
    
    suspend fun getUsersByIds(
        userIds: List<String>
    ): Result<List<User>>
    
    suspend fun isPremiumUser(
        userId: String
    ): Result<Boolean>
    
    suspend fun getConnectionRequests(
        userId: String
    ): Result<List<Map<String, String>>>
    
    suspend fun getConnections(
        userId: String
    ): Result<List<Map<String, String>>>
    
    suspend fun sendConnectionRequest(
        fromUserId: String,
        toUserId: String,
        cardId: String
    ): Result<Unit>
    
    suspend fun acceptConnectionRequest(
        currentUserId: String,
        requestUserId: String,
        cardId: String
    ): Result<Unit>
    
    suspend fun rejectConnectionRequest(
        currentUserId: String,
        requestUserId: String,
        cardId: String
    ): Result<Unit>
    
    suspend fun removeConnection(
        currentUserId: String,
        connectedUserId: String,
        cardId: String
    ): Result<Unit>
    
    suspend fun isAdmin(
        userId: String
    ): Result<Boolean>
    
    suspend fun redeemPromoCode(
        userId: String,
        promoCode: String
    ): Result<Pair<Boolean, String>>
}

