package com.cangzr.neocard.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cangzr.neocard.R
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.Screen
import com.cangzr.neocard.data.model.TextStyleDTO
import com.cangzr.neocard.ads.InlineAdView
import com.cangzr.neocard.ads.withAdItems
import com.cangzr.neocard.ui.screens.home.utils.loadExploreCards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreAllCardsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid

    var exploreCards by remember { mutableStateOf<List<UserCard>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastVisibleCard by remember { mutableStateOf<String?>(null) }
    var hasMoreCards by remember { mutableStateOf(true) }
    val pageSize = 20

    LaunchedEffect(Unit) {
        loadExploreCards(
            firestore = firestore,
            currentUserId = currentUserId,
            pageSize = pageSize,
            lastCardId = null,
            onSuccess = { cards, lastId, hasMore ->
                exploreCards = cards
                lastVisibleCard = lastId
                hasMoreCards = hasMore
                isLoading = false
            },
            onError = {
                isLoading = false
            }
        )
    }
    
    fun loadMoreCards() {
        if (!hasMoreCards || isLoadingMore || currentUserId == null) return
        
        isLoadingMore = true
        loadExploreCards(
            firestore = firestore,
            currentUserId = currentUserId,
            pageSize = pageSize,
            lastCardId = lastVisibleCard,
            onSuccess = { newCards, lastId, hasMore ->
                exploreCards = exploreCards + newCards
                lastVisibleCard = lastId
                hasMoreCards = hasMore
                isLoadingMore = false
            },
            onError = {
                isLoadingMore = false
            }
        )
    }

    val filteredCards = remember(exploreCards, searchQuery) {
        if (searchQuery.isEmpty()) {
            exploreCards
        } else {
            exploreCards.filter { card ->
                card.name.contains(searchQuery, ignoreCase = true) ||
                card.surname.contains(searchQuery, ignoreCase = true) ||
                card.title.contains(searchQuery, ignoreCase = true) ||
                card.company.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = context.getString(R.string.explore_all_cards),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = context.getString(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(context.getString(R.string.search_explore_cards)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.explore),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                context.getString(R.string.no_cards_found)
                            } else {
                                context.getString(R.string.no_explore_search_results)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCards.size) { index ->
                        val card = filteredCards[index]
                        
                        if (index > 0 && index % 5 == 0) {
                            InlineAdView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(Screen.SharedCardDetail.createRoute(card.id)) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .background(parseBackground(card))
                                    .padding(16.dp)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().fillMaxSize()
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (!card.profileImageUrl.isNullOrEmpty()) {
                                            coil.compose.AsyncImage(
                                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                                    .data(card.profileImageUrl)
                                                    .crossfade(true)
                                                    .size(coil.size.Size(192, 192)) // Tam olarak ihtiyaç duyulan boyut (64dp = 192px @3x)
                                                    .memoryCacheKey(card.profileImageUrl)
                                                    .diskCacheKey(card.profileImageUrl)
                                                    .placeholder(R.drawable.logo3)
                                                    .error(R.drawable.logo3)
                                                    .transformations(coil.transform.CircleCropTransformation())
                                                    .build(),
                                                contentDescription = "Profil Resmi",
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("${card.name} ${card.surname}", style = parseTextStyle(card.textStyles["NAME_SURNAME"]))
                                            Text(card.title, style = parseTextStyle(card.textStyles["TITLE"]))
                                            Text(card.company, style = parseTextStyle(card.textStyles["COMPANY"]))
                                            Text(card.email, style = parseTextStyle(card.textStyles["EMAIL"]))
                                            Text(card.phone, style = parseTextStyle(card.textStyles["PHONE"]))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (card.website.isNotEmpty()) ExploreSocialIcon(R.drawable.web, Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000")))
                                        if (card.linkedin.isNotEmpty()) ExploreSocialIcon(R.drawable.linkedin, Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000")))
                                        if (card.github.isNotEmpty()) ExploreSocialIcon(R.drawable.github, Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000")))
                                        if (card.twitter.isNotEmpty()) ExploreSocialIcon(R.drawable.twitt, Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000")))
                                        if (card.instagram.isNotEmpty()) ExploreSocialIcon(R.drawable.insta, Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000")))
                                        if (card.facebook.isNotEmpty()) ExploreSocialIcon(R.drawable.face, Color(android.graphics.Color.parseColor(card.textStyles["NAME_SURNAME"]?.color ?: "#000000")))
                                    }
                                }
                            }
                        }
                    }
                    
                    if (hasMoreCards && searchQuery.isEmpty()) {
                        val lastVisibleItemIndex = filteredCards.size - 3
                        items(filteredCards.size) { index ->
                            if (index >= lastVisibleItemIndex && !isLoadingMore) {
                                LaunchedEffect(index) {
                                    loadMoreCards()
                                }
                            }
                        }
                        
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExploreSocialIcon(iconRes: Int, color: Color) {
    androidx.compose.foundation.Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        colorFilter = ColorFilter.tint(color)
    )
}

fun parseBackground(card: UserCard): Brush {
    return if (card.backgroundType == "GRADIENT") {
        val allGradients = listOf(
            Pair("Gün Batımı", Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))),
            Pair("Sunset", Brush.horizontalGradient(listOf(Color(0xFFFE6B8B), Color(0xFFFF8E53)))),
            Pair("Okyanus", Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
            Pair("Ocean", Brush.horizontalGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
            Pair("Orman", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))),
            Pair("Forest", Brush.horizontalGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))),
            Pair("Gece", Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))),
            Pair("Night", Brush.verticalGradient(listOf(Color(0xFF2C3E50), Color(0xFF3498DB)))),
            Pair("Mor Sis", Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63)))),
            Pair("Purple Mist", Brush.verticalGradient(listOf(Color(0xFF9C27B0), Color(0xFFE91E63))))
        )
        
        allGradients.firstOrNull { it.first == card.selectedGradient }?.second
            ?: Brush.verticalGradient(listOf(Color.Gray, Color.LightGray))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(android.graphics.Color.parseColor(card.backgroundColor)),
                Color(android.graphics.Color.parseColor(card.backgroundColor))
            )
        )
    }
}

fun parseTextStyle(dto: TextStyleDTO?): androidx.compose.ui.text.TextStyle {
    return androidx.compose.ui.text.TextStyle(
        fontSize = (dto?.fontSize?.sp ?: 16.sp),
        fontWeight = if (dto?.isBold == true) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
        fontStyle = if (dto?.isItalic == true) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
        textDecoration = if (dto?.isUnderlined == true) androidx.compose.ui.text.style.TextDecoration.Underline else androidx.compose.ui.text.style.TextDecoration.None,
        color = Color(android.graphics.Color.parseColor(dto?.color ?: "#000000"))
    )
}

