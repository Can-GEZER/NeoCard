package com.cangzr.neocard.domain.usecase

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.UserCard
import com.cangzr.neocard.data.repository.CardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GetUserCardsUseCase için unit testler
 * MockK ve Kotlin Coroutines Test kullanılarak yazılmıştır
 * 
 * Test Senaryoları:
 * - Başarılı kart listesi getirme
 * - Hata durumları
 * - Boş liste durumu
 * - Pagination senaryoları
 * - hasMore flag kontrolü
 */
class GetUserCardsUseCaseTest {

    // Mock dependencies
    private lateinit var cardRepository: CardRepository
    private lateinit var getUserCardsUseCase: GetUserCardsUseCase

    // Test data
    private val testUserId = "test-user-123"
    private val testCard1 = UserCard(
        id = "card-1",
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
        bio = "Test bio",
        cv = "",
        backgroundType = "SOLID",
        backgroundColor = "#FFFFFF",
        selectedGradient = "Sunset",
        profileImageUrl = "",
        cardType = "Business",
        textStyles = emptyMap(),
        isPublic = true
    )

    private val testCard2 = UserCard(
        id = "card-2",
        name = "Jane",
        surname = "Smith",
        phone = "+9876543210",
        email = "jane.smith@example.com",
        company = "Another Company",
        title = "Designer",
        website = "https://example2.com",
        linkedin = "janesmith",
        instagram = "janesmith",
        twitter = "janesmith",
        facebook = "janesmith",
        github = "janesmith",
        bio = "Designer bio",
        cv = "",
        backgroundType = "GRADIENT",
        backgroundColor = "#000000",
        selectedGradient = "Ocean",
        profileImageUrl = "",
        cardType = "Personal",
        textStyles = emptyMap(),
        isPublic = false
    )

    @Before
    fun setup() {
        // Mock CardRepository
        cardRepository = mockk()
        
        // Initialize use case with mocked repository
        getUserCardsUseCase = GetUserCardsUseCase(cardRepository)
    }

    @Test
    fun `getUserCards returns Success with cards when repository succeeds`() = runTest {
        // Given: Repository mock configured to return success with cards
        val expectedCards = listOf(testCard1, testCard2)
        val expectedLastCardId = "card-2"
        val expectedHasMore = true
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(expectedCards, expectedLastCardId, expectedHasMore))

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should be Success with correct data
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        
        val successResult = result as Resource.Success
        val (cards, lastCardId, hasMore) = successResult.data
        
        assertEquals("Cards list size should match", 2, cards.size)
        assertEquals("First card should match", testCard1, cards[0])
        assertEquals("Second card should match", testCard2, cards[1])
        assertEquals("Last card ID should match", expectedLastCardId, lastCardId)
        assertTrue("hasMore should be true", hasMore)

