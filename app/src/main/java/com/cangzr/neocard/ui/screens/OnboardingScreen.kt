package com.cangzr.neocard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.utils.OnboardingPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(navController: NavHostController) {
    val context = LocalContext.current
    val onboardingPreferences = remember { OnboardingPreferences.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var currentPage by remember { mutableStateOf(0) }
    
    // Onboarding sayfaları
    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_title_1),
            description = stringResource(R.string.onboarding_desc_1),
            imageRes = R.drawable.first
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_title_2),
            description = stringResource(R.string.onboarding_desc_2),
            imageRes = R.drawable.second
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_title_3),
            description = stringResource(R.string.onboarding_desc_3),
            imageRes = R.drawable.third
        )
    )
    
    // Onboarding tamamlandı işlemi
    fun finishOnboarding() {
        coroutineScope.launch {
            onboardingPreferences.setOnboardingCompleted(true)
            navController.navigate(Screen.Auth.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Onboarding içeriği
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sayfa içeriği
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(600)) + slideInHorizontally(),
                exit = fadeOut(animationSpec = tween(600)) + slideOutHorizontally()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    // Resim
                    Image(
                        painter = painterResource(id = pages[currentPage].imageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Başlık
                    Text(
                        text = pages[currentPage].title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Açıklama
                    Text(
                        text = pages[currentPage].description,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
            
            // Alt kısım - İndikatörler ve butonlar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sayfa indikatörleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(pages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (currentPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Atla butonu
                    TextButton(onClick = { finishOnboarding() }) {
                        Text(
                            text = stringResource(R.string.skip),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // İleri / Başla butonu
                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                finishOnboarding()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (currentPage < pages.size - 1) 
                                stringResource(R.string.next_onboarding) 
                            else 
                                stringResource(R.string.get_started)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

/**
 * Onboarding sayfası veri sınıfı
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val imageRes: Int
)
