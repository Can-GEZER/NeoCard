package com.cangzr.neocard.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun <T> withAdItems(
    items: List<T>,
    adInterval: Int,
    itemContent: @Composable (T) -> Unit,
    adContent: @Composable () -> Unit
): List<@Composable () -> Unit> {
    val context = LocalContext.current
    val adManager = AdManager.getInstance(context)
    val showAds by adManager.showInlineAds.collectAsState(initial = true)
    
    val composableItems = mutableListOf<@Composable () -> Unit>()
    
    if (showAds) {
        items.forEachIndexed { index, item ->
            composableItems.add { itemContent(item) }
            
            if ((index + 1) % adInterval == 0 && index < items.size - 1) {
                composableItems.add { adContent() }
            }
        }
    } else {
        items.forEach { item ->
            composableItems.add { itemContent(item) }
        }
    }
    
    return composableItems
}

@Composable
fun InlineAdView(modifier: Modifier = Modifier) {
    val adManager = AdManager.getInstance(LocalContext.current)
    val showAds by adManager.showInlineAds.collectAsState(initial = true)
    
    if (showAds) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            factory = { context ->
                createBannerAdView(context)
            }
        )
    }
}

@Composable
fun BottomBannerAd(modifier: Modifier = Modifier) {
    val adManager = AdManager.getInstance(LocalContext.current)
    adManager.BannerAd(modifier = modifier.fillMaxWidth())
}

private fun createBannerAdView(context: Context): AdView {
    return AdView(context).apply {
        setAdSize(AdSize.BANNER)
        adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test Banner ID
        loadAd(AdRequest.Builder().build())
    }
} 
