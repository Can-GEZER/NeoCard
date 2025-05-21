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
            // Kullanıcı satın alma işlemini iptal etti - durumu yeniden kontrol et
            checkPremiumStatus()
        } else {
            // Diğer hata durumlarında da durumu güncelle
            checkPremiumStatus()
        }
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
                delay(300000) // Her 5 dakikada bir kontrol et (daha düşük sıklık)
                if (isActive) {
                    // Önce BillingClient üzerinden kontrol et
                    if (billingClient.isReady) {
                        queryPurchases() 
                    } else {
                        startBillingConnection()
                        delay(1000)
                        checkPremiumStatus()
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
                    } else {
                        // Hata durumunda tekrar kontrol et
                        checkPremiumStatus()
                    }
                }
            } else {
                updatePremiumStatus(true)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            // Bekleyen satın alma - durumu değiştirme ama takip et
            coroutineScope.launch {
                delay(10000) // 10 saniye sonra tekrar kontrol et
                queryPurchases() // Satın alma durumunu yeniden kontrol et
            }
        } else {
            // Diğer durumlarda premium durumunu güncelle (iptal edilmiş olabilir)
            checkPremiumStatus()
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) {
            startBillingConnection()
            return
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
                    
                    // Aktif premium satın alma yoksa ve şu anda premium görünüyorsa, durumu güncelle
                    if (!hasPremiumPurchase && _isPremium.value) {
                        checkPremiumStatus()
                    }
                } else {
                    // Hata durumunda tekrar dene
                    retryQueryPurchases()
                }
            }
        } catch (e: Exception) {
            // Exception durumunda tekrar dene
            retryQueryPurchases()
        }
    }
    
    private fun retryQueryPurchases() {
        purchaseQueryRetryCount++
        if (purchaseQueryRetryCount <= MAX_PURCHASE_QUERY_RETRY) {
            coroutineScope.launch {
                // Her denemede bekleme süresini arttır
                delay(1000L * purchaseQueryRetryCount)
                queryPurchases()
            }
        } else {
            // Maksimum deneme sayısına ulaşıldı, Firestore durumunu kontrol et
            purchaseQueryRetryCount = 0
            checkPremiumStatus()
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
            } else {
                // Ürün detayları alınamadı, premium durumunu kontrol et
                checkPremiumStatus()
            }
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
                    // Hata durumunda tekrar kontrol et
                    checkPremiumStatus()
                }
        }
    }

    fun checkPremiumStatus() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            // Önce billingClient üzerinden kontrol et
            if (billingClient.isReady) {
                queryPurchases()
            }
            
            // Firestore kontrolü
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
                    // Hata durumunda durumu yeniden kontrol et
                    checkPremiumStatus()
                }
        } catch (e: Exception) {
            result = false
            // Hata durumunda durumu yeniden kontrol et
            checkPremiumStatus()
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