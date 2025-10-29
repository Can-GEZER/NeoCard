package com.cangzr.neocard.domain.usecase

import android.net.Uri
import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SaveCardUseCase için unit testler
 * MockK ve Kotlin Coroutines Test kullanılarak yazılmıştır
 */
class SaveCardUseCaseTest {

    // Mock dependencies
    private lateinit var cardRepository: CardRepository
    private lateinit var saveCardUseCase: SaveCardUseCase

    // Test data
    private val testUserId = "test-user-123"
    private val testCardId = "test-card-456"
    private val testCard = UserCard(
        id = "",
        name = "John",
        surname = "Doe",
        phone = "+1234567890",
        email = "john.doe@example.com",
        company = "Test Company",
        title = "Software Engineer",
        website = "https://example.com",
        linkedin = "johndoe",
        instagram = "johndoe",
        twitter = "johndoe",
        facebook = "johndoe",
        github = "johndoe",
        bio = "",
        cv = "",
        backgroundType = "SOLID",
        backgroundColor = "#FFFFFF",
        selectedGradient = "Sunset",
        profileImageUrl = "",
        cardType = "Business",
        textStyles = emptyMap(),
        isPublic = true
    )

    @Before
    fun setup() {
        // Mock CardRepository
        cardRepository = mockk()
        
        // Initialize use case with mocked repository
        saveCardUseCase = SaveCardUseCase(cardRepository)
    }

    @Test
    fun `saveCard returns Success when repository succeeds`() = runTest {
        // Given: Repository mock configured to return success
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked
        val result = saveCardUseCase(
            userId = testUserId,
            card = testCard,
            imageUri = null
        )

        // Then: Result should be Success with correct card ID
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        assertEquals(
            "Card ID should match",
            testCardId,
            (result as Resource.Success).data
        )

        // Verify repository was called with correct parameters
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard returns Error when repository fails with PremiumRequiredException`() = runTest {
        // Given: Custom exception for premium requirement
        val exception = PremiumRequiredException("Premium subscription required")
        val errorMessage = "Premium üyelik gerekli"

        // Mock repository to return error
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            ) 
        } returns Resource.Error(
            exception = exception,
            message = errorMessage
        )

        // When: SaveCardUseCase is invoked
        val result = saveCardUseCase(
            userId = testUserId,
            card = testCard,
            imageUri = null
        )

        // Then: Result should be Error
        assertTrue("Result should be Resource.Error", result is Resource.Error)
        
        val errorResult = result as Resource.Error
        assertEquals(
            "Error message should match",
            errorMessage,
            errorResult.message
        )
        assertTrue(
            "Exception should be PremiumRequiredException",
            errorResult.exception is PremiumRequiredException
        )

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with image URI returns Success when repository succeeds`() = runTest {
        // Given: Mock URI and repository success
        val mockUri: Uri = mockk(relaxed = true)
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = mockUri
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with image URI
        val result = saveCardUseCase(
            userId = testUserId,
            card = testCard,
            imageUri = mockUri
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        assertEquals(
            "Card ID should match",
            testCardId,
            (result as Resource.Success).data
        )

        // Verify repository was called with image URI
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = mockUri
            )
        }
    }

