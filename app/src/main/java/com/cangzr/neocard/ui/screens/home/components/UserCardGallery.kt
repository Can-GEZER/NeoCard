package com.cangzr.neocard.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.cangzr.neocard.R
import com.cangzr.neocard.Screen
import com.cangzr.neocard.data.CardType
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.ui.screens.home.viewmodels.HomeViewModel

@Composable
fun UserCardGallery(
    navController: NavHostController,
    filterType: String,
    onCardSelected: (UserCard) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = hiltViewModel()
    
    // Collect paging items
    val userCards = viewModel.userCardsPagingFlow.collectAsLazyPagingItems()
    
    // Remember computed values
    val allFilterText = remember { context.getString(R.string.all) }
    val isAuthenticated = remember { viewModel.isUserAuthenticated() }
    
    // Use derivedStateOf for filtered cards to avoid recomposition
    val filteredCards = remember(filterType, allFilterText) {
        derivedStateOf {
            if (filterType == allFilterText) {
                userCards
            } else {
                // Filter will be applied in UI layer
                userCards
            }
        }
    }.value
    
    UserCardGalleryContent(
        userCards = userCards,
        filterType = filterType,
        allFilterText = allFilterText,
        isAuthenticated = isAuthenticated,
        onCardSelected = onCardSelected,
        onNavigateToAuth = { navController.navigate(Screen.Auth.route) },
        modifier = modifier
    )
}

@Composable
private fun UserCardGalleryContent(
    userCards: LazyPagingItems<UserCard>,
    filterType: String,
    allFilterText: String,
    isAuthenticated: Boolean,
    onCardSelected: (UserCard) -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Filter function with memoization based on filterType
    val shouldShowCard = remember(filterType, allFilterText) {
        { card: UserCard ->
            if (filterType == allFilterText) {
                true
            } else {
                val cardTypeTitle = try {
                    CardType.valueOf(card.cardType).getTitle(context)
                } catch (e: IllegalArgumentException) {
                    card.cardType
                }
                cardTypeTitle == filterType
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            // User not authenticated
            !isAuthenticated -> {
                LoginPromptCard(onNavigateToAuth = onNavigateToAuth)
            }
            
            // Loading first page
            userCards.loadState.refresh is LoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Error loading
            userCards.loadState.refresh is LoadState.Error -> {
                ErrorCard(
                    onRetry = { userCards.retry() }
                )
            }
            
            // Empty state
            userCards.itemCount == 0 -> {
                EmptyStateCard()
            }
            
            // Show cards
            else -> {
                CardsList(
                    userCards = userCards,
                    shouldShowCard = shouldShowCard,
                    onCardSelected = onCardSelected
                )
            }
        }
    }
}

@Composable
private fun LoginPromptCard(
    onNavigateToAuth: () -> Unit
) {
    val context = LocalContext.current
    
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
                onClick = onNavigateToAuth,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(context.getString(R.string.login))
            }
        }
    }
}

@Composable
private fun ErrorCard(
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = context.getString(R.string.error_loading_cards),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) {
                Text(context.getString(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = context.getString(R.string.no_cards_created),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CardsList(
    userCards: LazyPagingItems<UserCard>,
    shouldShowCard: (UserCard) -> Boolean,
    onCardSelected: (UserCard) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        // Items with key for better performance
        items(
            count = userCards.itemCount,
            key = { index ->
                // Use card ID as key for better recomposition performance
                userCards[index]?.id ?: index
            }
        ) { index ->
            userCards[index]?.let { card ->
                // Apply filter
                if (shouldShowCard(card)) {
                    UserCardItem(
                        card = card,
                        onClick = remember { { onCardSelected(card) } }
                    )
                }
            }
        }
        
        // Loading indicator for pagination
        if (userCards.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        
        // Error indicator for pagination
        if (userCards.loadState.append is LoadState.Error) {
            item {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = { userCards.retry() }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
