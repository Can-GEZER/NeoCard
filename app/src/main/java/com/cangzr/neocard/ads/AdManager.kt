package com.cangzr.neocard.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.cangzr.neocard.billing.BillingManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Uygulama içindeki reklamları yöneten sınıf
 */
class AdManager private constructor(private val context: Context) {

    // Reklam gösterim durumları
    private val _showBannerAds = MutableStateFlow(true)
    val showBannerAds: StateFlow<Boolean> = _showBannerAds

    private val _showInlineAds = MutableStateFlow(true)
    val showInlineAds: StateFlow<Boolean> = _showInlineAds

    private var interstitialAd: InterstitialAd? = null
    private val adRequest: AdRequest = AdRequest.Builder().build()

    // Test reklamları için ID'ler
    companion object {
        private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test Interstitial ID
        
        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context).also { instance = it }
            }
        }
        
        // Her kaç öğede bir reklam gösterileceğini belirten sabitler
        const val JOB_POST_AD_INTERVAL = 4 // Her 3 iş ilanından sonra bir reklam
        const val BUSINESS_CARD_AD_INTERVAL = 3 // Her 2 kartvizitden sonra bir reklam
    }

    init {
        // MobileAds'i başlat
        MobileAds.initialize(context) {}
        
        // Premium kullanıcı kontrolü
        checkPremiumStatus()
        
        // Tam sayfa reklam yükle
        loadInterstitialAd()
    }
    
    // Premium durumuna göre reklam gösterim ayarlarını günceller
    private fun checkPremiumStatus() {
        val billingManager = BillingManager.getInstance(context)
        
        // Coroutine scope için
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            billingManager.isPremium.collect { isPremium ->
                _showBannerAds.value = !isPremium
                _showInlineAds.value = !isPremium
            }
        }
    }
    
    // Banner reklam görünümünü oluşturan composable fonksiyon
    @Composable
    fun BannerAd(modifier: Modifier = Modifier) {
        val showAds by showBannerAds.collectAsState()
        
        if (showAds) {
            AndroidView(
                modifier = modifier,
                factory = { context ->
                    AdView(context).apply {
                        setAdSize(AdSize.BANNER)
                        adUnitId = BANNER_AD_UNIT_ID
                        loadAd(adRequest)
                    }
                }
            )
        }
    }
    
    // Tam sayfa reklam yükleme
    private fun loadInterstitialAd() {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }
            })
    }
    
    // Tam sayfa reklam gösterimi
    fun showInterstitialAd(activity: android.app.Activity, onAdDismissed: () -> Unit) {
        // Premium kullanıcıları kontrol etmek için direkt değeri kullanalım
        if (!_showBannerAds.value) {
            // Premium kullanıcı, reklam gösterme
            onAdDismissed()
            return
        }
        
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            interstitialAd?.setOnPaidEventListener {
                // Reklam gösterildikten sonra yeni bir reklam yükle
                loadInterstitialAd()
            }
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    // Yeni bir reklam yükle
                    loadInterstitialAd()
                }
            }
        } else {
            // Reklam yüklenmediyse callback'i çağır
            onAdDismissed()
            // Yeni bir reklam yüklemeyi dene
            loadInterstitialAd()
        }
    }
}