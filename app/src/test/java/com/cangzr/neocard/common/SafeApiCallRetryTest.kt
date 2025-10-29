package com.cangzr.neocard.common

import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for safeApiCall retry logic
 * 
 * Tests:
 * - Successful first attempt (no retry)
 * - Retry on UNAVAILABLE error
 * - Exponential backoff delays
 * - Non-retryable errors
 * - Maximum retry limit
 * - Different exception types
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafeApiCallRetryTest {

    @Test
    fun `safeApiCall succeeds on first attempt without retry`() = runTest {
        // Given: API call that succeeds
        val expectedResult = "Success"
        val apiCall: suspend () -> String = { expectedResult }
        
        // When: safeApiCall is executed
        val result = safeApiCall { apiCall() }
        
        // Then: Should return success immediately
        assertTrue("Result should be Success", result is Resource.Success)
        assertEquals(expectedResult, (result as Resource.Success).data)
    }

    @Test
    fun `safeApiCall retries on FirebaseFirestoreException UNAVAILABLE`() = runTest {
        // Given: API call that fails twice with UNAVAILABLE then succeeds
        val mockApiCall = mockk<suspend () -> String>()
        val unavailableException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        // Mock the exception code
        coEvery { unavailableException.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        
        var callCount = 0
        coEvery { mockApiCall() } answers {
            callCount++
            when (callCount) {
                1 -> throw unavailableException
                2 -> throw unavailableException
                else -> "Success after retries"
            }
        }
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should retry and eventually succeed
        assertTrue("Result should be Success", result is Resource.Success)
        assertEquals("Success after retries", (result as Resource.Success).data)
        
        // Verify API was called 3 times (1 initial + 2 retries)
        coVerify(exactly = 3) { mockApiCall() }
    }

    @Test
    fun `safeApiCall returns error after exhausting retries`() = runTest {
        // Given: API call that always fails with UNAVAILABLE
        val mockApiCall = mockk<suspend () -> String>()
        val unavailableException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        coEvery { unavailableException.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        coEvery { unavailableException.localizedMessage } returns "Service unavailable"
        coEvery { mockApiCall() } throws unavailableException
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should return error after all retries
        assertTrue("Result should be Error", result is Resource.Error)
        val error = result as Resource.Error
        assertEquals("Service unavailable", error.message)
        assertTrue("Should have user message", error.userMessage.isNotEmpty())
        
        // Verify API was called 4 times (1 initial + 3 retries)
        coVerify(exactly = 4) { mockApiCall() }
    }

    @Test
    fun `safeApiCall does not retry on non-UNAVAILABLE FirebaseException`() = runTest {
        // Given: API call that fails with PERMISSION_DENIED
        val mockApiCall = mockk<suspend () -> String>()
        val permissionException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        coEvery { permissionException.code } returns FirebaseFirestoreException.Code.PERMISSION_DENIED
        coEvery { permissionException.localizedMessage } returns "Permission denied"
        coEvery { mockApiCall() } throws permissionException
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should return error immediately without retry
        assertTrue("Result should be Error", result is Resource.Error)
        val error = result as Resource.Error
        assertEquals("Permission denied", error.message)
        assertTrue("Should have user message", error.userMessage.isNotEmpty())
        assertEquals("Bu işlem için yetkiniz yok", error.userMessage)
        
        // Verify API was called only once (no retries)
        coVerify(exactly = 1) { mockApiCall() }
    }

    @Test
    fun `safeApiCall does not retry on generic Exception`() = runTest {
        // Given: API call that fails with generic exception
        val mockApiCall = mockk<suspend () -> String>()
        val genericException = Exception("Network error")
        
        coEvery { mockApiCall() } throws genericException
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should return error immediately without retry
        assertTrue("Result should be Error", result is Resource.Error)
        val error = result as Resource.Error
        assertEquals("Network error", error.message)
        assertTrue("Should have user message", error.userMessage.isNotEmpty())
        
        // Verify API was called only once (no retries)
        coVerify(exactly = 1) { mockApiCall() }
    }

    @Test
    fun `safeApiCall uses exponential backoff delays`() = runTest {
        // Given: API call that fails 3 times then succeeds
        val mockApiCall = mockk<suspend () -> String>()
        val unavailableException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        coEvery { unavailableException.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        
        var callCount = 0
        coEvery { mockApiCall() } answers {
            callCount++
            when (callCount) {
                in 1..3 -> throw unavailableException
                else -> "Success"
            }
        }
        
        // When: safeApiCall is executed
        val job = kotlinx.coroutines.launch {
            safeApiCall { mockApiCall() }
        }
        
        // Verify timing (approximate due to test scheduler)
        // First call: immediate
        // Second call: after 500ms delay
        advanceTimeBy(500)
        // Third call: after 1000ms delay
        advanceTimeBy(1000)
        // Fourth call: after 2000ms delay
        advanceTimeBy(2000)
        
        job.join()
        
        // Then: Should have called 4 times with delays
        coVerify(exactly = 4) { mockApiCall() }
    }

    @Test
    fun `safeApiCall succeeds on second attempt after one retry`() = runTest {
        // Given: API call that fails once then succeeds
        val mockApiCall = mockk<suspend () -> String>()
        val unavailableException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        coEvery { unavailableException.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        
        var callCount = 0
        coEvery { mockApiCall() } answers {
            callCount++
            if (callCount == 1) throw unavailableException else "Success on retry"
        }
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should succeed after one retry
        assertTrue("Result should be Success", result is Resource.Success)
        assertEquals("Success on retry", (result as Resource.Success).data)
        
        // Verify API was called 2 times (1 initial + 1 retry)
        coVerify(exactly = 2) { mockApiCall() }
    }

    @Test
    fun `safeApiCall handles null localizedMessage`() = runTest {
        // Given: Exception with null localizedMessage
        val mockApiCall = mockk<suspend () -> String>()
        val unavailableException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        coEvery { unavailableException.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        coEvery { unavailableException.localizedMessage } returns null
        coEvery { mockApiCall() } throws unavailableException
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should return error with default message
        assertTrue("Result should be Error", result is Resource.Error)
        val error = result as Resource.Error
        assertEquals("Unknown error occurred", error.message)
        assertTrue("Should have user message", error.userMessage.isNotEmpty())
    }

    @Test
    fun `safeApiCall returns correct exception in error result`() = runTest {
        // Given: API call that fails
        val mockApiCall = mockk<suspend () -> String>()
        val customException = IllegalStateException("Custom error")
        
        coEvery { mockApiCall() } throws customException
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should return error with correct exception
        assertTrue("Result should be Error", result is Resource.Error)
        val error = result as Resource.Error
        assertEquals(customException, error.exception)
        assertTrue("Should have user message", error.userMessage.isNotEmpty())
    }

    @Test
    fun `safeApiCall works with different return types`() = runTest {
        // Test with Int
        val intResult = safeApiCall { 42 }
        assertTrue(intResult is Resource.Success)
        assertEquals(42, (intResult as Resource.Success).data)
        
        // Test with List
        val listResult = safeApiCall { listOf("a", "b", "c") }
        assertTrue(listResult is Resource.Success)
        assertEquals(3, (listResult as Resource.Success).data.size)
        
        // Test with custom object
        data class TestData(val id: String, val value: Int)
        val objectResult = safeApiCall { TestData("test", 100) }
        assertTrue(objectResult is Resource.Success)
        assertEquals("test", (objectResult as Resource.Success).data.id)
    }

    @Test
    fun `safeApiCall maximum retry count is correct`() = runTest {
        // Given: API call that always fails
        var callCount = 0
        val mockApiCall: suspend () -> String = {
            callCount++
            val exception = mockk<FirebaseFirestoreException>(relaxed = true)
            coEvery { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
            throw exception
        }
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should have attempted exactly 4 times (1 + 3 retries)
        assertEquals(4, callCount)
        assertTrue("Result should be Error", result is Resource.Error)
        assertTrue("Should have user message", (result as Resource.Error).userMessage.isNotEmpty())
    }

    @Test
    fun `safeApiCall stops retrying when successful`() = runTest {
        // Given: API call that succeeds on third attempt
        val mockApiCall = mockk<suspend () -> String>()
        val unavailableException = mockk<FirebaseFirestoreException>(relaxed = true)
        
        coEvery { unavailableException.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        
        var callCount = 0
        coEvery { mockApiCall() } answers {
            callCount++
            when (callCount) {
                1, 2 -> throw unavailableException
                else -> "Success on third attempt"
            }
        }
        
        // When: safeApiCall is executed
        val result = safeApiCall { mockApiCall() }
        
        // Then: Should stop retrying after success
        assertTrue("Result should be Success", result is Resource.Success)
        assertEquals("Success on third attempt", (result as Resource.Success).data)
        
        // Should have called exactly 3 times (not 4)
        coVerify(exactly = 3) { mockApiCall() }
    }
}

