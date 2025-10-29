package com.cangzr.neocard.data.repository

import com.cangzr.neocard.common.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser

/**
 * AuthRepository interface defines authentication and user profile operations.
 * 
 * This repository provides a clean abstraction layer for Firebase Authentication
 * and user profile management operations. All authentication flows and user
 * account management operations are handled through this interface.
 * 
 * **Key Operations:**
 * - Email/password authentication (sign in, sign up, password reset)
 * - Google Sign-In integration
 * - User profile CRUD operations
 * - Account deletion
 * 
 * **Implementation:**
 * - [FirebaseAuthRepository][com.cangzr.neocard.data.repository.impl.FirebaseAuthRepository] - Firebase implementation
 * 
 * @see [FirebaseUser] Firebase user object
 * @see [Resource] Result wrapper for all operations
 * @see com.cangzr.neocard.ui.screens.AuthScreen Authentication UI
 * 
 * @since 1.0
 */
interface AuthRepository {
    
    /**
     * Email ve şifre ile giriş yapar
     * @param email Kullanıcı email'i
     * @param password Kullanıcı şifresi
     * @return FirebaseUser veya hata
     */
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Resource<FirebaseUser>
    
    /**
     * Email ve şifre ile kayıt olur
     * @param email Kullanıcı email'i
     * @param password Kullanıcı şifresi
     * @param displayName Kullanıcı adı
     * @return FirebaseUser veya hata
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Resource<FirebaseUser>
    
    /**
     * Google hesabı ile giriş yapar
     * @param account Google hesap bilgileri
     * @return FirebaseUser veya hata
     */
    suspend fun signInWithGoogle(
        account: GoogleSignInAccount
    ): Resource<FirebaseUser>
    
    /**
     * Kullanıcıyı çıkış yapar
     */
    suspend fun signOut(): Resource<Unit>
    
    /**
     * Mevcut oturum açmış kullanıcıyı getirir
     * @return FirebaseUser veya null
     */
    fun getCurrentUser(): FirebaseUser?
    
    /**
     * Kullanıcı profili oluşturur (Firestore'da)
     * @param userId Kullanıcı ID'si
     * @param email Email
     * @param displayName Görünen ad
     * @param isPremium Premium üyelik durumu
     */
    suspend fun createUserProfile(
        userId: String,
        email: String?,
        displayName: String?,
        isPremium: Boolean = false
    ): Resource<Unit>
    
    /**
     * Kullanıcı profil bilgilerini günceller
     * @param userId Kullanıcı ID'si
     * @param updates Güncellenecek alanlar
     */
    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Resource<Unit>
    
    /**
     * Kullanıcı hesabını siler
     * @param userId Kullanıcı ID'si
     */
    suspend fun deleteUserAccount(
        userId: String
    ): Resource<Unit>
    
    /**
     * Şifre sıfırlama email'i gönderir
     * @param email Email adresi
     */
    suspend fun sendPasswordResetEmail(
        email: String
    ): Resource<Unit>
}