    @Test
    fun `saveCard returns Error when repository fails with generic exception`() = runTest {
        // Given: Generic exception
        val exception = Exception("Network error")
        val errorMessage = "Bağlantı hatası oluştu"

        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            ) 
        } returns Resource.Error(
            exception = exception,
            message = errorMessage
        )

        // When: SaveCardUseCase is invoked
        val result = saveCardUseCase(
            userId = testUserId,
            card = testCard,
            imageUri = null
        )

        // Then: Result should be Error
        assertTrue("Result should be Resource.Error", result is Resource.Error)
        
        val errorResult = result as Resource.Error
        assertEquals(
            "Error message should match",
            errorMessage,
            errorResult.message
        )

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard passes through repository Loading state`() = runTest {
        // Given: Repository returns Loading (though unlikely in practice)
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            ) 
        } returns Resource.Loading

        // When: SaveCardUseCase is invoked
        val result = saveCardUseCase(
            userId = testUserId,
            card = testCard,
            imageUri = null
        )

        // Then: Result should be Loading
        assertTrue("Result should be Resource.Loading", result is Resource.Loading)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with different card type returns Success`() = runTest {
        // Given: Card with Personal card type
        val personalCard = testCard.copy(cardType = "Personal")
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = personalCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with Personal card
        val result = saveCardUseCase(
            userId = testUserId,
            card = personalCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        assertEquals(
            "Card ID should match",
            testCardId,
            (result as Resource.Success).data
        )

        // Verify repository was called with Personal card
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = personalCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with gradient background returns Success`() = runTest {
        // Given: Card with gradient background
        val gradientCard = testCard.copy(
            backgroundType = "GRADIENT",
            selectedGradient = "Ocean",
            backgroundColor = "#000000"
        )
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = gradientCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with gradient card
        val result = saveCardUseCase(
            userId = testUserId,
            card = gradientCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called with gradient card
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = gradientCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with private card returns Success`() = runTest {
        // Given: Private card (isPublic = false)
        val privateCard = testCard.copy(isPublic = false)
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = privateCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with private card
        val result = saveCardUseCase(
            userId = testUserId,
            card = privateCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = privateCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with minimal data returns Success`() = runTest {
        // Given: Card with only required fields
        val minimalCard = UserCard(
            id = "",
            name = "John",
            surname = "Doe",
            phone = "",
            email = "",
            company = "",
            title = "",
            website = "",
            linkedin = "",
            instagram = "",
            twitter = "",
            facebook = "",
            github = "",
            bio = "",
            cv = "",
            backgroundType = "SOLID",
            backgroundColor = "#FFFFFF",
            selectedGradient = "",
            profileImageUrl = "",
            cardType = "Genel",
            textStyles = emptyMap(),
            isPublic = true
        )
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = minimalCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with minimal card
        val result = saveCardUseCase(
            userId = testUserId,
            card = minimalCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = minimalCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with image URI returns Error when repository fails`() = runTest {
        // Given: Mock URI and repository error
        val mockUri: Uri = mockk(relaxed = true)
        val exception = Exception("Image upload failed")
        val errorMessage = "Resim yüklenemedi"
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = mockUri
            ) 
        } returns Resource.Error(
            exception = exception,
            message = errorMessage
        )

        // When: SaveCardUseCase is invoked with image URI
        val result = saveCardUseCase(
            userId = testUserId,
            card = testCard,
            imageUri = mockUri
        )

        // Then: Result should be Error
        assertTrue("Result should be Resource.Error", result is Resource.Error)
        
        val errorResult = result as Resource.Error
        assertEquals("Error message should match", errorMessage, errorResult.message)

        // Verify repository was called with image URI
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = testCard,
                imageUri = mockUri
            )
        }
    }

    @Test
    fun `saveCard with different userId calls repository correctly`() = runTest {
        // Given: Different user ID
        val differentUserId = "different-user-789"
        
        coEvery { 
            cardRepository.saveCard(
                userId = differentUserId,
                card = testCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with different userId
        val result = saveCardUseCase(
            userId = differentUserId,
            card = testCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called with correct userId
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = differentUserId,
                card = testCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with complete social media links returns Success`() = runTest {
        // Given: Card with all social media fields filled
        val socialCard = testCard.copy(
            linkedin = "https://linkedin.com/in/johndoe",
            instagram = "@johndoe",
            twitter = "@johndoe",
            facebook = "johndoe",
            github = "johndoe"
        )
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = socialCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with complete social media card
        val result = saveCardUseCase(
            userId = testUserId,
            card = socialCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = socialCard,
                imageUri = null
            )
        }
    }

    @Test
    fun `saveCard with custom text styles returns Success`() = runTest {
        // Given: Card with custom text styles
        val textStylesMap = mapOf(
            "name" to com.cangzr.neocard.data.model.TextStyleDTO(
                fontSize = 20f,
                isBold = true,
                isItalic = false,
                isUnderlined = false,
                color = "#000000"
            ),
            "title" to com.cangzr.neocard.data.model.TextStyleDTO(
                fontSize = 16f,
                isBold = false,
                isItalic = true,
                isUnderlined = false,
                color = "#666666"
            )
        )
        val styledCard = testCard.copy(textStyles = textStylesMap)
        
        coEvery { 
            cardRepository.saveCard(
                userId = testUserId,
                card = styledCard,
                imageUri = null
            ) 
        } returns Resource.Success(testCardId)

        // When: SaveCardUseCase is invoked with styled card
        val result = saveCardUseCase(
            userId = testUserId,
            card = styledCard,
            imageUri = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.saveCard(
                userId = testUserId,
                card = styledCard,
                imageUri = null
            )
        }
    }
}

/**
 * Custom exception for premium requirement testing
 */
class PremiumRequiredException(message: String) : Exception(message)

