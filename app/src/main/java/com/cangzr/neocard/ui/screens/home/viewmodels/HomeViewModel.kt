package com.cangzr.neocard.ui.screens.home.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.ui.screens.home.utils.loadCards
import com.cangzr.neocard.ui.screens.home.utils.loadExploreCards
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

data class HomeUiState(
    val cards: List<UserCard> = emptyList(),
    val exploreCards: List<UserCard> = emptyList(),
    val isLoading: Boolean = true,
    val isExploreLoading: Boolean = true,
    val selectedCardType: String = "Tümü",
    val searchQuery: String = "",
    val lastCardId: String? = null,
    val lastExploreCardId: String? = null,
    val hasMoreCards: Boolean = true,
    val hasMoreExploreCards: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isLoadingMoreExplore: Boolean = false
)

class HomeViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    var uiState by mutableStateOf(HomeUiState())
        private set
    
    private val pageSize = 10
    
    init {
        loadInitialData()
    }
    
    private fun loadInitialData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserCards()
            loadExploreCards()
        } else {
            uiState = uiState.copy(isLoading = false, isExploreLoading = false)
        }
    }
    
    fun loadUserCards() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            loadCards(
                userId = currentUser.uid,
                firestore = firestore,
                pageSize = pageSize,
                lastCardId = null,
                onSuccess = { newCards, lastId, hasMore ->
                    uiState = uiState.copy(
                        cards = newCards,
                        lastCardId = lastId,
                        hasMoreCards = hasMore,
                        isLoading = false
                    )
                },
                onError = {
                    uiState = uiState.copy(isLoading = false)
                }
            )
        }
    }
    
    fun loadExploreCards() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            loadExploreCards(
                firestore = firestore,
                currentUserId = currentUser.uid,
                pageSize = pageSize,
                lastCardId = null,
                onSuccess = { newCards, lastId, hasMore ->
                    uiState = uiState.copy(
                        exploreCards = newCards,
                        lastExploreCardId = lastId,
                        hasMoreExploreCards = hasMore,
                        isExploreLoading = false
                    )
                },
                onError = {
                    uiState = uiState.copy(isExploreLoading = false)
                }
            )
        }
    }
    
    fun loadMoreUserCards() {
        if (!uiState.hasMoreCards || uiState.isLoadingMore) return
        
        val currentUser = auth.currentUser ?: return
        
        uiState = uiState.copy(isLoadingMore = true)
        
        viewModelScope.launch {
            loadCards(
                userId = currentUser.uid,
                firestore = firestore,
                pageSize = pageSize,
                lastCardId = uiState.lastCardId,
                onSuccess = { newCards, lastId, hasMore ->
                    uiState = uiState.copy(
                        cards = uiState.cards + newCards,
                        lastCardId = lastId,
                        hasMoreCards = hasMore,
                        isLoadingMore = false
                    )
                },
                onError = {
                    uiState = uiState.copy(isLoadingMore = false)
                }
            )
        }
    }
    
    fun loadMoreExploreCards() {
        if (!uiState.hasMoreExploreCards || uiState.isLoadingMoreExplore) return
        
        val currentUser = auth.currentUser ?: return
        
        uiState = uiState.copy(isLoadingMoreExplore = true)
        
        viewModelScope.launch {
            loadExploreCards(
                firestore = firestore,
                currentUserId = currentUser.uid,
                pageSize = pageSize,
                lastCardId = uiState.lastExploreCardId,
                onSuccess = { newCards, lastId, hasMore ->
                    uiState = uiState.copy(
                        exploreCards = uiState.exploreCards + newCards,
                        lastExploreCardId = lastId,
                        hasMoreExploreCards = hasMore,
                        isLoadingMoreExplore = false
                    )
                },
                onError = {
                    uiState = uiState.copy(isLoadingMoreExplore = false)
                }
            )
        }
    }
    
    fun updateSelectedCardType(cardType: String) {
        uiState = uiState.copy(selectedCardType = cardType)
    }
    
    fun updateSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }
    
    fun getFilteredCards(): List<UserCard> {
        return if (uiState.selectedCardType == "Tümü") {
            uiState.cards
        } else {
            uiState.cards.filter { it.cardType == uiState.selectedCardType }
        }
    }
}
