package com.cangzr.neocard.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive

class BillingManager private constructor(private val context: Context) {

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var premiumCheckJob: Job? = null
    private var connectionRetryCount = 0
    private val MAX_RETRY_COUNT = 5

    private var purchaseQueryRetryCount = 0
    private val MAX_PURCHASE_QUERY_RETRY = 3

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Kullanıcı satın alma işlemini iptal etti - sadece Firestore'dan kontrol et
            checkFirestorePremiumStatus()
        }
        // Diğer hata durumlarında hiçbir şey yapma, döngüsel çağrıları önle
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        startBillingConnection()
        checkPremiumStatus()
        startPeriodicPremiumCheck()
    }

    // Periyodik premium durumu kontrolü başlat
    private fun startPeriodicPremiumCheck() {
        premiumCheckJob?.cancel()
        premiumCheckJob = coroutineScope.launch {
            while (isActive) {
                delay(1800000) // Her 30 dakikada bir kontrol et (daha düşük sıklık)
                if (isActive) {
                    // Önce BillingClient üzerinden kontrol et
                    if (billingClient.isReady) {
                        queryPurchases() 
                    } else {
                        // Bağlantı yoksa sadece Firestore'dan kontrol et
                        checkFirestorePremiumStatus()
                    }
                }
            }
        }
    }

    private fun startBillingConnection() {
        // Eğer zaten bağlıysa, tekrar bağlanmaya çalışma
        if (billingClient.isReady) return
        
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Bağlantı başarılı, retry sayacını sıfırla
                    connectionRetryCount = 0
                    queryPurchases()
                } else {
                    // Bağlantı hatası, yeniden dene
                    retryBillingConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Bağlantı koptuğunda yeniden bağlanmayı dene
                retryBillingConnection()
            }
        })
    }
    
    private fun retryBillingConnection() {
        connectionRetryCount++
        if (connectionRetryCount <= MAX_RETRY_COUNT) {
            coroutineScope.launch {
                // Her denemede bekleme süresini arttır (exponential backoff)
                val delayTime = (1000L * connectionRetryCount * connectionRetryCount).coerceAtMost(30000L)
                delay(delayTime)
                withContext(Dispatchers.Main) {
                    startBillingConnection()
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        // Satın alma başarılı, premium durumunu güncelle
                        updatePremiumStatus(true)
                    }
                    // Hata durumunda hiçbir şey yapma, döngüsel çağrıları önle
                }
            } else {
                updatePremiumStatus(true)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            // Bekleyen satın alma - 30 saniye sonra tekrar kontrol et
            coroutineScope.launch {
                delay(30000)
                if (billingClient.isReady) {
                    queryPurchases()
                }
            }
        }
        // Diğer durumlarda hiçbir şey yapma
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            return // Bağlantı yoksa çıkış yap, döngüsel çağrıları önle
        }
        
        try {
            val queryPurchasesParams = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(queryPurchasesParams) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Sorgu başarılı, retry sayacını sıfırla
                    purchaseQueryRetryCount = 0
                    
                    var hasPremiumPurchase = false
                    for (purchase in purchases) {
                        if (purchase.products.contains("premium_subscription") && 
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            handlePurchase(purchase)
                            hasPremiumPurchase = true
                        }
                    }
                    
                    // Aktif premium satın alma yoksa, Firestore'dan kontrol et
                    if (!hasPremiumPurchase) {
                        checkFirestorePremiumStatus()
                    }
                } else if (billingResult.responseCode != BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                    // Sadece SERVICE_UNAVAILABLE değilse retry et
                    retryQueryPurchases()
                }
                // Diğer hata durumlarında hiçbir şey yapma
            }
        } catch (e: Exception) {
            // Exception'da hiçbir şey yapma, döngüsel çağrıları önle
        }
    }
    
    private fun retryQueryPurchases() {
        purchaseQueryRetryCount++
        if (purchaseQueryRetryCount <= MAX_PURCHASE_QUERY_RETRY) {
            coroutineScope.launch {
                // Her denemede bekleme süresini arttır (exponential backoff)
                delay(2000L * purchaseQueryRetryCount)
                if (billingClient.isReady) {
                    queryPurchases()
                }
            }
        } else {
            // Maksimum deneme sayısına ulaşıldı, sadece sayacı sıfırla
            purchaseQueryRetryCount = 0
        }
    }

    fun launchBillingFlow(activity: Activity) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_subscription")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken

                if (offerToken != null) {
                    val billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(productDetails)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        )
                        .build()

                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
            }
            // Ürün detayları alınamazsa hiçbir şey yapma
        }
    }

    private fun updatePremiumStatus(isPremium: Boolean, endTime: Long = 0) {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val updateData = if (isPremium && endTime > 0) {
                mapOf(
                    "premium" to isPremium,
                    "premiumEndTime" to endTime
                )
            } else {
                mapOf("premium" to isPremium)
            }

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update(updateData)
                .addOnSuccessListener {
                    coroutineScope.launch {
                        _isPremium.emit(isPremium)
                    }
                }
                .addOnFailureListener {
                    // Hata durumunda önceki durumu koru, döngüsel çağrıları önle
                }
        }
    }

    // Sadece Firestore'dan premium durumunu kontrol et
    private fun checkFirestorePremiumStatus() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val isPremium = document.getBoolean("premium") ?: false
                    val premiumEndTime = document.getLong("premiumEndTime") ?: 0L
                    
                    // Promosyon kodundan alınan süreli premium üyeliği kontrol et
                    if (isPremium && premiumEndTime > 0) {
                        if (System.currentTimeMillis() >= premiumEndTime) {
                            // Premium süresi dolmuş, üyeliği sonlandır
                            updatePremiumStatus(false)
                            return@addOnSuccessListener
                        }
                    }
                    
                    coroutineScope.launch {
                        if (_isPremium.value != isPremium) {
                            _isPremium.emit(isPremium)
                        }
                    }
                }
                .addOnFailureListener {
                    // Firestore erişimi başarısız, önceki durumu koru
                }
        } ?: run {
            // Kullanıcı giriş yapmamış - premium değil
            coroutineScope.launch {
                _isPremium.emit(false)
            }
        }
    }

    fun checkPremiumStatus() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            // Önce billingClient üzerinden kontrol et
            if (billingClient.isReady) {
                queryPurchases()
            } else {
                // BillingClient hazır değilse sadece Firestore'dan kontrol et
                checkFirestorePremiumStatus()
            }
        } ?: run {
            // Kullanıcı giriş yapmamış - premium değil
            coroutineScope.launch {
                _isPremium.emit(false)
            }
        }
    }
    
    // Promosyon kodundan premium üyelik verdikten sonra sona erme tarihini yönetmek için
    fun setPremiumWithPromoCode(userId: String, duration: Long): Boolean {
        var result = false
        try {
            val endTime = System.currentTimeMillis() + duration
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(
                    mapOf(
                        "premium" to true,
                        "premiumEndTime" to endTime
                    )
                )
                .addOnSuccessListener {
                    if (FirebaseAuth.getInstance().currentUser?.uid == userId) {
                        coroutineScope.launch {
                            _isPremium.emit(true)
                        }
                    }
                    result = true
                }
                .addOnFailureListener {
                    result = false
                }
        } catch (e: Exception) {
            result = false
        }
        return result
    }

    // Mevcut premium süresine yeni süre ekleme (promosyon kodu için)
    fun extendPremiumWithPromoCode(userId: String, additionalDuration: Long): Boolean {
        var result = false
        try {
            // Önce mevcut premium durumunu kontrol et
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val currentEndTime = document.getLong("premiumEndTime") ?: 0L
                    val currentTime = System.currentTimeMillis()
                    
                    // Eğer premium süresi geçmişse, şu andan itibaren başlat
                    // Eğer hala aktifse, mevcut bitiş zamanına ekle
                    val newEndTime = if (currentEndTime > currentTime) {
                        currentEndTime + additionalDuration
                    } else {
                        currentTime + additionalDuration
                    }
                    
                    // Güncelle
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .update(
                            mapOf(
                                "premium" to true,
                                "premiumEndTime" to newEndTime
                            )
                        )
                        .addOnSuccessListener {
                            if (FirebaseAuth.getInstance().currentUser?.uid == userId) {
                                coroutineScope.launch {
                                    _isPremium.emit(true)
                                }
                            }
                            result = true
                        }
                        .addOnFailureListener {
                            result = false
                        }
                }
                .addOnFailureListener {
                    result = false
                }
        } catch (e: Exception) {
            result = false
        }
        return result
    }

    // UIController sınıfı için manuel tetikleme metodu
    fun refreshPremiumStatus() {
        checkPremiumStatus()
    }

    // Aktivitelerin onResume metodunda çağrılabilecek premium durum kontrolü
    fun checkPremiumOnResume() {
        checkPremiumStatus()
    }

    // Sınıfı temizleme metodu (Application/Activity kapanırken çağrılabilir)
    fun cleanup() {
        premiumCheckJob?.cancel()
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        @Volatile
        private var instance: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return instance ?: synchronized(this) {
                instance ?: BillingManager(context).also { instance = it }
            }
        }
        
        // Promosyon kodundan 1 haftalık premium üyelik süresi (milisaniye cinsinden)
        const val PROMO_PREMIUM_DURATION = 7 * 24 * 60 * 60 * 1000L // 7 gün
    }
} 