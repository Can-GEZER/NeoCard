package com.cangzr.neocard

import App
import CardDetailScreen
import SharedCardDetailScreen
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cangzr.neocard.ui.screens.AuthScreen
import com.cangzr.neocard.ui.screens.BusinessCardListScreen
import com.cangzr.neocard.ui.screens.ComplaintsScreen
import com.cangzr.neocard.ui.screens.CreateCardScreen
import com.cangzr.neocard.ui.screens.HomeScreen
import com.cangzr.neocard.ui.screens.JobPostDetail
import com.cangzr.neocard.ui.screens.PostAdScreen
import com.cangzr.neocard.ui.screens.ProfileScreen
import com.cangzr.neocard.ui.theme.NeoCardTheme
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.MutableState
import com.cangzr.neocard.ads.AdManager
import com.cangzr.neocard.ads.BottomBannerAd
import com.cangzr.neocard.ui.screens.JobPostScreen
import com.cangzr.neocard.ui.screens.initializeCoil
import com.google.android.gms.ads.MobileAds
import com.cangzr.neocard.billing.BillingManager

sealed class Screen(
    val route: String,
    val iconRes: Int,
    val label: String,
    val indicatorColor: Color
) {
    object Home : Screen(
        "home",
        R.drawable.home, // İkonun drawable ismi
        "Ana Sayfa",
        Color(0xFFFFEB3B) // Sarı
    )
    object JobPost : Screen(
        "jobpost",
        R.drawable.job, // İkonun drawable ismi
        "İş İlanları",
        Color(0xFF2196F3) // Mavi
    )
    object CardList : Screen(
        "cardlist",
        R.drawable.connect, // İkonun drawable ismi
        "Kartvizitler",
        Color(0xFF4CAF50) // Yeşil
    )
    object CreateCard : Screen(
        "createcard",
        R.drawable.add, // İkonun drawable ismi
        "Yeni Kart",
        Color(0xFFE91E63) // Pembe
    )
    object Profile : Screen(
        "profile",
        R.drawable.profile, // İkonun drawable ismi
        "Profil",
        Color(0xFFFF9800) // Turuncu
    )
    object Auth : Screen(
        "auth",
        R.drawable.profile,
        "Giriş",
        Color(0xFFFF9800)
    )
    object CardDetail : Screen(
        "card_detail/{cardId}",
        R.drawable.cards,
        "Kart Detayı",
        Color(0xFF9C27B0)
    ) {
        fun createRoute(cardId: String) = "card_detail/$cardId"
    }
    object SharedCardDetail : Screen(
        "sharedCardDetail/{cardId}",
        R.drawable.cards,
        "Paylaşılan Kart",
        Color(0xFF9C27B0)
    ) {
        fun createRoute(cardId: String) = "shared_card_detail/$cardId"
    }
    object PostAd : Screen(
        "postad",
        R.drawable.add,
        "Post Ad",
        Color(0xFF9C27B0)
    )
    object JobPostDetail : Screen(
        "jobPostDetail/{jobPostId}",
        R.drawable.add,
        "Job Post Detail",
        Color(0xFF9C27B0)
    ) {
        fun createRoute(jobPostId: String) = "jobPostDetail/$jobPostId"
    }
    object ComplaintsScreen : Screen(
        "complaints",
        R.drawable.add,
        "Complaints Detail",
        Color(0xFF9C27B0)
    )
    companion object {
        val items = listOf(Home, JobPost, CardList, CreateCard, Profile)
    }
}

class MainActivity : ComponentActivity() {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val connectivityManager by lazy {
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    private val _showNoInternetDialog = mutableStateOf(false)
    val showNoInternetDialog = _showNoInternetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Coil görsel yükleme kütüphanesini yapılandır
        initializeCoil(this)
        
        // MobileAds'i başlat
        MobileAds.initialize(this) {}
        
        // AdManager'ı başlat
        AdManager.getInstance(this)
        
        // Deep link'i işle
        handleDeepLink(intent)
        
        // İlk giriş kontrolü
        if (!checkInternetConnection()) {
            _showNoInternetDialog.value = true
        }

        setupNetworkCallback()