        // Verify repository was called with correct parameters
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards returns Success with empty list when no cards exist`() = runTest {
        // Given: Repository returns empty list
        val emptyList = emptyList<UserCard>()
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(emptyList, null, false))

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should be Success with empty list
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        
        val successResult = result as Resource.Success
        val (cards, lastCardId, hasMore) = successResult.data
        
        assertTrue("Cards list should be empty", cards.isEmpty())
        assertNull("Last card ID should be null", lastCardId)
        assertFalse("hasMore should be false", hasMore)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards handles pagination with lastCardId parameter`() = runTest {
        // Given: Repository mock for pagination request
        val expectedCards = listOf(testCard2)
        val previousCardId = "card-1"
        val expectedLastCardId = "card-2"
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = previousCardId
            ) 
        } returns Resource.Success(Triple(expectedCards, expectedLastCardId, false))

        // When: GetUserCardsUseCase is invoked with lastCardId
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = previousCardId
        )

        // Then: Result should be Success with paginated data
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        
        val successResult = result as Resource.Success
        val (cards, lastCardId, hasMore) = successResult.data
        
        assertEquals("Should return one card", 1, cards.size)
        assertEquals("Card should match", testCard2, cards[0])
        assertEquals("Last card ID should match", expectedLastCardId, lastCardId)
        assertFalse("hasMore should be false (last page)", hasMore)

        // Verify repository was called with pagination parameter
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = previousCardId
            )
        }
    }

    @Test
    fun `getUserCards returns Error when repository fails`() = runTest {
        // Given: Repository returns error
        val exception = Exception("Network error")
        val errorMessage = "Kartlar yüklenirken hata oluştu"
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Error(
            exception = exception,
            message = errorMessage
        )

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should be Error
        assertTrue("Result should be Resource.Error", result is Resource.Error)
        
        val errorResult = result as Resource.Error
        assertEquals("Error message should match", errorMessage, errorResult.message)
        assertEquals("Exception should match", exception, errorResult.exception)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards returns Error when repository fails with database exception`() = runTest {
        // Given: Repository returns database error
        val exception = DatabaseException("Firestore connection failed")
        val errorMessage = "Veritabanı hatası"
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Error(
            exception = exception,
            message = errorMessage
        )

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should be Error with correct exception type
        assertTrue("Result should be Resource.Error", result is Resource.Error)
        
        val errorResult = result as Resource.Error
        assertTrue(
            "Exception should be DatabaseException",
            errorResult.exception is DatabaseException
        )
        assertEquals("Error message should match", errorMessage, errorResult.message)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards passes through repository Loading state`() = runTest {
        // Given: Repository returns Loading (though unlikely in practice)
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Loading

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should be Loading
        assertTrue("Result should be Resource.Loading", result is Resource.Loading)

        // Verify repository was called
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards uses default pageSize when not specified`() = runTest {
        // Given: Repository mock with default pageSize
        val expectedCards = listOf(testCard1)
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10, // Default value
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(expectedCards, "card-1", false))

        // When: GetUserCardsUseCase is invoked without pageSize parameter
        val result = getUserCardsUseCase(userId = testUserId)

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called with default pageSize
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10, // Verify default value
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards with custom pageSize calls repository with correct parameter`() = runTest {
        // Given: Repository mock with custom pageSize
        val customPageSize = 5
        val expectedCards = listOf(testCard1, testCard2)
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = customPageSize,
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(expectedCards, "card-2", true))

        // When: GetUserCardsUseCase is invoked with custom pageSize
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = customPageSize,
            lastCardId = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called with custom pageSize
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = testUserId,
                pageSize = customPageSize,
                lastCardId = null
            )
        }
    }

    @Test
    fun `getUserCards with hasMore true indicates more cards available`() = runTest {
        // Given: Repository returns data with hasMore = true
        val expectedCards = listOf(testCard1, testCard2)
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 2,
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(expectedCards, "card-2", true))

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 2,
            lastCardId = null
        )

        // Then: Result should indicate more cards are available
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        
        val (_, _, hasMore) = (result as Resource.Success).data
        assertTrue("hasMore should be true when more cards exist", hasMore)
    }

    @Test
    fun `getUserCards with hasMore false indicates no more cards`() = runTest {
        // Given: Repository returns data with hasMore = false
        val expectedCards = listOf(testCard1)
        
        coEvery { 
            cardRepository.getCards(
                userId = testUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(expectedCards, "card-1", false))

        // When: GetUserCardsUseCase is invoked
        val result = getUserCardsUseCase(
            userId = testUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should indicate no more cards are available
        assertTrue("Result should be Resource.Success", result is Resource.Success)
        
        val (_, _, hasMore) = (result as Resource.Success).data
        assertFalse("hasMore should be false when no more cards exist", hasMore)
    }

    @Test
    fun `getUserCards with different userId calls repository with correct parameter`() = runTest {
        // Given: Different user ID
        val differentUserId = "different-user-456"
        val expectedCards = listOf(testCard1)
        
        coEvery { 
            cardRepository.getCards(
                userId = differentUserId,
                pageSize = 10,
                lastCardId = null
            ) 
        } returns Resource.Success(Triple(expectedCards, "card-1", false))

        // When: GetUserCardsUseCase is invoked with different userId
        val result = getUserCardsUseCase(
            userId = differentUserId,
            pageSize = 10,
            lastCardId = null
        )

        // Then: Result should be Success
        assertTrue("Result should be Resource.Success", result is Resource.Success)

        // Verify repository was called with correct userId
        coVerify(exactly = 1) {
            cardRepository.getCards(
                userId = differentUserId,
                pageSize = 10,
                lastCardId = null
            )
        }
    }
}

/**
 * Custom exception for database errors testing
 */
class DatabaseException(message: String) : Exception(message)

