package com.cangzr.neocard

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cangzr.neocard.Screen
import com.cangzr.neocard.ads.AdManager
import com.cangzr.neocard.ads.BottomBannerAd
import com.cangzr.neocard.storage.FirebaseStorageManager
import com.cangzr.neocard.storage.StorageCleanupWorker
import com.cangzr.neocard.ui.screens.HomeScreen
import com.cangzr.neocard.ui.screens.ProfileScreen
import com.cangzr.neocard.ui.screens.CardDetailScreen
import com.cangzr.neocard.ui.screens.SharedCardDetailScreen
import com.cangzr.neocard.ui.screens.AuthScreen
import com.cangzr.neocard.ui.screens.ConnectionRequestsScreen
import com.cangzr.neocard.ui.screens.CreateCardScreen
import com.cangzr.neocard.ui.screens.CardStatisticsScreen
import com.cangzr.neocard.ui.components.BottomNavBar
import com.cangzr.neocard.ui.screens.BusinessCardListScreen
import com.cangzr.neocard.ui.screens.ExploreAllCardsScreen
import com.cangzr.neocard.ui.screens.SplashScreen
import com.cangzr.neocard.ui.screens.OnboardingScreen
import com.cangzr.neocard.ui.screens.NotificationsScreen
import com.cangzr.neocard.utils.LanguageManager
import com.cangzr.neocard.utils.NetworkUtils
import java.util.concurrent.TimeUnit

@Composable
fun NeoCardApp(initialCardId: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // AdManager örneğini başlat
    val adManager = remember { AdManager.getInstance(context) }
    
    // NetworkUtils örneğini başlat
    val networkUtils = remember { NetworkUtils.getInstance(context) }

    // Dil ayarlarını uygula
    LaunchedEffect(Unit) {
        LanguageManager.applyLanguageFromPreference(context)
    }

    // Periyodik temizleme işlemini başlat
    LaunchedEffect(Unit) {
        // Her 7 günde bir çalışacak periyodik iş
        val cleanupWorkRequest = PeriodicWorkRequestBuilder<StorageCleanupWorker>(
            7, TimeUnit.DAYS, // Her 7 günde bir çalış
            6, TimeUnit.HOURS  // 6 saatlik esneklik
        ).build()
        
        // Önceden zamanlanmış bir iş varsa, onu güncelle
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "storage_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWorkRequest
        )
    }

    LaunchedEffect(initialCardId) {
        // Deep link ile gelen cardId varsa, detay sayfasına yönlendir
        initialCardId?.let { cardId ->
            navController.navigate(Screen.SharedCardDetail.createRoute(cardId))
        }
    }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            // Ana ekranlar için alt menüyü göster, detay ekranlarında gizle
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val mainRoutes = listOf(Screen.Home.route, Screen.Profile.route, Screen.CreateCard.route,
                Screen.Business.route)
            
            if (currentRoute in mainRoutes) {
                Column {
                    // Önce navigation bar
                    BottomNavBar(navController = navController)
                    // Sonra alt banner reklam
                    BottomBannerAd()
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Splash ve Onboarding ekranları
            composable(Screen.Splash.route) {
                SplashScreen(navController = navController)
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(navController = navController)
            }
            
            // Ana ekranlar
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Business.route) {
                BusinessCardListScreen(navController = navController)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
            composable(Screen.CreateCard.route) {
                CreateCardScreen(navController = navController)
            }
            composable(Screen.Auth.route) {
                AuthScreen(navController = navController)
            }
            composable(Screen.ConnectionRequests.route) {
                ConnectionRequestsScreen(navController = navController)
            }
            composable(Screen.CardDetail.route) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
                CardDetailScreen(
                    cardId = cardId,
                    onBackClick = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable(Screen.SharedCardDetail.route) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
                SharedCardDetailScreen(
                    cardId = cardId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(Screen.CardStatistics.route) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
                CardStatisticsScreen(
                    navController = navController,
                    cardId = cardId
                )
            }
            composable(Screen.ExploreAllCards.route) {
                ExploreAllCardsScreen(navController = navController)
            }
            composable(Screen.Notifications.route) {
                NotificationsScreen(navController = navController)
            }
        }
    }
} 