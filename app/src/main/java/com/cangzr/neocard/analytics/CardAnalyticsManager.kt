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

/**
 * Kartvizit istatistiklerini yönetmek için kullanılan sınıf.
 * Firebase Analytics ve Firestore kullanarak kartvizit görüntülenme ve etkileşim verilerini toplar.
 */
class CardAnalyticsManager private constructor() {

    private val firebaseAnalytics = Firebase.analytics
    private val firestore = FirebaseFirestore.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Kartvizit görüntülenme olayını kaydeder.
     * 
     * @param cardId Görüntülenen kartvizit ID'si
     * @param cardOwnerId Kartvizit sahibinin kullanıcı ID'si
     * @param viewerUserId Görüntüleyen kullanıcının ID'si (null ise anonim görüntüleme)
     */
    fun logCardView(cardId: String, cardOwnerId: String, viewerUserId: String?) {
        // Kartvizit sahibi kendi kartvizitini görüntülüyorsa istatistikleri etkileme
        if (viewerUserId != null && viewerUserId == cardOwnerId) {
            println("Kartvizit sahibi kendi kartvizitini görüntüledi, istatistikler etkilenmeyecek.")
            return
        }
        
        // Firebase Analytics için olay kaydı
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card")
            putString("card_owner_id", cardOwnerId)
            putString("viewer_user_id", viewerUserId ?: "anonymous")
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle)

        // Firestore'da görüntülenme sayısını artır
        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                // Transaction ile atomik güncelleme yap
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val views = cardStats.getLong("total_views") ?: 0
                        transaction.update(cardStatsRef, "total_views", views + 1)
                        
                        // Görüntülenme zamanını kaydet
                        val viewsTimestamps = cardStats.get("views_timestamps") as? MutableList<Long> ?: mutableListOf()
                        viewsTimestamps.add(System.currentTimeMillis())
                        transaction.update(cardStatsRef, "views_timestamps", viewsTimestamps)
                        
