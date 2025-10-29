package com.cangzr.neocard.common

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * ErrorMapper maps technical exceptions to user-friendly Turkish error messages.
 * 
 * This utility object provides translation of common Firebase and network exceptions
 * into understandable messages for end users. It supports 40+ exception types including
 * Firebase Firestore, Firebase Auth, and network exceptions.
 * 
 * **Supported Exception Types:**
 * - Firebase Firestore exceptions (16 error codes)
 * - Firebase Auth exceptions (10+ error codes)
 * - Network exceptions (UnknownHost, SocketTimeout, IOException)
 * - Generic exceptions (IllegalArgumentException, NullPointerException)
 * 
 * @see [Resource.Error] Uses ErrorMapper for userMessage
 * @see com.cangzr.neocard.ui.components.ErrorDisplay UI components using error messages
 * 
 * @since 1.0
 */
object ErrorMapper {
    
    /**
     * Maps an exception to a user-friendly Turkish error message.
     * 
     * This method examines the exception type and returns an appropriate user-friendly
     * message. If no specific mapping exists, returns the default message.
     * 
     * @param exception The exception to map to a user message
     * @param defaultMessage Default message if no specific mapping exists (default: "Bir hata oluştu. Lütfen tekrar deneyin.")
     * @return User-friendly error message in Turkish
     * 
     * @see [getErrorTitle] Get error title for UI
     * @see [isRetryableError] Check if error is retryable
     * 
     * @since 1.0
     */
    fun getUserMessage(
        exception: Throwable,
        defaultMessage: String = "Bir hata oluştu. Lütfen tekrar deneyin."
    ): String {
        return when (exception) {
            // Firebase Firestore Exceptions
            is FirebaseFirestoreException -> mapFirestoreException(exception)
            
            // Firebase Auth Exceptions
            is FirebaseAuthException -> mapAuthException(exception)
            
            // Network Exceptions
            is UnknownHostException -> "İnternet bağlantınızı kontrol edin"
            is SocketTimeoutException -> "Bağlantı zaman aşımına uğradı. Lütfen tekrar deneyin"
            is IOException -> "Ağ bağlantısı hatası. Lütfen tekrar deneyin"
            is FirebaseNetworkException -> "İnternet bağlantınızı kontrol edin"
            is FirebaseTooManyRequestsException -> "Çok fazla istek gönderildi. Lütfen daha sonra tekrar deneyin"
            
            // Generic Exceptions
            is IllegalArgumentException -> "Geçersiz veri girişi"
            is IllegalStateException -> "İşlem şu anda gerçekleştirilemiyor"
            is NullPointerException -> "Beklenmeyen bir hata oluştu"
            
            // Default
            else -> defaultMessage
        }
    }
    
    /**
     * Maps FirebaseFirestoreException to user-friendly Turkish message.
     * 
     * This private method handles all Firebase Firestore error codes and returns
     * appropriate user-friendly messages in Turkish.
     * 
     * @param exception The FirebaseFirestoreException to map
     * @return User-friendly error message for the Firestore exception
     * 
     * @see [getUserMessage] Public method that calls this
     * 
     * @since 1.0
     */
    private fun mapFirestoreException(exception: FirebaseFirestoreException): String {
        return when (exception.code) {
            FirebaseFirestoreException.Code.CANCELLED -> 
                "İşlem iptal edildi"
            
            FirebaseFirestoreException.Code.UNKNOWN -> 
                "Bilinmeyen bir hata oluştu"
            
            FirebaseFirestoreException.Code.INVALID_ARGUMENT -> 
                "Geçersiz parametre"
            
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> 
                "İşlem zaman aşımına uğradı"
            
            FirebaseFirestoreException.Code.NOT_FOUND -> 
                "İstenen veri bulunamadı"
            
            FirebaseFirestoreException.Code.ALREADY_EXISTS -> 
                "Bu veri zaten mevcut"
            
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                "Bu işlem için yetkiniz yok"
            
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> 
                "Kaynak kotası aşıldı. Lütfen daha sonra tekrar deneyin"
            
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> 
                "İşlem gereksinimleri karşılanmadı"
            
            FirebaseFirestoreException.Code.ABORTED -> 
                "İşlem durduruldu. Lütfen tekrar deneyin"
            
            FirebaseFirestoreException.Code.OUT_OF_RANGE -> 
                "Geçersiz aralık değeri"
            
            FirebaseFirestoreException.Code.UNIMPLEMENTED -> 
                "Bu özellik henüz desteklenmiyor"
            
            FirebaseFirestoreException.Code.INTERNAL -> 
                "Sunucu hatası. Lütfen daha sonra tekrar deneyin"
            
            FirebaseFirestoreException.Code.UNAVAILABLE -> 
                "Sunucu şu anda kullanılamıyor. Lütfen tekrar deneyin"
            
            FirebaseFirestoreException.Code.DATA_LOSS -> 
                "Veri kaybı oluştu"
            
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> 
                "Lütfen giriş yapın"
            
            else -> "Veritabanı hatası. Lütfen tekrar deneyin"
        }
    }
    
