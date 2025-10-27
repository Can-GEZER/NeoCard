package com.cangzr.neocard.data.repository

import com.cangzr.neocard.data.model.User

/**
 * Kullanıcı işlemleri için repository interface
 */
interface UserRepository {
    
    /**
     * Kullanıcı profil bilgilerini getirir
     * @param userId Kullanıcı ID'si
     * @return User veya null
     */
    suspend fun getUserProfile(
        userId: String
    ): Result<User?>
    
    /**
     * Birden fazla kullanıcının bilgilerini ID'lerine göre getirir
     * @param userIds Kullanıcı ID'leri listesi
     * @return Kullanıcılar listesi
     */
    suspend fun getUsersByIds(
        userIds: List<String>
    ): Result<List<User>>
    
    /**
     * Kullanıcının premium durumunu kontrol eder
     * @param userId Kullanıcı ID'si
     * @return Premium üyelik durumu
     */
    suspend fun isPremiumUser(
        userId: String
    ): Result<Boolean>
    
    /**
     * Kullanıcının bağlantı isteklerini getirir
     * @param userId Kullanıcı ID'si
     * @return Bağlantı istekleri listesi (userId ve cardId içeren map'ler)
     */
    suspend fun getConnectionRequests(
        userId: String
    ): Result<List<Map<String, String>>>
    
    /**
     * Kullanıcının bağlantılarını getirir
     * @param userId Kullanıcı ID'si
     * @return Bağlantılar listesi (userId ve cardId içeren map'ler)
     */
    suspend fun getConnections(
        userId: String
    ): Result<List<Map<String, String>>>
    
    /**
     * Bağlantı isteği gönderir
     * @param fromUserId İstek gönderen kullanıcı ID'si
     * @param toUserId İstek gönderilen kullanıcı ID'si
     * @param cardId Kartvizit ID'si
     */
    suspend fun sendConnectionRequest(
        fromUserId: String,
        toUserId: String,
        cardId: String
    ): Result<Unit>
    
    /**
     * Bağlantı isteğini kabul eder
     * @param currentUserId Mevcut kullanıcı ID'si
     * @param requestUserId İstek gönderen kullanıcı ID'si
     * @param cardId Kartvizit ID'si
     */
    suspend fun acceptConnectionRequest(
        currentUserId: String,
        requestUserId: String,
        cardId: String
    ): Result<Unit>
    
    /**
     * Bağlantı isteğini reddeder
     * @param currentUserId Mevcut kullanıcı ID'si
     * @param requestUserId İstek gönderen kullanıcı ID'si
     * @param cardId Kartvizit ID'si
     */
    suspend fun rejectConnectionRequest(
        currentUserId: String,
        requestUserId: String,
        cardId: String
    ): Result<Unit>
    
    /**
     * Bağlantıyı kaldırır
     * @param currentUserId Mevcut kullanıcı ID'si
     * @param connectedUserId Bağlantı kaldırılacak kullanıcı ID'si
     * @param cardId Kartvizit ID'si
     */
    suspend fun removeConnection(
        currentUserId: String,
        connectedUserId: String,
        cardId: String
    ): Result<Unit>
    
    /**
     * Kullanıcının admin olup olmadığını kontrol eder
     * @param userId Kullanıcı ID'si
     * @return Admin durumu
     */
    suspend fun isAdmin(
        userId: String
    ): Result<Boolean>
    
    /**
     * Promo kodu kullanır
     * @param userId Kullanıcı ID'si
     * @param promoCode Promo kodu
     * @return Başarılı olup olmadığı ve mesaj
     */
    suspend fun redeemPromoCode(
        userId: String,
        promoCode: String
    ): Result<Pair<Boolean, String>>
}

