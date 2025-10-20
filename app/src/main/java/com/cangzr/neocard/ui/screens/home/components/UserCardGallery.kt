package com.cangzr.neocard.ui.screens.home.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.ui.screens.home.utils.onAppear
import com.cangzr.neocard.ui.screens.home.utils.loadCards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCardGallery(
    navController: NavHostController, 
    filterType: String,
    onCardSelected: ((UserCard) -> Unit)? = null
) {
    val context = LocalContext.current
    val allFilterText = context.getString(R.string.all)
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser

    var cards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var filteredCards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<UserCard?>(null) }
    
    // Bottom sheet state
    val bottomSheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Navigation için LaunchedEffect
    LaunchedEffect(selectedCard) {
        selectedCard?.let { card ->
            // Dialog'u HomeScreen'de göstermek için callback kullan
            onCardSelected?.invoke(card)
            selectedCard = null // Reset after selection
        }
    }
    
    // Pagination için değişkenler
    var lastVisibleCard by remember { mutableStateOf<String?>(null) }
    var hasMoreCards by remember { mutableStateOf(true) }
    val pageSize = 10

    // İlk veri yüklemesi
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            loadCards(
                userId = currentUser.uid,
                firestore = firestore,
                pageSize = pageSize,
                lastCardId = null,
                onSuccess = { newCards, lastId, hasMore ->
                    cards = newCards
                    // Filtreleme işlemi
                    filteredCards = if (filterType == allFilterText) {
                        newCards
                    } else {
                        newCards.filter { card ->
                            val cardTypeTitle = try {
                                CardType.valueOf(card.cardType).getTitle(context)
                            } catch (e: IllegalArgumentException) {
                                card.cardType
                            }
                            cardTypeTitle == filterType
                        }
                    }
                    lastVisibleCard = lastId
                    hasMoreCards = hasMore
                    isLoading = false
                },
                onError = {
                    isLoading = false
                }
            )
        } else {
            // Kullanıcı giriş yapmamış, yükleme durumunu kapat
            isLoading = false
        }
    }

    // Filtreleme değiştiğinde yeniden filtrele
    LaunchedEffect(filterType, cards) {
        filteredCards = if (filterType == allFilterText) {
            cards
        } else {
            // CardType enum'ları ile karşılaştırma yap
            cards.filter { card ->
                val cardTypeTitle = try {
                    CardType.valueOf(card.cardType).getTitle(context)
                } catch (e: IllegalArgumentException) {
                    card.cardType // Eğer enum değilse direkt string'i kullan
                }
                cardTypeTitle == filterType
            }
        }
    }

    // Daha fazla kart yükleme fonksiyonu
    fun loadMoreCards() {
        if (!hasMoreCards || isLoadingMore || currentUser == null) return
        
        isLoadingMore = true
        loadCards(
            userId = currentUser.uid,
            firestore = firestore,
            pageSize = pageSize,
            lastCardId = lastVisibleCard,
            onSuccess = { newCards, lastId, hasMore ->
                val updatedCards = cards + newCards
                cards = updatedCards
                // Filtreleme işlemi
                filteredCards = if (filterType == allFilterText) {
                    updatedCards
                } else {
                    updatedCards.filter { card ->
                        val cardTypeTitle = try {
                            CardType.valueOf(card.cardType).getTitle(context)
                        } catch (e: IllegalArgumentException) {
                            card.cardType
                        }
                        cardTypeTitle == filterType
                    }
                }
                lastVisibleCard = lastId
                hasMoreCards = hasMore
                isLoadingMore = false
            },
            onError = {
                isLoadingMore = false
            }
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (currentUser == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.cards),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Text(
                        text = context.getString(R.string.login_to_create_cards),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Button(
                        onClick = { navController.navigate(Screen.Auth.route) },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text(context.getString(R.string.login))
                    }
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredCards.isEmpty()) {
            // Kullanıcının hiç kartı yoksa veya filtrelenmiş sonuç boşsa uyarı göster
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (cards.isEmpty()) 
                        context.getString(R.string.no_cards_created)
                    else 
                        context.getString(R.string.no_cards_of_type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(filteredCards) { card ->
                    UserCardItem(card = card, onClick = { selectedCard = card })
                }
                
                // Daha fazla kart varsa yükleme göstergesi
                if (hasMoreCards) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(30.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                // Son öğeye gelince daha fazla yükle
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .onAppear {
                                            loadMoreCards()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    
    // Bottom Sheet for sharing
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            // Paylaşma bottomsheet içeriği buraya gelecek
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = context.getString(R.string.share_card),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = context.getString(R.string.share_coming_soon),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
