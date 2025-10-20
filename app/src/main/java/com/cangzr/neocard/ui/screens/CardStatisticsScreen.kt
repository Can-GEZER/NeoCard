package com.cangzr.neocard.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.analytics.CardAnalyticsManager
import com.cangzr.neocard.analytics.CardStatistics
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStatisticsScreen(
    navController: NavHostController,
    cardId: String
) {
    val context = LocalContext.current
    val analyticsManager = CardAnalyticsManager.getInstance()
    var statistics by remember { mutableStateOf<CardStatistics?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Günleri temsil eden liste (bugünden geriye doğru 7 gün)
    val dayLabels = remember {
        val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        List(7) { index ->
            calendar.add(Calendar.DAY_OF_MONTH, if (index == 0) 0 else -1)
            dateFormat.format(calendar.time)
        }.reversed()
    }

    // İstatistikleri yükle
    LaunchedEffect(cardId) {
        loadStatistics(analyticsManager, cardId, context) { stats, errorMsg ->
            statistics = stats
            error = errorMsg
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(context.getString(R.string.card_statistics)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            loadStatistics(analyticsManager, cardId, context) { stats, errorMsg ->
                                statistics = stats
                                error = errorMsg
                                isLoading = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = context.getString(R.string.refresh)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.info),
                            contentDescription = context.getString(R.string.error),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = error ?: context.getString(R.string.unknown_error),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = {
                                isLoading = true
                                loadStatistics(analyticsManager, cardId, context) { stats, errorMsg ->
                                    statistics = stats
                                    error = errorMsg
                                    isLoading = false
                                }
                            }
                        ) {
                            Text(context.getString(R.string.retry))
                        }
                    }
                }
            } else if (statistics != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Özet İstatistikler
                    StatisticsSummarySection(statistics!!)
                    
                    // Görüntülenme Grafiği
                    ViewsChartSection(statistics!!, dayLabels)
                    
                    // Etkileşim Detayları
                    InteractionDetailsSection(statistics!!)
                }
            }
        }
    }
}

@Composable
fun StatisticsSummarySection(statistics: CardStatistics) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = context.getString(R.string.summary_statistics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    title = context.getString(R.string.total_views),
                    value = "${statistics.totalViews}",
                    iconRes = R.drawable.eye,
                    modifier = Modifier.weight(1f)
                )
                
                StatisticItem(
                    title = context.getString(R.string.unique_visitors),
                    value = "${statistics.uniqueViewers}",
                    iconRes = R.drawable.person,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem(
                    title = context.getString(R.string.link_clicks),
                    value = "${statistics.linkClicks}",
                    iconRes = R.drawable.link,
                    modifier = Modifier.weight(1f)
                )
                
                StatisticItem(
                    title = context.getString(R.string.qr_scans),
                    value = "${statistics.qrScans}",
                    iconRes = R.drawable.qr_code,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    title: String,
    value: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ViewsChartSection(statistics: CardStatistics, dayLabels: List<String>) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = context.getString(R.string.last_seven_days_views),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Görüntülenme grafiği
            val weeklyViews = statistics.weeklyViews.reversed() // Son günden ilk güne sıralı
            val maxViews = weeklyViews.maxOrNull() ?: 1L
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 32.dp) // Gün etiketleri için alan
            ) {
                // Çizgili arka plan
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    
                    // Yatay çizgiler
                    val lineCount = 5
                    for (i in 0..lineCount) {
                        val y = size.height * (1 - i.toFloat() / lineCount)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            pathEffect = dashPathEffect
                        )
                    }
                }
                
                // Çubuk grafik
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyViews.forEachIndexed { index, views ->
                        val barHeight = if (maxViews > 0) {
                            160.dp * (views.toFloat() / maxViews.toFloat())
                        } else {
                            0.dp
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(barHeight)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Gün etiketi
                            Text(
                                text = dayLabels[index],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            
                            // Görüntülenme sayısı
                            Text(
                                text = "$views",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionDetailsSection(statistics: CardStatistics) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = context.getString(R.string.interaction_details),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Link tıklamaları
            if (statistics.linkClicksByType.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.link_clicks),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                statistics.linkClicksByType.forEach { (type, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconRes = when (type.lowercase()) {
                            "email" -> R.drawable.email
                            "phone" -> R.drawable.phone
                            "website" -> R.drawable.web
                            "linkedin" -> R.drawable.linkedin
                            "github" -> R.drawable.github
                            "twitter" -> R.drawable.twitt
                            "instagram" -> R.drawable.insta
                            "facebook" -> R.drawable.face
                            "cv" -> R.drawable.document
                            else -> R.drawable.link
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = type.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = context.getString(R.string.no_link_clicks),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Paylaşım detayları
            if (statistics.sharesByMethod.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.share_details),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                statistics.sharesByMethod.forEach { (method, count) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val iconRes = when (method.lowercase()) {
                            "qr" -> R.drawable.qr_code
                            "link" -> R.drawable.link
                            "image" -> R.drawable.image
                            "nfc" -> R.drawable.nfc
                            else -> R.drawable.share
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = method.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = context.getString(R.string.no_shares),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

private fun loadStatistics(
    analyticsManager: CardAnalyticsManager,
    cardId: String,
    context: android.content.Context,
    callback: (CardStatistics?, String?) -> Unit
) {
    analyticsManager.getCardStatistics(
        cardId = cardId,
        onSuccess = { stats ->
            callback(stats, null)
        },
        onError = { e ->
            callback(null, context.getString(R.string.statistics_load_error, e.message))
        }
    )
}