                        // Görüntüleyen kullanıcıları kaydet (anonim değilse)
                        if (viewerUserId != null) {
                            val uniqueViewers = cardStats.get("unique_viewers") as? MutableList<String> ?: mutableListOf()
                            if (!uniqueViewers.contains(viewerUserId)) {
                                uniqueViewers.add(viewerUserId)
                                transaction.update(cardStatsRef, "unique_viewers", uniqueViewers)
                            }
                        }
                    } else {
                        // Yeni istatistik dokümanı oluştur
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
                // Hata durumunda log kaydı
                println("Kartvizit görüntülenme istatistiği kaydedilemedi: ${e.message}")
            }
        }
    }

    /**
     * Kartvizit bağlantı tıklanma olayını kaydeder.
     * 
     * @param cardId Kartvizit ID'si
     * @param linkType Tıklanan bağlantı tipi (email, phone, website, linkedin, github, vb.)
     * @param viewerUserId Tıklayan kullanıcının ID'si (null ise anonim tıklama)
     */
    fun logLinkClick(cardId: String, linkType: String, viewerUserId: String?) {
        // Kartvizit sahibini kontrol et
        if (viewerUserId != null) {
            // Kartvizit sahibini belirle
            coroutineScope.launch {
                try {
                    val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                    val cardStats = cardStatsRef.get().await()
                    
                    if (cardStats.exists()) {
                        val ownerId = cardStats.getString("owner_id")
                        
                        // Kartvizit sahibi kendi kartvizitindeki linke tıklıyorsa istatistikleri etkileme
                        if (ownerId != null && ownerId == viewerUserId) {
                            println("Kartvizit sahibi kendi kartvizitindeki linke tıkladı, istatistikler etkilenmeyecek.")
                            return@launch
                        }
                    }
                    
                    // Kartvizit sahibi değilse veya sahibi belirlenemezse normal işleme devam et
                    logLinkClickInternal(cardId, linkType, viewerUserId)
                } catch (e: Exception) {
                    // Hata durumunda yine de tıklanmayı kaydet
                    logLinkClickInternal(cardId, linkType, viewerUserId)
                }
            }
        } else {
            // Anonim tıklamalar için direkt kaydet
            logLinkClickInternal(cardId, linkType, viewerUserId)
        }
    }
    
    /**
     * Kartvizit bağlantı tıklanma olayını kaydeden iç fonksiyon.
     */
    private fun logLinkClickInternal(cardId: String, linkType: String, viewerUserId: String?) {
        // Firebase Analytics için olay kaydı
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card_link")
            putString("link_type", linkType)
            putString("viewer_user_id", viewerUserId ?: "anonymous")
        }
        firebaseAnalytics.logEvent("card_link_click", bundle)

        // Firestore'da tıklanma sayısını artır
        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val linkClicks = cardStats.getLong("link_clicks") ?: 0
                        transaction.update(cardStatsRef, "link_clicks", linkClicks + 1)
                        
                        // Link tipine göre tıklanma sayısını kaydet
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

    /**
     * QR kod tarama olayını kaydeder.
     * 
     * @param cardId Taranan kartvizit ID'si
     * @param scannerUserId Tarayan kullanıcının ID'si (null ise anonim tarama)
     */
    fun logQrScan(cardId: String, scannerUserId: String? = null) {
        // Kartvizit sahibini kontrol et
        if (scannerUserId != null) {
            // Kartvizit sahibini belirle
            coroutineScope.launch {
                try {
                    val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                    val cardStats = cardStatsRef.get().await()
                    
                    if (cardStats.exists()) {
                        val ownerId = cardStats.getString("owner_id")
                        
                        // Kartvizit sahibi kendi kartvizitinin QR kodunu tarıyorsa istatistikleri etkileme
                        if (ownerId != null && ownerId == scannerUserId) {
                            println("Kartvizit sahibi kendi kartvizitinin QR kodunu taradı, istatistikler etkilenmeyecek.")
                            return@launch
                        }
                    }
                    
                    // Kartvizit sahibi değilse veya sahibi belirlenemezse normal işleme devam et
                    logQrScanInternal(cardId, scannerUserId)
                } catch (e: Exception) {
                    // Hata durumunda yine de taramayı kaydet
                    logQrScanInternal(cardId, scannerUserId)
                }
            }
        } else {
            // Anonim taramalar için direkt kaydet
            logQrScanInternal(cardId, scannerUserId)
        }
    }
    
    /**
     * QR kod tarama olayını kaydeden iç fonksiyon.
     */
    private fun logQrScanInternal(cardId: String, scannerUserId: String? = null) {
        // Firebase Analytics için olay kaydı
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card_qr")
            if (scannerUserId != null) {
                putString("scanner_user_id", scannerUserId)
            }
        }
        firebaseAnalytics.logEvent("card_qr_scan", bundle)

        // Firestore'da QR tarama sayısını artır
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

    /**
     * Kartvizit paylaşım olayını kaydeder.
     * 
     * @param cardId Paylaşılan kartvizit ID'si
     * @param shareMethod Paylaşım metodu (qr, link, image, nfc, vb.)
     * @param sharerUserId Paylaşan kullanıcının ID'si
     */
    fun logCardShare(cardId: String, shareMethod: String, sharerUserId: String) {
        // Kartvizit sahibini kontrol et
        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                val cardStats = cardStatsRef.get().await()
                
                if (cardStats.exists()) {
                    val ownerId = cardStats.getString("owner_id")
                    
                    // Kartvizit sahibi kendi kartvizitini paylaşıyorsa istatistikleri etkileme
                    // Ancak bu durumda bile paylaşımı Firebase Analytics'e kaydet (ama Firestore'a kaydetme)
                    if (ownerId != null && ownerId == sharerUserId) {
                        println("Kartvizit sahibi kendi kartvizitini paylaştı, istatistikler etkilenmeyecek.")
                        
                        // Sadece Analytics'e kaydet
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
                
                // Kartvizit sahibi değilse veya sahibi belirlenemezse normal işleme devam et
                logCardShareInternal(cardId, shareMethod, sharerUserId)
            } catch (e: Exception) {
                // Hata durumunda yine de paylaşımı kaydet
                logCardShareInternal(cardId, shareMethod, sharerUserId)
            }
        }
    }
    
    /**
     * Kartvizit paylaşım olayını kaydeden iç fonksiyon.
     */
    private fun logCardShareInternal(cardId: String, shareMethod: String, sharerUserId: String) {
        // Firebase Analytics için olay kaydı
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_ID, cardId)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "card")
            putString("share_method", shareMethod)
            putString("sharer_user_id", sharerUserId)
            putBoolean("is_owner_sharing", false)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)

        // Firestore'da paylaşım sayısını artır
        coroutineScope.launch {
            try {
                val cardStatsRef = firestore.collection("card_statistics").document(cardId)
                
                firestore.runTransaction { transaction ->
                    val cardStats = transaction.get(cardStatsRef)
                    
                    if (cardStats.exists()) {
                        val shares = cardStats.getLong("shares") ?: 0
                        transaction.update(cardStatsRef, "shares", shares + 1)
                        
                        // Paylaşım metoduna göre sayıyı kaydet
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

    /**
     * Belirli bir kartvizit için istatistikleri getirir.
     * 
     * @param cardId Kartvizit ID'si
     * @param onSuccess Başarı durumunda çağrılacak callback
     * @param onError Hata durumunda çağrılacak callback
     */
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
                    
                    // Son 7 gün için görüntülenme sayıları
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
                    // Eğer istatistik henüz oluşturulmamışsa boş bir istatistik nesnesi döndür
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

    /**
     * Zaman damgalarından son 7 günlük görüntülenme sayılarını hesaplar.
     * 
     * @param timestamps Görüntülenme zaman damgaları listesi
     * @return Son 7 günün her biri için görüntülenme sayılarını içeren liste
     */
    private fun calculateWeeklyViews(timestamps: List<Long>): List<Long> {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        // Son 7 gün için görüntülenme sayıları (0. indeks: bugün, 6. indeks: 6 gün önce)
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

/**
 * Kartvizit istatistiklerini temsil eden veri sınıfı.
 */
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