    /**
     * Maps FirebaseAuthException to user-friendly Turkish message.
     * 
     * This private method handles all Firebase Authentication error codes and returns
     * appropriate user-friendly messages in Turkish.
     * 
     * @param exception The FirebaseAuthException to map
     * @return User-friendly error message for the Auth exception
     * 
     * @see [getUserMessage] Public method that calls this
     * 
     * @since 1.0
     */
    private fun mapAuthException(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> 
                "Geçersiz e-posta adresi"
            
            "ERROR_WRONG_PASSWORD" -> 
                "Hatalı şifre"
            
            "ERROR_USER_NOT_FOUND" -> 
                "Kullanıcı bulunamadı"
            
            "ERROR_USER_DISABLED" -> 
                "Bu hesap devre dışı bırakılmış"
            
            "ERROR_TOO_MANY_REQUESTS" -> 
                "Çok fazla başarısız deneme. Lütfen daha sonra tekrar deneyin"
            
            "ERROR_EMAIL_ALREADY_IN_USE" -> 
                "Bu e-posta adresi zaten kullanımda"
            
            "ERROR_WEAK_PASSWORD" -> 
                "Şifre çok zayıf. Lütfen daha güçlü bir şifre seçin"
            
            "ERROR_REQUIRES_RECENT_LOGIN" -> 
                "Bu işlem için yeniden giriş yapmanız gerekiyor"
            
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> 
                "Bu e-posta adresi farklı bir giriş yöntemiyle kayıtlı"
            
            "ERROR_CREDENTIAL_ALREADY_IN_USE" -> 
                "Bu kimlik bilgileri başka bir hesap tarafından kullanılıyor"
            
            "ERROR_INVALID_CREDENTIAL" -> 
                "Geçersiz kimlik bilgileri"
            
            "ERROR_OPERATION_NOT_ALLOWED" -> 
                "Bu işleme izin verilmiyor"
            
            "ERROR_NETWORK_REQUEST_FAILED" -> 
                "Ağ bağlantısı hatası"
            
            else -> "Kimlik doğrulama hatası. Lütfen tekrar deneyin"
        }
    }
    
    /**
     * Gets a short error title for UI display.
     * 
     * Returns a brief title that can be used in error dialogs, snackbars, or other UI components.
     * The title is context-aware and matches the error type.
     * 
     * @param exception The exception to get title for
     * @return Short error title (e.g., "Bağlantı Hatası", "Yetki Hatası")
     * 
     * @see [getUserMessage] Get detailed error message
     * @see com.cangzr.neocard.ui.components.ErrorDisplay ErrorDisplay component
     * 
     * @since 1.0
     */
    fun getErrorTitle(exception: Throwable): String {
        return when (exception) {
            is UnknownHostException, 
            is FirebaseNetworkException, 
            is IOException -> "Bağlantı Hatası"
            
            is FirebaseFirestoreException -> when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Yetki Hatası"
                FirebaseFirestoreException.Code.NOT_FOUND -> "Bulunamadı"
                FirebaseFirestoreException.Code.UNAVAILABLE -> "Sunucu Hatası"
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> "Giriş Gerekli"
                else -> "Hata"
            }
            
            is FirebaseAuthException -> "Kimlik Doğrulama Hatası"
            
            else -> "Hata"
        }
    }
    
    /**
     * Checks if an error is retryable by the user.
     * 
     * Some errors like network timeouts or temporary unavailability can be retried.
     * This method determines if showing a retry button makes sense for the given error.
     * 
     * **Retryable Errors:**
     * - Network exceptions (UnknownHostException, SocketTimeoutException, IOException)
     * - Temporary Firebase errors (UNAVAILABLE, DEADLINE_EXCEEDED, ABORTED)
     * 
     * **Non-Retryable Errors:**
     * - Permission errors (PERMISSION_DENIED)
     * - Not found errors (NOT_FOUND)
     * - Authentication errors (UNAUTHENTICATED)
     * - Validation errors (IllegalArgumentException)
     * 
     * @param exception The exception to check
     * @return true if the error is retryable, false otherwise
     * 
     * @see [getUserMessage] Get error message
     * @see com.cangzr.neocard.ui.components.ErrorDisplay ErrorDisplay with retry button
     * 
     * @since 1.0
     */
    fun isRetryableError(exception: Throwable): Boolean {
        return when (exception) {
            is UnknownHostException,
            is SocketTimeoutException,
            is IOException,
            is FirebaseNetworkException -> true
            
            is FirebaseFirestoreException -> when (exception.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
                FirebaseFirestoreException.Code.ABORTED -> true
                else -> false
            }
            
            else -> false
        }
    }
}