        // Kamera izni kontrolü
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        setContent {
            NeoCardTheme {
                val showDialog = remember { showNoInternetDialog }
                
                if (showDialog.value) {
                    NoInternetDialog(
                        onRetry = {
                            if (checkInternetConnection()) {
                                showDialog.value = false
                            }
                        },
                        onDismiss = { finish() }
                    )
                }
                
                MainScreen(showNoInternetDialog = showDialog)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Yeni intent geldiğinde deep link'i işle
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        intent.data?.let { uri ->
            when {
                // HTTPS deep link
                (uri.scheme == "https" && uri.host == "neocardapp.com" && uri.path?.startsWith("/card/") == true) ||
                // Özel uygulama deep link
                (uri.scheme == "neocard" && uri.host == "card") -> {
                    val cardId = uri.lastPathSegment
                    if (!cardId.isNullOrEmpty()) {
                        // Kartvizit detay sayfasına yönlendir
                        navigateToCardDetail(cardId)
                    }
                }
            }
        }
    }

    private fun navigateToCardDetail(cardId: String) {
        // NavController'a erişim için composable içinde işlem yapmalıyız
        setContent {
            NeoCardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(initialCardId = cardId)
                }
            }
        }
    }

    private fun setupNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // İnternet bağlantısı geldiğinde
                runOnUiThread {
                    _showNoInternetDialog.value = false
                    Toast.makeText(
                        this@MainActivity,
                        "İnternet bağlantısı kuruldu",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onLost(network: Network) {
                // İnternet bağlantısı gittiğinde
                runOnUiThread {
                    _showNoInternetDialog.value = true
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        BillingManager.getInstance(applicationContext).cleanup()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}

private fun Context.checkInternetConnection(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(network)
    
    return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}

@Composable
fun NoInternetDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { /* Dialog'u kapatma işlemini engelle */ }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "İnternet Bağlantısı Yok",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Uygulamayı kullanmak için internet bağlantısı gerekiyor.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Tekrar Dene")
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Çıkış")
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(showNoInternetDialog: MutableState<Boolean>) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Auth ekranında bottom bar'ı gizlemek için current route'u takip edelim
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBottomBar = currentRoute != Screen.Auth.route && 
                       currentRoute?.startsWith("card_detail") != true &&
                       currentRoute?.startsWith("sharedCardDetail") != true &&
                       currentRoute != Screen.PostAd.route &&
                       currentRoute?.startsWith("jobPostDetail") != true &&
                       currentRoute != Screen.ComplaintsScreen.route

    if (showNoInternetDialog.value) {
        NoInternetDialog(
            onRetry = {
                if (context.checkInternetConnection()) {
                    showNoInternetDialog.value = false
                }
            },
            onDismiss = {
                (context as? Activity)?.finish()
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // Bottom Navigation Bar
                    BottomNavigationBar(navController)
                    
                    // Banner Reklam
                    BottomBannerAd(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(bottom = 4.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(
                bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp,
                top = if (showBottomBar) innerPadding.calculateTopPadding() else 0.dp
            )
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.JobPost.route) { JobPostScreen(navController) }
            composable(Screen.CardList.route) { BusinessCardListScreen(navController) }
            composable(Screen.CreateCard.route) { CreateCardScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController) }
            composable(
                route = Screen.Auth.route,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AuthScreen(navController)
                }
            }
            composable(
                route = Screen.CardDetail.route,
                arguments = listOf(
                    navArgument("cardId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId")
                CardDetailScreen(
                    cardId = cardId ?: "",
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.SharedCardDetail.route,
                arguments = listOf(
                    navArgument("cardId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId")
                SharedCardDetailScreen(
                    cardId = cardId ?: "",
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.PostAd.route) {
                PostAdScreen(navController)
            }
            composable(
                route = Screen.JobPostDetail.route,
                arguments = listOf(navArgument("jobPostId") { type = NavType.StringType })
            ) { backStackEntry ->
                val jobPostId = backStackEntry.arguments?.getString("jobPostId") ?: ""
                JobPostDetail(jobPostId, navController)
            }
            composable(Screen.ComplaintsScreen.route) { // 🔥 Şikayetler Sayfası Eklendi
                ComplaintsScreen(navController)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Seçili ve seçili olmayan durum için sabit renkler
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp
    ) {
        Screen.items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            NavigationBarItem(
                icon = {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // İkon üzerindeki gösterge çizgisi
                        if (isSelected) {
                    Box(
                        modifier = Modifier
                                    .width(24.dp)
                                    .height(3.dp)
                                    .background(selectedColor, RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        } else {
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                        
                        // İkon
                        Icon(
                            painter = painterResource(id = screen.iconRes),
                            contentDescription = screen.label,
                            modifier = Modifier.size(if (isSelected) 26.dp else 22.dp),
                            tint = if (isSelected) selectedColor else unselectedColor
                        )
                    }
                },
                label = null, // Etiket gösterme
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Unspecified,
                    unselectedIconColor = Color.Unspecified,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NeoCardTheme {
        Greeting("Android")
    }
}
