package com.cangzr.neocard.common

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {
    
    fun getUserMessage(
        exception: Throwable,
        defaultMessage: String = "Bir hata oluştu. Lütfen tekrar deneyin."
    ): String {
        return when (exception) {
            is FirebaseFirestoreException -> mapFirestoreException(exception)
            
            is FirebaseAuthException -> mapAuthException(exception)
            
            is UnknownHostException -> "İnternet bağlantınızı kontrol edin"
            is SocketTimeoutException -> "Bağlantı zaman aşımına uğradı. Lütfen tekrar deneyin"
            is IOException -> "Ağ bağlantısı hatası. Lütfen tekrar deneyin"
            is FirebaseNetworkException -> "İnternet bağlantınızı kontrol edin"
            is FirebaseTooManyRequestsException -> "Çok fazla istek gönderildi. Lütfen daha sonra tekrar deneyin"
            
            is IllegalArgumentException -> "Geçersiz veri girişi"
            is IllegalStateException -> "İşlem şu anda gerçekleştirilemiyor"
            is NullPointerException -> "Beklenmeyen bir hata oluştu"
            
            else -> defaultMessage
        }
    }
    
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

