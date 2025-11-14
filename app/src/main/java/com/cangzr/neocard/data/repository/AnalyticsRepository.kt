package com.cangzr.neocard.data.repository

import com.cangzr.neocard.analytics.CardStatistics

interface AnalyticsRepository {
    
    suspend fun logCardView(
        cardId: String,
        cardOwnerId: String,
        viewerUserId: String?
    ): Result<Unit>
    
    suspend fun logLinkClick(
        cardId: String,
        cardOwnerId: String,
        linkType: String,
        viewerUserId: String?
    ): Result<Unit>
    
    suspend fun logQRScan(
        cardId: String,
        cardOwnerId: String,
        viewerUserId: String?
    ): Result<Unit>
    
    suspend fun logCardShare(
        cardId: String,
        cardOwnerId: String,
        shareMethod: String,
        userId: String?
    ): Result<Unit>
    
    suspend fun logConnectionAdd(
        cardId: String,
        cardOwnerId: String,
        connectorUserId: String
    ): Result<Unit>
    
    suspend fun getCardStatistics(
        cardId: String
    ): Result<CardStatistics>
    
    suspend fun getUserTotalStatistics(
        userId: String
    ): Result<Map<String, Long>>
}

