package com.cangzr.neocard.ui.screens.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.model.TextStyleDTO
import com.cangzr.neocard.ui.screens.home.utils.loadExploreCards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext

@Composable
fun ExploreCardsSection(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    var exploreCards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // İlk veri yüklemesi
    LaunchedEffect(Unit) {
        loadExploreCards(
            firestore = firestore,
            currentUserId = currentUserId,
            pageSize = 10,
            lastCardId = null,
            onSuccess = { cards, _, _ ->
                exploreCards = cards
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Başlık ve Tümünü Gör Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = context.getString(R.string.explore_other_cards),
                style = MaterialTheme.typography.titleLarge
            )
            
            OutlinedButton(
                onClick = {
                    navController.navigate(Screen.ExploreAllCards.route)
                },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.explore),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Kart Listesi
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (exploreCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.search),
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = context.getString(R.string.no_cards_to_explore),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Sadece ilk 20 kartviziti göster
            val limitedCards = exploreCards.take(20)
            
            // Kartvizitleri göster - LazyColumn ile kaydırılabilir
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp), // Sabit yükseklik ver
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(limitedCards) { card ->
                    UserCardItem(card = card) {
                        navController.navigate(Screen.SharedCardDetail.createRoute(card.id))
                    }
                }
            }
        }
    }
}
