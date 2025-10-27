package com.cangzr.neocard.data.repository

import com.cangzr.neocard.analytics.CardStatistics

/**
 * Analitik işlemleri için repository interface
 */
interface AnalyticsRepository {
    
    /**
     * Kartvizit görüntülenme olayını kaydeder
     * @param cardId Kart ID'si
     * @param cardOwnerId Kart sahibinin ID'si
     * @param viewerUserId Görüntüleyen kullanıcının ID'si (null ise anonim)
     */
    suspend fun logCardView(
        cardId: String,
        cardOwnerId: String,
        viewerUserId: String?
    ): Result<Unit>
    
    /**
     * Link tıklama olayını kaydeder
     * @param cardId Kart ID'si
     * @param cardOwnerId Kart sahibinin ID'si
     * @param linkType Link tipi (email, phone, website, vs.)
     * @param viewerUserId Tıklayan kullanıcının ID'si (null ise anonim)
     */
    suspend fun logLinkClick(
        cardId: String,
        cardOwnerId: String,
        linkType: String,
        viewerUserId: String?
    ): Result<Unit>
    
    /**
     * QR kod tarama olayını kaydeder
     * @param cardId Kart ID'si
     * @param cardOwnerId Kart sahibinin ID'si
     * @param viewerUserId Tarayan kullanıcının ID'si (null ise anonim)
     */
    suspend fun logQRScan(
        cardId: String,
        cardOwnerId: String,
        viewerUserId: String?
    ): Result<Unit>
    
    /**
     * Kart paylaşma olayını kaydeder
     * @param cardId Kart ID'si
     * @param cardOwnerId Kart sahibinin ID'si
     * @param shareMethod Paylaşma yöntemi (whatsapp, email, vs.)
     * @param userId Paylaşan kullanıcının ID'si
     */
    suspend fun logCardShare(
        cardId: String,
        cardOwnerId: String,
        shareMethod: String,
        userId: String?
    ): Result<Unit>
    
    /**
     * Bağlantı ekleme olayını kaydeder
     * @param cardId Kart ID'si
     * @param cardOwnerId Kart sahibinin ID'si
     * @param connectorUserId Bağlantı ekleyen kullanıcının ID'si
     */
    suspend fun logConnectionAdd(
        cardId: String,
        cardOwnerId: String,
        connectorUserId: String
    ): Result<Unit>
    
    /**
     * Belirli bir kart için istatistikleri getirir
     * @param cardId Kart ID'si
     * @return CardStatistics veya hata
     */
    suspend fun getCardStatistics(
        cardId: String
    ): Result<CardStatistics>
    
    /**
     * Kullanıcının tüm kartları için toplam istatistikleri getirir
     * @param userId Kullanıcı ID'si
     * @return Toplam görüntülenme, tıklama, vs. sayıları
     */
    suspend fun getUserTotalStatistics(
        userId: String
    ): Result<Map<String, Long>>
}

