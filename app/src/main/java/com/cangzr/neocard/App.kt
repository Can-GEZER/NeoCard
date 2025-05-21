import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cangzr.neocard.Screen
import com.cangzr.neocard.ads.AdManager
import com.cangzr.neocard.storage.FirebaseStorageManager
import com.cangzr.neocard.storage.StorageCleanupWorker
import java.util.concurrent.TimeUnit

@Composable
fun App(initialCardId: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // AdManager örneğini başlat
    val adManager = remember { AdManager.getInstance(context) }

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

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // ... existing navigation code ...
    }
} 