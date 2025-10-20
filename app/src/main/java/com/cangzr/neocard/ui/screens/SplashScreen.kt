package com.cangzr.neocard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.utils.NetworkUtils
import com.cangzr.neocard.utils.OnboardingPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: SplashViewModel = viewModel()
) {
    val context = LocalContext.current
    val networkUtils = remember { NetworkUtils.getInstance(context) }
    val onboardingPreferences = remember { OnboardingPreferences.getInstance(context) }
    
    // İnternet bağlantısı durumu
    val isNetworkAvailable by networkUtils.isNetworkAvailable.collectAsState()
    
    // Onboarding durumu
    val isOnboardingCompleted by onboardingPreferences.isOnboardingCompleted.collectAsState(initial = false)
    
    // Animasyon durumları
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500)
    )
    
    // Hata durumu
    var showNetworkError by remember { mutableStateOf(false) }
    
    // İnternet bağlantısı durumu değiştiğinde tetiklenecek efekt
    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable && showNetworkError) {
            // İnternet bağlantısı geldiğinde ve daha önce hata gösteriliyorsa
            // Kullanıcı giriş yapmış mı kontrolü
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            when {
                // Kullanıcı giriş yapmamış ve onboarding tamamlanmamış
                currentUser == null && !isOnboardingCompleted -> {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                // Kullanıcı giriş yapmamış ama onboarding tamamlanmış
                currentUser == null && isOnboardingCompleted -> {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                // Kullanıcı giriş yapmış
                else -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }
    }
    
    // İlk yükleme efekti
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000) // 2 saniye bekle
        
        // İnternet bağlantısı kontrolü
        if (!isNetworkAvailable) {
            showNetworkError = true
        } else {
            // Kullanıcı giriş yapmış mı kontrolü
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            when {
                // Kullanıcı giriş yapmamış ve onboarding tamamlanmamış
                currentUser == null && !isOnboardingCompleted -> {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                // Kullanıcı giriş yapmamış ama onboarding tamamlanmış
                currentUser == null && isOnboardingCompleted -> {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                // Kullanıcı giriş yapmış
                else -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }
    }
    
    // Splash Screen UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alphaAnim)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo3),
                contentDescription = "NeoCard Logo",
                modifier = Modifier.size(180.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Uygulama Adı
            Text(
                text = "NeoCard",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Yükleniyor göstergesi veya hata mesajı
            if (showNetworkError) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.network_error_message),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // İnternet bağlantısı kontrolü devam ediyor göstergesi
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
