package com.cangzr.neocard.common

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * API çağrılarının sonucunu temsil eden sealed class
 * Loading, Success ve Error durumlarını yönetir
 */
sealed class Resource<out T> {
    /**
     * Başarılı sonuç
     * @param data Dönen veri
     */
    data class Success<out T>(val data: T) : Resource<T>()
    
    /**
     * Hata durumu
     * @param exception Oluşan hata
     * @param message Teknik hata mesajı (debugging için)
     * @param userMessage Kullanıcıya gösterilecek anlaşılır mesaj
     */
    data class Error(
        val exception: Throwable,
        val message: String? = exception.localizedMessage,
        val userMessage: String = ErrorMapper.getUserMessage(exception)
    ) : Resource<Nothing>()
    
    /**
     * Yükleme durumu
     */
    data object Loading : Resource<Nothing>()
    
    /**
     * Resource'un başarılı olup olmadığını kontrol eder
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Resource'un hata olup olmadığını kontrol eder
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Resource'un yükleme durumunda olup olmadığını kontrol eder
     */
    fun isLoading(): Boolean = this is Loading
    
    /**
     * Başarılı ise veriyi döner, değilse null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

/**
 * API çağrılarını güvenli şekilde yapar ve Resource ile sarar
 * Try-catch ile hataları yakalar ve geçici ağ hatalarında retry yapar
 * 
 * Retry Stratejisi:
 * - Maksimum 3 retry (toplam 4 deneme)
 * - Sadece FirebaseFirestoreException.Code.UNAVAILABLE için retry
 * - Exponential backoff: 500ms, 1000ms, 2000ms
 * - Dispatchers.IO kullanarak main thread'i bloklamaz
 *
 * @param apiCall Yapılacak API çağrısı
 * @return Resource<T> Success veya Error
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Resource<T> = withContext(Dispatchers.IO) {
    var lastException: Exception? = null
    val maxRetries = 3
    val retryDelays = listOf(500L, 1000L, 2000L) // Exponential backoff delays
    
    // Initial attempt + 3 retries = 4 total attempts
    repeat(maxRetries + 1) { attempt ->
        try {
            val result = apiCall()
            return@withContext Resource.Success(result)
        } catch (e: Exception) {
            lastException = e
            
            // Determine if this is a retryable error
            val isRetryable = when (e) {
                is FirebaseFirestoreException -> {
                    e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                }
                else -> false
            }
            
            // If this is the last attempt or error is not retryable, return error
            if (attempt >= maxRetries || !isRetryable) {
                return@withContext Resource.Error(
                    exception = e,
                    message = e.localizedMessage ?: "Unknown error occurred",
                    userMessage = ErrorMapper.getUserMessage(e)
                )
            }
            
            // Wait before retry with exponential backoff
            delay(retryDelays[attempt])
        }
    }
    
    // Fallback (should never reach here due to return in catch block)
    Resource.Error(
        exception = lastException ?: Exception("Unknown error"),
        message = "Failed after $maxRetries retries",
        userMessage = ErrorMapper.getUserMessage(
            lastException ?: Exception("Unknown error")
        )
    )
}

