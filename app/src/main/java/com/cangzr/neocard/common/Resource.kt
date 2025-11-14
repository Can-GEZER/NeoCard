package com.cangzr.neocard.common

import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    
    data class Error(
        val exception: Throwable,
        val message: String? = exception.localizedMessage,
        val userMessage: String = ErrorMapper.getUserMessage(exception)
    ) : Resource<Nothing>()
    
    data object Loading : Resource<Nothing>()
    
    fun isSuccess(): Boolean = this is Success
    
    fun isError(): Boolean = this is Error
    
    fun isLoading(): Boolean = this is Loading
    
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Resource<T> = withContext(Dispatchers.IO) {
    var lastException: Exception? = null
    val maxRetries = 3
    val retryDelays = listOf(500L, 1000L, 2000L) // Exponential backoff delays
    
    repeat(maxRetries + 1) { attempt ->
        try {
            val result = apiCall()
            return@withContext Resource.Success(result)
        } catch (e: Exception) {
            lastException = e
            
            val isRetryable = when (e) {
                is FirebaseFirestoreException -> {
                    e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                }
                else -> false
            }
            
            if (attempt >= maxRetries || !isRetryable) {
                return@withContext Resource.Error(
                    exception = e,
                    message = e.localizedMessage ?: "Unknown error occurred",
                    userMessage = ErrorMapper.getUserMessage(e)
                )
            }
            
            delay(retryDelays[attempt])
        }
    }
    
    Resource.Error(
        exception = lastException ?: Exception("Unknown error"),
        message = "Failed after $maxRetries retries",
        userMessage = ErrorMapper.getUserMessage(
            lastException ?: Exception("Unknown error")
        )
    )
}

