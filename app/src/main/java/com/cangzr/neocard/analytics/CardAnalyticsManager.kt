package com.cangzr.neocard.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CardAnalyticsManager private constructor() {

    private val firebaseAnalytics = Firebase.analytics
    private val firestore = FirebaseFirestore.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    fun logCardView(cardId: String, cardOwnerId: String, viewerUserId: String?) {
        if (viewerUserId != null && viewerUserId == cardOwnerId) {
            println("Kartvizit sahibi kendi kartvizitini görüntüledi, istatistikler etkilenmeyecek.")
            return
        }
        
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card")
            putString("card_owner_id", cardOwnerId)
            putString("viewer_user_id", viewerUserId ?: "anonymous")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)

        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val views = cardStats.getLong("total_views") ?: 0
                        transaction.update(cardStatsRef, "total_views", views + 1)
                        
                        val viewsTimestamps = cardStats.get("views_timestamps") as? MutableList<Long> ?: mutableListOf()
                        viewsTimestamps.add(System.currentTimeMillis())
                        transaction.update(cardStatsRef, "views_timestamps", viewsTimestamps)
                        
                        if (viewerUserId != null) {
                            val uniqueViewers = cardStats.get("unique_viewers") as? MutableList<String> ?: mutableListOf()
                            if (!uniqueViewers.contains(viewerUserId)) {
                                uniqueViewers.add(viewerUserId)
                                transaction.update(cardStatsRef, "unique_viewers", uniqueViewers)
                            }
                        }
                    } else {
                        val statsData = hashMapOf(
                            "card_id" to cardId,
                            "owner_id" to cardOwnerId,
                            "total_views" to 1L,
                            "unique_viewers" to if (viewerUserId != null) listOf(viewerUserId) else listOf<String>(),
                            "views_timestamps" to listOf(System.currentTimeMillis()),
                            "link_clicks" to 0L,
                            "qr_scans" to 0L,
                            "shares" to 0L,
                            "created_at" to System.currentTimeMillis()
                        )
                        transaction.set(cardStatsRef, statsData)
                    }
                }.await()
            } catch (e: Exception) {
                println("Kartvizit görüntülenme istatistiği kaydedilemedi: ${e.message}")
            }
        }
    }

    fun logLinkClick(cardId: String, linkType: String, viewerUserId: String?) {
        if (viewerUserId != null) {
            coroutineScope.launch {
                try {
                    val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                    val cardStats = cardStatsRef.get().await()
                    
                    if (cardStats.exists()) {
                        val ownerId = cardStats.getString("owner_id")
                        
                        if (ownerId != null && ownerId == viewerUserId) {
                            println("Kartvizit sahibi kendi kartvizitindeki linke tıkladı, istatistikler etkilenmeyecek.")
                            return@launch
                        }
                    }
                    
                    logLinkClickInternal(cardId, linkType, viewerUserId)
                } catch (e: Exception) {
                    logLinkClickInternal(cardId, linkType, viewerUserId)
                }
            }
        } else {
            logLinkClickInternal(cardId, linkType, viewerUserId)
        }
    }
    
    private fun logLinkClickInternal(cardId: String, linkType: String, viewerUserId: String?) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card_link")
            putString("link_type", linkType)
            putString("viewer_user_id", viewerUserId ?: "anonymous")
        }
        firebaseAnalytics.logEvent("card_link_click", bundle)

        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val linkClicks = cardStats.getLong("link_clicks") ?: 0
                        transaction.update(cardStatsRef, "link_clicks", linkClicks + 1)
                        
                        val linkClicksMap = cardStats.get("link_clicks_by_type") as? MutableMap<String, Long> ?: mutableMapOf()
                        val currentCount = linkClicksMap[linkType] ?: 0L
                        linkClicksMap[linkType] = currentCount + 1
                        transaction.update(cardStatsRef, "link_clicks_by_type", linkClicksMap)
                    }
                }.await()
            } catch (e: Exception) {
                println("Kartvizit link tıklanma istatistiği kaydedilemedi: ${e.message}")
            }
        }
    }

    fun logQrScan(cardId: String, scannerUserId: String? = null) {
        if (scannerUserId != null) {
            coroutineScope.launch {
                try {
                    val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                    val cardStats = cardStatsRef.get().await()
                    
                    if (cardStats.exists()) {
                        val ownerId = cardStats.getString("owner_id")
                        
                        if (ownerId != null && ownerId == scannerUserId) {
                            println("Kartvizit sahibi kendi kartvizitinin QR kodunu taradı, istatistikler etkilenmeyecek.")
                            return@launch
                        }
                    }
                    
                    logQrScanInternal(cardId, scannerUserId)
                } catch (e: Exception) {
                    logQrScanInternal(cardId, scannerUserId)
                }
            }
        } else {
            logQrScanInternal(cardId, scannerUserId)
        }
    }
    
    private fun logQrScanInternal(cardId: String, scannerUserId: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card_qr")
            if (scannerUserId != null) {
                putString("scanner_user_id", scannerUserId)
            }
        }
        firebaseAnalytics.logEvent("card_qr_scan", bundle)

        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val qrScans = cardStats.getLong("qr_scans") ?: 0
                        transaction.update(cardStatsRef, "qr_scans", qrScans + 1)
                    }
                }.await()
            } catch (e: Exception) {
                println("Kartvizit QR tarama istatistiği kaydedilemedi: ${e.message}")
            }
        }
    }

    fun logCardShare(cardId: String, shareMethod: String, sharerUserId: String) {
        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                val cardStats = cardStatsRef.get().await()
                
                if (cardStats.exists()) {
                    val ownerId = cardStats.getString("owner_id")
                    
                    if (ownerId != null && ownerId == sharerUserId) {
                        println("Kartvizit sahibi kendi kartvizitini paylaştı, istatistikler etkilenmeyecek.")
                        
                        val bundle = Bundle().apply {
                            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
                            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card")
                            putString("share_method", shareMethod)
                            putString("sharer_user_id", sharerUserId)
                            putBoolean("is_owner_sharing", true)
                        }
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)
                        
                        return@launch
                    }
                }
                
                logCardShareInternal(cardId, shareMethod, sharerUserId)
            } catch (e: Exception) {
                logCardShareInternal(cardId, shareMethod, sharerUserId)
            }
        }
    }
    
    private fun logCardShareInternal(cardId: String, shareMethod: String, sharerUserId: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card")
            putString("share_method", shareMethod)
            putString("sharer_user_id", sharerUserId)
            putBoolean("is_owner_sharing", false)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)

        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val shares = cardStats.getLong("shares") ?: 0
                        transaction.update(cardStatsRef, "shares", shares + 1)
                        
                        val sharesByMethod = cardStats.get("shares_by_method") as? MutableMap<String, Long> ?: mutableMapOf()
                        val currentCount = sharesByMethod[shareMethod] ?: 0L
                        sharesByMethod[shareMethod] = currentCount + 1
                        transaction.update(cardStatsRef, "shares_by_method", sharesByMethod)
                    }
                }.await()
            } catch (e: Exception) {
                println("Kartvizit paylaşım istatistiği kaydedilemedi: ${e.message}")
            }
        }
    }

    fun getCardStatistics(
        cardId: String, 
        onSuccess: (CardStatistics) -> Unit, 
        onError: (Exception) -> Unit
    ) {
        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                val document = cardStatsRef.get().await()
                
                if (document.exists()) {
                    val totalViews = document.getLong("total_views") ?: 0
                    val uniqueViewers = (document.get("unique_viewers") as? List<*>)?.size ?: 0
                    val linkClicks = document.getLong("link_clicks") ?: 0
                    val qrScans = document.getLong("qr_scans") ?: 0
                    val shares = document.getLong("shares") ?: 0
                    
                    val linkClicksByType = document.get("link_clicks_by_type") as? Map<String, Long> ?: mapOf()
                    val sharesByMethod = document.get("shares_by_method") as? Map<String, Long> ?: mapOf()
                    
                    val viewsTimestamps = document.get("views_timestamps") as? List<Long> ?: listOf()
                    val weeklyViews = calculateWeeklyViews(viewsTimestamps)
                    
                    val cardStatistics = CardStatistics(
                        cardId = cardId,
                        totalViews = totalViews,
                        uniqueViewers = uniqueViewers,
                        linkClicks = linkClicks,
                        qrScans = qrScans,
                        shares = shares,
                        linkClicksByType = linkClicksByType,
                        sharesByMethod = sharesByMethod,
                        weeklyViews = weeklyViews
                    )
                    
                    withContext(Dispatchers.Main) {
                        onSuccess(cardStatistics)
                    }
                } else {
                    val emptyStats = CardStatistics(
                        cardId = cardId,
                        totalViews = 0,
                        uniqueViewers = 0,
                        linkClicks = 0,
                        qrScans = 0,
                        shares = 0,
                        linkClicksByType = mapOf(),
                        sharesByMethod = mapOf(),
                        weeklyViews = List(7) { 0L }
                    )
                    
                    withContext(Dispatchers.Main) {
                        onSuccess(emptyStats)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    private fun calculateWeeklyViews(timestamps: List<Long>): List<Long> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        val weeklyViews = MutableList(7) { 0L }
        
        timestamps.forEach { timestamp ->
            val daysAgo = ((now - timestamp) / oneDay).toInt()
            if (daysAgo in 0..6) {
                weeklyViews[daysAgo]++
            }
        }
        
        return weeklyViews
    }

    companion object {
        @Volatile
        private var instance: CardAnalyticsManager? = null
        
        fun getInstance(): CardAnalyticsManager {
            return instance ?: synchronized(this) {
                instance ?: CardAnalyticsManager().also { instance = it }
            }
        }
    }
}

data class CardStatistics(
    val cardId: String,
    val totalViews: Long,
    val uniqueViewers: Int,
    val linkClicks: Long,
    val qrScans: Long,
    val shares: Long,
    val linkClicksByType: Map<String, Long>,
    val sharesByMethod: Map<String, Long>,
    val weeklyViews: List<Long> // Son 7 günün her biri için görüntülenme sayıları
)
