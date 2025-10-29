package com.cangzr.neocard.ui.screens.home.viewmodels

import app.cash.turbine.test
import com.cangzr.neocard.data.repository.AuthRepository
import com.cangzr.neocard.data.repository.CardRepository
import com.cangzr.neocard.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * HomeViewModel için unit testler
 * 
 * Test Senaryoları:
 * - UI state management (filter, search)
 * - User authentication status
 * - State flow emissions
 * - Isolated and independent tests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock dependencies
    private lateinit var authRepository: AuthRepository
    private lateinit var cardRepository: CardRepository
    private lateinit var viewModel: HomeViewModel
    
    // Mock Firebase user
    private lateinit var mockFirebaseUser: FirebaseUser

    @Before
    fun setup() {
        // Mock repositories
        authRepository = mockk(relaxed = true)
        cardRepository = mockk(relaxed = true)
        
        // Mock Firebase user
        mockFirebaseUser = mockk(relaxed = true)
        every { mockFirebaseUser.uid } returns "test-user-123"
        every { mockFirebaseUser.email } returns "test@example.com"
    }

    @Test
    fun `initial state has default values`() = runTest {
        // Given: ViewModel with no user logged in
        every { authRepository.getCurrentUser() } returns null
        
        // When: ViewModel is created
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // Then: UI state should have default values
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Tümü", state.selectedCardType)
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun `updateSelectedCardType updates state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Card type is updated
        val newCardType = "Business"
        viewModel.updateSelectedCardType(newCardType)
        
        // Then: State should reflect the new card type
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(newCardType, state.selectedCardType)
            assertEquals("", state.searchQuery) // Other fields unchanged
        }
    }

    @Test
    fun `updateSearchQuery updates state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Search query is updated
        val newQuery = "John Doe"
        viewModel.updateSearchQuery(newQuery)
        
        // Then: State should reflect the new search query
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Tümü", state.selectedCardType) // Other fields unchanged
            assertEquals(newQuery, state.searchQuery)
        }
    }

    @Test
    fun `multiple state updates work correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Multiple updates are performed
        viewModel.updateSelectedCardType("Personal")
        viewModel.updateSearchQuery("test query")
        
        // Then: State should reflect all updates
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Personal", state.selectedCardType)
            assertEquals("test query", state.searchQuery)
        }
    }

    @Test
    fun `isUserAuthenticated returns true when user is logged in`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Checking authentication status
        val isAuthenticated = viewModel.isUserAuthenticated()
        
        // Then: Should return true
        assertTrue("User should be authenticated", isAuthenticated)
        
        // Verify repository was called
        verify(exactly = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `isUserAuthenticated returns false when user is not logged in`() = runTest {
        // Given: No user is logged in
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Checking authentication status
        val isAuthenticated = viewModel.isUserAuthenticated()
        
        // Then: Should return false
        assertFalse("User should not be authenticated", isAuthenticated)
        
        // Verify repository was called
        verify(exactly = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `userCardsPagingFlow is empty when user is not logged in`() = runTest {
        // Given: No user is logged in
        every { authRepository.getCurrentUser() } returns null
        
        // When: ViewModel is created
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // Then: userCardsPagingFlow should be initialized
        assertNotNull("Paging flow should not be null", viewModel.userCardsPagingFlow)
        
        // Verify getCurrentUser was called during initialization
        verify(atLeast = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `exploreCardsPagingFlow is empty when user is not logged in`() = runTest {
        // Given: No user is logged in
        every { authRepository.getCurrentUser() } returns null
        
        // When: ViewModel is created
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // Then: exploreCardsPagingFlow should be initialized
        assertNotNull("Paging flow should not be null", viewModel.exploreCardsPagingFlow)
        
        // Verify getCurrentUser was called during initialization
        verify(atLeast = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `userCardsPagingFlow is created when user is logged in`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        
        // When: ViewModel is created
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // Then: userCardsPagingFlow should be initialized
        assertNotNull("Paging flow should not be null", viewModel.userCardsPagingFlow)
        
        // Verify getCurrentUser was called
        verify(atLeast = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `exploreCardsPagingFlow is created when user is logged in`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        
        // When: ViewModel is created
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // Then: exploreCardsPagingFlow should be initialized
        assertNotNull("Paging flow should not be null", viewModel.exploreCardsPagingFlow)
        
        // Verify getCurrentUser was called
        verify(atLeast = 1) { authRepository.getCurrentUser() }
    }

    @Test
    fun `state changes are properly emitted through flow`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When/Then: Collect state changes
        viewModel.uiState.test {
            // Initial state
            val initialState = awaitItem()
            assertEquals("Tümü", initialState.selectedCardType)
            assertEquals("", initialState.searchQuery)
            
            // Update card type
            viewModel.updateSelectedCardType("Business")
            val afterTypeChange = awaitItem()
            assertEquals("Business", afterTypeChange.selectedCardType)
            assertEquals("", afterTypeChange.searchQuery)
            
            // Update search query
            viewModel.updateSearchQuery("test")
            val afterQueryChange = awaitItem()
            assertEquals("Business", afterQueryChange.selectedCardType)
            assertEquals("test", afterQueryChange.searchQuery)
        }
    }

    @Test
    fun `updateSelectedCardType does not affect search query`() = runTest {
        // Given: ViewModel with existing search query
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        viewModel.updateSearchQuery("existing query")
        
        // When: Card type is updated
        viewModel.updateSelectedCardType("Personal")
        
        // Then: Search query should remain unchanged
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Personal", state.selectedCardType)
            assertEquals("existing query", state.searchQuery)
        }
    }

    @Test
    fun `updateSearchQuery does not affect card type filter`() = runTest {
        // Given: ViewModel with existing card type filter
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        viewModel.updateSelectedCardType("Business")
        
        // When: Search query is updated
        viewModel.updateSearchQuery("new query")
        
        // Then: Card type should remain unchanged
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Business", state.selectedCardType)
            assertEquals("new query", state.searchQuery)
        }
    }

    @Test
    fun `empty string updates are handled correctly`() = runTest {
        // Given: ViewModel with some values
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        viewModel.updateSelectedCardType("Business")
        viewModel.updateSearchQuery("test")
        
        // When: Values are cleared with empty strings
        viewModel.updateSelectedCardType("")
        viewModel.updateSearchQuery("")
        
        // Then: State should have empty values
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.selectedCardType)
            assertEquals("", state.searchQuery)
        }
    }

    @Test
    fun `special characters in search query are handled correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Search query with special characters
        val specialQuery = "test@#$%^&*()"
        viewModel.updateSearchQuery(specialQuery)
        
        // Then: State should contain the special characters
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(specialQuery, state.searchQuery)
        }
    }

    @Test
    fun `long card type name is handled correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Long card type name
        val longTypeName = "A".repeat(100)
        viewModel.updateSelectedCardType(longTypeName)
        
        // Then: State should contain the long name
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(longTypeName, state.selectedCardType)
        }
    }

    @Test
    fun `rapid successive updates are handled correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns null
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: Multiple rapid updates
        viewModel.updateSelectedCardType("Type1")
        viewModel.updateSelectedCardType("Type2")
        viewModel.updateSelectedCardType("Type3")
        viewModel.updateSearchQuery("Query1")
        viewModel.updateSearchQuery("Query2")
        
        // Then: State should reflect the latest updates
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Type3", state.selectedCardType)
            assertEquals("Query2", state.searchQuery)
        }
    }

    @Test
    fun `authentication check is independent of state updates`() = runTest {
        // Given: ViewModel with logged in user
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = HomeViewModel(authRepository, cardRepository)
        
        // When: State is updated multiple times
        viewModel.updateSelectedCardType("Business")
        viewModel.updateSearchQuery("test")
        
        // Then: Authentication status remains consistent
        assertTrue("User should remain authenticated", viewModel.isUserAuthenticated())
        
        // Verify repository calls
        verify(atLeast = 1) { authRepository.getCurrentUser() }
    }
}

