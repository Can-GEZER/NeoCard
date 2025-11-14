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
            checkFirestorePremiumStatus()
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

    private fun startPeriodicPremiumCheck() {
        premiumCheckJob?.cancel()
        premiumCheckJob = coroutineScope.launch {
            while (isActive) {
                delay(1800000) // Her 30 dakikada bir kontrol et (daha düşük sıklık)
                if (isActive) {
                    if (billingClient.isReady) {
                        queryPurchases() 
                    } else {
                        checkFirestorePremiumStatus()
                    }
                }
            }
        }
    }

    private fun startBillingConnection() {
        if (billingClient.isReady) return
        
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    connectionRetryCount = 0
                    queryPurchases()
                } else {
                    retryBillingConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                retryBillingConnection()
            }
        })
    }
    
    private fun retryBillingConnection() {
        connectionRetryCount++
        if (connectionRetryCount <= MAX_RETRY_COUNT) {
            coroutineScope.launch {
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
                        updatePremiumStatus(true)
                    }
                }
            } else {
                updatePremiumStatus(true)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            coroutineScope.launch {
                delay(30000)
                if (billingClient.isReady) {
                    queryPurchases()
                }
            }
        }
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
                    purchaseQueryRetryCount = 0
                    
                    var hasPremiumPurchase = false
                    for (purchase in purchases) {
                        if (purchase.products.contains("premium_subscription") && 
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            handlePurchase(purchase)
                            hasPremiumPurchase = true
                        }
                    }
                    
                    if (!hasPremiumPurchase) {
                        checkFirestorePremiumStatus()
                    }
                } else if (billingResult.responseCode != BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE) {
                    retryQueryPurchases()
                }
            }
        } catch (e: Exception) {
        }
    }
    
    private fun retryQueryPurchases() {
        purchaseQueryRetryCount++
        if (purchaseQueryRetryCount <= MAX_PURCHASE_QUERY_RETRY) {
            coroutineScope.launch {
                delay(2000L * purchaseQueryRetryCount)
                if (billingClient.isReady) {
                    queryPurchases()
                }
            }
        } else {
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
                }
        }
    }

    private fun checkFirestorePremiumStatus() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val isPremium = document.getBoolean("premium") ?: false
                    val premiumEndTime = document.getLong("premiumEndTime") ?: 0L
                    
                    if (isPremium && premiumEndTime > 0) {
                        if (System.currentTimeMillis() >= premiumEndTime) {
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
                }
        } ?: run {
            coroutineScope.launch {
                _isPremium.emit(false)
            }
        }
    }

    fun checkPremiumStatus() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            if (billingClient.isReady) {
                queryPurchases()
            } else {
                checkFirestorePremiumStatus()
            }
        } ?: run {
            coroutineScope.launch {
                _isPremium.emit(false)
            }
        }
    }
    
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

    fun extendPremiumWithPromoCode(userId: String, additionalDuration: Long): Boolean {
        var result = false
        try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val currentEndTime = document.getLong("premiumEndTime") ?: 0L
                    val currentTime = System.currentTimeMillis()
                    
                    val newEndTime = if (currentEndTime > currentTime) {
                        currentEndTime + additionalDuration
                    } else {
                        currentTime + additionalDuration
                    }
                    
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

    fun refreshPremiumStatus() {
        checkPremiumStatus()
    }

    fun checkPremiumOnResume() {
        checkPremiumStatus()
    }

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
        
        const val PROMO_PREMIUM_DURATION = 7 * 24 * 60 * 60 * 1000L // 7 gün
    }
} 
