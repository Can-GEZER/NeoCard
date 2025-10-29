package com.cangzr.neocard.common

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for ErrorMapper
 * 
 * Tests user-friendly error message mapping
 */
class ErrorMapperTest {

    @Test
    fun `getUserMessage returns correct message for UnknownHostException`() {
        // Given
        val exception = UnknownHostException()
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("İnternet bağlantınızı kontrol edin", message)
    }

    @Test
    fun `getUserMessage returns correct message for SocketTimeoutException`() {
        // Given
        val exception = SocketTimeoutException()
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Bağlantı zaman aşımına uğradı. Lütfen tekrar deneyin", message)
    }

    @Test
    fun `getUserMessage returns correct message for IOException`() {
        // Given
        val exception = IOException()
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Ağ bağlantısı hatası. Lütfen tekrar deneyin", message)
    }

    @Test
    fun `getUserMessage returns correct message for FirebaseNetworkException`() {
        // Given
        val exception = mockk<FirebaseNetworkException>(relaxed = true)
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("İnternet bağlantınızı kontrol edin", message)
    }

    @Test
    fun `getUserMessage returns correct message for PERMISSION_DENIED`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.PERMISSION_DENIED
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Bu işlem için yetkiniz yok", message)
    }

    @Test
    fun `getUserMessage returns correct message for NOT_FOUND`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.NOT_FOUND
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("İstenen veri bulunamadı", message)
    }

    @Test
    fun `getUserMessage returns correct message for UNAVAILABLE`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Sunucu şu anda kullanılamıyor. Lütfen tekrar deneyin", message)
    }

    @Test
    fun `getUserMessage returns correct message for UNAUTHENTICATED`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.UNAUTHENTICATED
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Lütfen giriş yapın", message)
    }

    @Test
    fun `getUserMessage returns correct message for IllegalArgumentException`() {
        // Given
        val exception = IllegalArgumentException()
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Geçersiz veri girişi", message)
    }

    @Test
    fun `getUserMessage returns default message for unknown exception`() {
        // Given
        val exception = RuntimeException()
        val defaultMessage = "Özel hata mesajı"
        
        // When
        val message = ErrorMapper.getUserMessage(exception, defaultMessage)
        
        // Then
        assertEquals(defaultMessage, message)
    }

    @Test
    fun `getErrorTitle returns correct title for network exceptions`() {
        // Given
        val exception = UnknownHostException()
        
        // When
        val title = ErrorMapper.getErrorTitle(exception)
        
        // Then
        assertEquals("Bağlantı Hatası", title)
    }

    @Test
    fun `getErrorTitle returns correct title for PERMISSION_DENIED`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.PERMISSION_DENIED
        
        // When
        val title = ErrorMapper.getErrorTitle(exception)
        
        // Then
        assertEquals("Yetki Hatası", title)
    }

    @Test
    fun `getErrorTitle returns correct title for NOT_FOUND`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.NOT_FOUND
        
        // When
        val title = ErrorMapper.getErrorTitle(exception)
        
        // Then
        assertEquals("Bulunamadı", title)
    }

    @Test
    fun `getErrorTitle returns correct title for UNAVAILABLE`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        
        // When
        val title = ErrorMapper.getErrorTitle(exception)
        
        // Then
        assertEquals("Sunucu Hatası", title)
    }

    @Test
    fun `getErrorTitle returns correct title for UNAUTHENTICATED`() {
        // Given
        val exception = mockk<FirebaseFirestoreException>(relaxed = true)
        every { exception.code } returns FirebaseFirestoreException.Code.UNAUTHENTICATED
        
        // When
        val title = ErrorMapper.getErrorTitle(exception)
        
        // Then
        assertEquals("Giriş Gerekli", title)
    }

    @Test
    fun `isRetryableError returns true for network exceptions`() {
        // Given
        val exceptions = listOf(
            UnknownHostException(),
            SocketTimeoutException(),
            IOException(),
            mockk<FirebaseNetworkException>(relaxed = true)
        )
        
        // When/Then
        exceptions.forEach { exception ->
            assertTrue(
                "Should be retryable: ${exception::class.simpleName}",
                ErrorMapper.isRetryableError(exception)
            )
        }
    }

    @Test
    fun `isRetryableError returns true for retryable Firestore exceptions`() {
        // Given
        val retryableCodes = listOf(
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.ABORTED
        )
        
        // When/Then
        retryableCodes.forEach { code ->
            val exception = mockk<FirebaseFirestoreException>(relaxed = true)
            every { exception.code } returns code
            
            assertTrue(
                "Should be retryable: $code",
                ErrorMapper.isRetryableError(exception)
            )
        }
    }

    @Test
    fun `isRetryableError returns false for non-retryable Firestore exceptions`() {
        // Given
        val nonRetryableCodes = listOf(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.NOT_FOUND,
            FirebaseFirestoreException.Code.ALREADY_EXISTS,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.UNAUTHENTICATED
        )
        
        // When/Then
        nonRetryableCodes.forEach { code ->
            val exception = mockk<FirebaseFirestoreException>(relaxed = true)
            every { exception.code } returns code
            
            assertFalse(
                "Should not be retryable: $code",
                ErrorMapper.isRetryableError(exception)
            )
        }
    }

    @Test
    fun `isRetryableError returns false for generic exceptions`() {
        // Given
        val exceptions = listOf(
            IllegalArgumentException(),
            IllegalStateException(),
            NullPointerException(),
            RuntimeException()
        )
        
        // When/Then
        exceptions.forEach { exception ->
            assertFalse(
                "Should not be retryable: ${exception::class.simpleName}",
                ErrorMapper.isRetryableError(exception)
            )
        }
    }

    @Test
    fun `mapAuthException returns correct message for ERROR_INVALID_EMAIL`() {
        // Given
        val exception = mockk<FirebaseAuthException>(relaxed = true)
        every { exception.errorCode } returns "ERROR_INVALID_EMAIL"
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Geçersiz e-posta adresi", message)
    }

    @Test
    fun `mapAuthException returns correct message for ERROR_WRONG_PASSWORD`() {
        // Given
        val exception = mockk<FirebaseAuthException>(relaxed = true)
        every { exception.errorCode } returns "ERROR_WRONG_PASSWORD"
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Hatalı şifre", message)
    }

    @Test
    fun `mapAuthException returns correct message for ERROR_USER_NOT_FOUND`() {
        // Given
        val exception = mockk<FirebaseAuthException>(relaxed = true)
        every { exception.errorCode } returns "ERROR_USER_NOT_FOUND"
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Kullanıcı bulunamadı", message)
    }

    @Test
    fun `mapAuthException returns correct message for ERROR_EMAIL_ALREADY_IN_USE`() {
        // Given
        val exception = mockk<FirebaseAuthException>(relaxed = true)
        every { exception.errorCode } returns "ERROR_EMAIL_ALREADY_IN_USE"
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Bu e-posta adresi zaten kullanımda", message)
    }

    @Test
    fun `mapAuthException returns correct message for ERROR_WEAK_PASSWORD`() {
        // Given
        val exception = mockk<FirebaseAuthException>(relaxed = true)
        every { exception.errorCode } returns "ERROR_WEAK_PASSWORD"
        
        // When
        val message = ErrorMapper.getUserMessage(exception)
        
        // Then
        assertEquals("Şifre çok zayıf. Lütfen daha güçlü bir şifre seçin", message)
    }

    @Test
    fun `all Firestore exception codes have mappings`() {
        // Test that all Firestore exception codes return non-default messages
        val codes = listOf(
            FirebaseFirestoreException.Code.CANCELLED,
            FirebaseFirestoreException.Code.UNKNOWN,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.NOT_FOUND,
            FirebaseFirestoreException.Code.ALREADY_EXISTS,
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.OUT_OF_RANGE,
            FirebaseFirestoreException.Code.UNIMPLEMENTED,
            FirebaseFirestoreException.Code.INTERNAL,
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DATA_LOSS,
            FirebaseFirestoreException.Code.UNAUTHENTICATED
        )
        
        codes.forEach { code ->
            val exception = mockk<FirebaseFirestoreException>(relaxed = true)
            every { exception.code } returns code
            
            val message = ErrorMapper.getUserMessage(exception)
            
            assertNotEquals(
                "Code $code should have a specific message",
                "Bir hata oluştu. Lütfen tekrar deneyin.",
                message
            )
            assertFalse(
                "Code $code message should not be empty",
                message.isEmpty()
            )
        }
    }
}

