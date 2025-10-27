package com.cangzr.neocard.ui.screens.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.paging.CardPagingSource
import com.cangzr.neocard.data.paging.ExploreCardPagingSource
import com.cangzr.neocard.data.repository.AuthRepository
import com.cangzr.neocard.data.repository.CardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

/**
 * UI State for filter and search options
 */
data class HomeUiState(
    val selectedCardType: String = "Tümü",
    val searchQuery: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cardRepository: CardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val pageSize = 10
    
    /**
     * PagingData flow for user's cards
     * Uses Paging3 to handle pagination automatically
     */
    val userCardsPagingFlow: Flow<PagingData<UserCard>> = run {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            Pager(
                config = PagingConfig(
                    pageSize = pageSize,
                    enablePlaceholders = false,
                    initialLoadSize = pageSize
                ),
                pagingSourceFactory = {
                    CardPagingSource(
                        userId = currentUser.uid,
                        cardRepository = cardRepository
                    )
                }
            ).flow.cachedIn(viewModelScope)
        } else {
            emptyFlow()
        }
    }
    
    /**
     * PagingData flow for explore cards
     * Uses Paging3 to handle pagination automatically
     */
    val exploreCardsPagingFlow: Flow<PagingData<UserCard>> = run {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser != null) {
            Pager(
                config = PagingConfig(
                    pageSize = pageSize,
                    enablePlaceholders = false,
                    initialLoadSize = pageSize
                ),
                pagingSourceFactory = {
                    ExploreCardPagingSource(
                        currentUserId = currentUser.uid,
                        cardRepository = cardRepository
                    )
                }
            ).flow.cachedIn(viewModelScope)
        } else {
            emptyFlow()
        }
    }
    
    /**
     * Update selected card type filter
     */
    fun updateSelectedCardType(cardType: String) {
        _uiState.value = _uiState.value.copy(selectedCardType = cardType)
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    /**
     * Check if user is authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return authRepository.getCurrentUser() != null
    }
}
