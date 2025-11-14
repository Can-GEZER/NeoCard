package com.cangzr.neocard.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.cangzr.neocard.BuildConfig
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

class AdManager private constructor(private val context: Context) {

    private val _showBannerAds = MutableStateFlow(true)
    val showBannerAds: StateFlow<Boolean> = _showBannerAds

    private val _showInlineAds = MutableStateFlow(true)
    val showInlineAds: StateFlow<Boolean> = _showInlineAds

    private var interstitialAd: InterstitialAd? = null
    private val adRequest: AdRequest = AdRequest.Builder().build()

    companion object {
        private val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
        private val INTERSTITIAL_AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID
        
        @Volatile
        private var instance: AdManager? = null

        fun getInstance(context: Context): AdManager {
            return instance ?: synchronized(this) {
                instance ?: AdManager(context).also { instance = it }
            }
        }
        
        const val JOB_POST_AD_INTERVAL = 4 // Her 3 iş ilanından sonra bir reklam
        const val BUSINESS_CARD_AD_INTERVAL = 3 // Her 2 kartvizitden sonra bir reklam
    }

    init {
        MobileAds.initialize(context) {}
        
        checkPremiumStatus()
        
        loadInterstitialAd()
    }
    
    private fun checkPremiumStatus() {
        val billingManager = BillingManager.getInstance(context)
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            billingManager.isPremium.collect { isPremium ->
                _showBannerAds.value = !isPremium
                _showInlineAds.value = !isPremium
            }
        }
    }
    
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
    
    fun showInterstitialAd(activity: android.app.Activity, onAdDismissed: () -> Unit) {
        if (!_showBannerAds.value) {
            onAdDismissed()
            return
        }
        
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            interstitialAd?.setOnPaidEventListener {
                loadInterstitialAd()
            }
            interstitialAd?.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    onAdDismissed()
                    loadInterstitialAd()
                }
            }
        } else {
            onAdDismissed()
            loadInterstitialAd()
        }
    }
}
