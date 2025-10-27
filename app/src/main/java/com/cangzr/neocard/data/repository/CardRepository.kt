package com.cangzr.neocard.data.repository

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard

/**
 * Kartvizit işlemleri için repository interface
 */
interface CardRepository {
    
    /**
     * Kullanıcının kartlarını sayfalı olarak getirir
     * @param userId Kullanıcı ID'si
     * @param pageSize Sayfa başına kart sayısı
     * @param lastCardId Son kartın ID'si (pagination için)
     * @return Triple<Kartlar, Son kart ID'si, Daha fazla kart var mı>
     */
    suspend fun getCards(
        userId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>>
    
    /**
     * Belirli bir kartı ID'sine göre getirir
     * @param userId Kullanıcı ID'si
     * @param cardId Kart ID'si
     * @return UserCard veya null
     */
    suspend fun getCardById(
        userId: String,
        cardId: String
    ): Resource<UserCard?>
    
    /**
     * Yeni bir kart kaydeder
     * @param userId Kullanıcı ID'si
     * @param card Kaydedilecek kart
     * @param imageUri Profil resmi URI'si (opsiyonel)
     * @return Kaydedilen kartın ID'si
     */
    suspend fun saveCard(
        userId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<String>
    
    /**
     * Mevcut bir kartı günceller
     * @param userId Kullanıcı ID'si
     * @param cardId Kart ID'si
     * @param card Güncellenmiş kart
     * @param imageUri Yeni profil resmi URI'si (opsiyonel)
     */
    suspend fun updateCard(
        userId: String,
        cardId: String,
        card: UserCard,
        imageUri: Uri?
    ): Resource<Unit>
    
    /**
     * Bir kartı siler
     * @param userId Kullanıcı ID'si
     * @param cardId Kart ID'si
     * @param profileImageUrl Profil resmi URL'si (storage'dan silinmesi için)
     */
    suspend fun deleteCard(
        userId: String,
        cardId: String,
        profileImageUrl: String
    ): Resource<Unit>
    
    /**
     * Keşif kartlarını getirir (kullanıcının bağlantılarında olmayanlar)
     * @param currentUserId Mevcut kullanıcı ID'si
     * @param pageSize Sayfa başına kart sayısı
     * @param lastCardId Son kartın ID'si (pagination için)
     * @return Triple<Kartlar, Son kart ID'si, Daha fazla kart var mı>
     */
    suspend fun getExploreCards(
        currentUserId: String,
        pageSize: Int,
        lastCardId: String?
    ): Resource<Triple<List<UserCard>, String?, Boolean>>
    
    /**
     * Public kartı ID'sine göre getirir
     * @param cardId Public kart ID'si
     * @return UserCard veya null
     */
    suspend fun getPublicCardById(
        cardId: String
    ): Resource<UserCard?>
}

