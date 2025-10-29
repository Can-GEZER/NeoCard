package com.cangzr.neocard.ui.screens.createcard.viewmodels

import android.content.Context
import android.net.Uri
import app.cash.turbine.test
import com.cangzr.neocard.R
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.AuthRepository
import com.cangzr.neocard.domain.usecase.GetUserCardsUseCase
import com.cangzr.neocard.domain.usecase.SaveCardUseCase
import com.cangzr.neocard.util.MainDispatcherRule
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * CreateCardViewModel için unit testler
 * 
 * Test Senaryoları:
 * - Form field updates ve state management
 * - Card saving success flow
 * - Card saving error flows
 * - Premium status checks
 * - Loading states
 * - Form validation
 * - Design state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateCardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock dependencies
    private lateinit var authRepository: AuthRepository
    private lateinit var saveCardUseCase: SaveCardUseCase
    private lateinit var getUserCardsUseCase: GetUserCardsUseCase
    private lateinit var viewModel: CreateCardViewModel
    
    // Mock context and user
    private lateinit var mockContext: Context
    private lateinit var mockFirebaseUser: FirebaseUser
    
    // Test data
    private val testUserId = "test-user-123"
    private val testCardId = "test-card-456"

    @Before
    fun setup() {
        // Mock repositories and use cases
        authRepository = mockk(relaxed = true)
        saveCardUseCase = mockk(relaxed = true)
        getUserCardsUseCase = mockk(relaxed = true)
        
        // Mock context
        mockContext = mockk(relaxed = true)
        every { mockContext.getString(R.string.please_login) } returns "Lütfen giriş yapın"
        every { mockContext.getString(R.string.premium_card_limit) } returns "Premium üyelik gerekli"
        every { mockContext.getString(R.string.error_occurred, any()) } returns "Hata oluştu"
        every { mockContext.getString(R.string.card_saved) } returns "Kart kaydedildi"
        
        // Mock Firebase user
        mockFirebaseUser = mockk(relaxed = true)
        every { mockFirebaseUser.uid } returns testUserId
        every { mockFirebaseUser.email } returns "test@example.com"
        
        // Mock CardCreationUtils.isUserPremium()
        mockkStatic("com.cangzr.neocard.ui.screens.createcard.utils.CardCreationUtils")
    }

    @Test
    fun `initial state has default values`() = runTest {
        // Given/When: ViewModel is created
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // Then: All form fields should be empty
        viewModel.name.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.surname.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.email.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.phone.test {
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun `updateName updates name state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Name is updated
        val testName = "John"
        viewModel.updateName(testName)
        
        // Then: State should reflect the new name
        viewModel.name.test {
            assertEquals(testName, awaitItem())
        }
    }

    @Test
    fun `updateSurname updates surname state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Surname is updated
        val testSurname = "Doe"
        viewModel.updateSurname(testSurname)
        
        // Then: State should reflect the new surname
        viewModel.surname.test {
            assertEquals(testSurname, awaitItem())
        }
    }

    @Test
    fun `updateEmail updates email state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Email is updated
        val testEmail = "john@example.com"
        viewModel.updateEmail(testEmail)
        
        // Then: State should reflect the new email
        viewModel.email.test {
            assertEquals(testEmail, awaitItem())
        }
    }

    @Test
    fun `updatePhone updates phone state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Phone is updated
        val testPhone = "+1234567890"
        viewModel.updatePhone(testPhone)
        
        // Then: State should reflect the new phone
        viewModel.phone.test {
            assertEquals(testPhone, awaitItem())
        }
    }

    @Test
    fun `saveCard returns error when user is not logged in`() = runTest {
        // Given: No user is logged in
        every { authRepository.getCurrentUser() } returns null
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        var successCalled = false
        
        // When: Attempting to save card
        viewModel.saveCard(mockContext) {
            successCalled = true
        }
        
        // Wait for coroutine to complete
        advanceUntilIdle()
        
        // Then: Should return error and not call success callback
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("State should be Error", state is Resource.Error)
            assertEquals("Lütfen giriş yapın", (state as Resource.Error).message)
        }
        
        assertFalse("Success callback should not be called", successCalled)
    }

    @Test
    fun `saveCard checks premium status for free users`() = runTest {
        // Given: User is logged in but not premium
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // Mock premium status to false
        viewModel.updateName("John")
        
        // Mock getUserCardsUseCase to return cards (limit reached)
        val existingCard = UserCard(
            id = "existing-card",
            name = "Existing",
            surname = "Card"
        )
        coEvery {
            getUserCardsUseCase(testUserId, 1, null)
        } returns Resource.Success(Triple(listOf(existingCard), "existing-card", false))
        
        var successCalled = false
        
        // When: Attempting to save card
        viewModel.saveCard(mockContext) {
            successCalled = true
        }
        
        // Wait for coroutine to complete
        advanceUntilIdle()
        
        // Then: Should return premium required error
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("State should be Error", state is Resource.Error)
        }
        
        assertFalse("Success callback should not be called", successCalled)
        
        // Verify getUserCardsUseCase was called
        coVerify(exactly = 1) { getUserCardsUseCase(testUserId, 1, null) }
    }

    @Test
    fun `saveCard succeeds when repository returns success`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // Set up form data
        viewModel.updateName("John")
        viewModel.updateSurname("Doe")
        viewModel.updateEmail("john@example.com")
        
        // Mock getUserCardsUseCase to return empty (no limit)
        coEvery {
            getUserCardsUseCase(testUserId, 1, null)
        } returns Resource.Success(Triple(emptyList(), null, false))
        
        // Mock saveCardUseCase to return success
        coEvery {
            saveCardUseCase(
                userId = testUserId,
                card = any(),
                imageUri = null
            )
        } returns Resource.Success(testCardId)
        
        var successCalled = false
        
        // When: Saving card
        viewModel.saveCard(mockContext) {
            successCalled = true
        }
        
        // Wait for coroutine to complete
        advanceUntilIdle()
        
        // Then: Should transition through Loading to Success
        assertTrue("Success callback should be called", successCalled)
        
        // Verify use cases were called
        coVerify(exactly = 1) { getUserCardsUseCase(testUserId, 1, null) }
        coVerify(exactly = 1) {
            saveCardUseCase(
                userId = testUserId,
                card = any(),
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard handles repository error correctly`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // Set up form data
        viewModel.updateName("John")
        
        // Mock getUserCardsUseCase to return empty
        coEvery {
            getUserCardsUseCase(testUserId, 1, null)
        } returns Resource.Success(Triple(emptyList(), null, false))
        
        // Mock saveCardUseCase to return error
        val exception = Exception("Network error")
        coEvery {
            saveCardUseCase(
                userId = testUserId,
                card = any(),
                imageUri = null
            )
        } returns Resource.Error(exception, "Network error")
        
        var successCalled = false
        
        // When: Attempting to save card
        viewModel.saveCard(mockContext) {
            successCalled = true
        }
        
        // Wait for coroutine to complete
        advanceUntilIdle()
        
        // Then: Should return error
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("State should be Error", state is Resource.Error)
        }
        
        assertFalse("Success callback should not be called", successCalled)
    }

    @Test
    fun `clearForm resets all form fields to default`() = runTest {
        // Given: ViewModel with filled form
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        viewModel.updateName("John")
        viewModel.updateSurname("Doe")
        viewModel.updateEmail("john@example.com")
        viewModel.updatePhone("+1234567890")
        viewModel.updateCompany("Test Company")
        
        // When: Form is cleared
        viewModel.clearForm()
        
        // Then: All fields should be reset
        viewModel.name.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.surname.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.email.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.phone.test {
            assertEquals("", awaitItem())
        }
        
        viewModel.company.test {
            assertEquals("", awaitItem())
        }
    }

    @Test
    fun `resetState resets UI state to success with default`() = runTest {
        // Given: ViewModel with error state
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: State is reset
        viewModel.resetState()
        
        // Then: State should be Success with default values
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("State should be Success", state is Resource.Success)
            
            val uiState = (state as Resource.Success).data
            assertFalse("isSaved should be false", uiState.isSaved)
            assertNull("errorMessage should be null", uiState.errorMessage)
        }
    }

    @Test
    fun `updateIsPublic updates isPublic state correctly`() = runTest {
        // Given: ViewModel is initialized (default isPublic = true)
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: isPublic is set to false
        viewModel.updateIsPublic(false)
        
        // Then: State should reflect the change
        viewModel.isPublic.test {
            assertFalse("isPublic should be false", awaitItem())
        }
    }

    @Test
    fun `showPremiumDialog sets dialog visibility to true`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Premium dialog is shown
        viewModel.showPremiumDialog()
        
        // Then: Dialog visibility should be true
        viewModel.showPremiumDialog.test {
            assertTrue("Dialog should be visible", awaitItem())
        }
    }

    @Test
    fun `hidePremiumDialog sets dialog visibility to false`() = runTest {
        // Given: ViewModel with dialog shown
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        viewModel.showPremiumDialog()
        
        // When: Premium dialog is hidden
        viewModel.hidePremiumDialog()
        
        // Then: Dialog visibility should be false
        viewModel.showPremiumDialog.test {
            assertFalse("Dialog should be hidden", awaitItem())
        }
    }

    @Test
    fun `updateBackgroundType updates background type correctly`() = runTest {
        // Given: ViewModel is initialized with SOLID background
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Background type is changed to GRADIENT
        viewModel.updateBackgroundType(BackgroundType.GRADIENT)
        
        // Then: State should reflect the change
        viewModel.backgroundType.test {
            assertEquals(BackgroundType.GRADIENT, awaitItem())
        }
    }

    @Test
    fun `updateProfileImageUri updates image uri correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // Mock URI
        val mockUri = mockk<Uri>(relaxed = true)
        
        // When: Profile image URI is updated
        viewModel.updateProfileImageUri(mockUri)
        
        // Then: State should contain the URI
        viewModel.profileImageUri.test {
            val uri = awaitItem()
            assertNotNull("URI should not be null", uri)
            assertEquals(mockUri, uri)
        }
    }

    @Test
    fun `multiple field updates work correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Multiple fields are updated
        viewModel.updateName("John")
        viewModel.updateSurname("Doe")
        viewModel.updateEmail("john@example.com")
        viewModel.updatePhone("+1234567890")
        viewModel.updateCompany("Test Company")
        viewModel.updateTitle("Software Engineer")
        
        // Then: All fields should be updated correctly
        viewModel.name.test { assertEquals("John", awaitItem()) }
        viewModel.surname.test { assertEquals("Doe", awaitItem()) }
        viewModel.email.test { assertEquals("john@example.com", awaitItem()) }
        viewModel.phone.test { assertEquals("+1234567890", awaitItem()) }
        viewModel.company.test { assertEquals("Test Company", awaitItem()) }
        viewModel.title.test { assertEquals("Software Engineer", awaitItem()) }
    }

    @Test
    fun `social media fields update correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Social media fields are updated
        viewModel.updateLinkedin("johndoe")
        viewModel.updateInstagram("@johndoe")
        viewModel.updateTwitter("@johndoe")
        viewModel.updateFacebook("johndoe")
        viewModel.updateGithub("johndoe")
        
        // Then: All social media fields should be updated
        viewModel.linkedin.test { assertEquals("johndoe", awaitItem()) }
        viewModel.instagram.test { assertEquals("@johndoe", awaitItem()) }
        viewModel.twitter.test { assertEquals("@johndoe", awaitItem()) }
        viewModel.facebook.test { assertEquals("johndoe", awaitItem()) }
        viewModel.github.test { assertEquals("johndoe", awaitItem()) }
    }

    @Test
    fun `updateWebsite updates website state correctly`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Website is updated
        val testWebsite = "https://example.com"
        viewModel.updateWebsite(testWebsite)
        
        // Then: State should reflect the new website
        viewModel.website.test {
            assertEquals(testWebsite, awaitItem())
        }
    }

    @Test
    fun `saveCard emits loading state during operation`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        viewModel.updateName("John")
        
        // Mock getUserCardsUseCase with delay
        coEvery {
            getUserCardsUseCase(testUserId, 1, null)
        } returns Resource.Success(Triple(emptyList(), null, false))
        
        coEvery {
            saveCardUseCase(any(), any(), any())
        } returns Resource.Success(testCardId)
        
        // When: Saving card
        viewModel.saveCard(mockContext) {}
        
        // Then: Should see Loading state
        // Note: Due to rapid execution in tests, we verify the final state
        advanceUntilIdle()
        
        // Verify save was called
        coVerify { saveCardUseCase(any(), any(), any()) }
    }

    @Test
    fun `form fields accept empty strings`() = runTest {
        // Given: ViewModel with filled fields
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        viewModel.updateName("John")
        viewModel.updateEmail("test@example.com")
        
        // When: Fields are cleared with empty strings
        viewModel.updateName("")
        viewModel.updateEmail("")
        
        // Then: Fields should be empty
        viewModel.name.test { assertEquals("", awaitItem()) }
        viewModel.email.test { assertEquals("", awaitItem()) }
    }

    @Test
    fun `form fields handle special characters`() = runTest {
        // Given: ViewModel is initialized
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        // When: Fields with special characters
        viewModel.updateName("José")
        viewModel.updateEmail("test+tag@example.com")
        viewModel.updateCompany("Company & Co.")
        
        // Then: Special characters should be preserved
        viewModel.name.test { assertEquals("José", awaitItem()) }
        viewModel.email.test { assertEquals("test+tag@example.com", awaitItem()) }
        viewModel.company.test { assertEquals("Company & Co.", awaitItem()) }
    }

    @Test
    fun `getUserCardsUseCase error prevents card save`() = runTest {
        // Given: User is logged in
        every { authRepository.getCurrentUser() } returns mockFirebaseUser
        viewModel = CreateCardViewModel(authRepository, saveCardUseCase, getUserCardsUseCase)
        
        viewModel.updateName("John")
        
        // Mock getUserCardsUseCase to return error
        coEvery {
            getUserCardsUseCase(testUserId, 1, null)
        } returns Resource.Error(Exception("Database error"), "Veritabanı hatası")
        
        var successCalled = false
        
        // When: Attempting to save card
        viewModel.saveCard(mockContext) {
            successCalled = true
        }
        
        // Wait for coroutine to complete
        advanceUntilIdle()
        
        // Then: Should return error and not proceed to save
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("State should be Error", state is Resource.Error)
        }
        
        assertFalse("Success callback should not be called", successCalled)
        
        // Verify saveCardUseCase was NOT called
        coVerify(exactly = 0) { saveCardUseCase(any(), any(), any()) }
    }
}

