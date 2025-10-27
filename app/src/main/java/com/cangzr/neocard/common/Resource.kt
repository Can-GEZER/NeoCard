package com.cangzr.neocard.common

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
     * @param message Hata mesajı
     */
    data class Error(
        val exception: Throwable,
        val message: String? = exception.localizedMessage
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
 * Try-catch ile hataları yakalar
 *
 * @param apiCall Yapılacak API çağrısı
 * @return Resource<T> Success, Error veya Loading
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): Resource<T> {
    return try {
        val result = apiCall()
        Resource.Success(result)
    } catch (e: Exception) {
        Resource.Error(
            exception = e,
            message = e.localizedMessage ?: "Unknown error occurred"
        )
    }
}

