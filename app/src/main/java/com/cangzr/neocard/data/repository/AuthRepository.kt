package com.cangzr.neocard.data.repository

import com.cangzr.neocard.common.Resource
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Resource<FirebaseUser>
    
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Resource<FirebaseUser>
    
    suspend fun signInWithGoogle(
        account: GoogleSignInAccount
    ): Resource<FirebaseUser>
    
    suspend fun signOut(): Resource<Unit>
    
    fun getCurrentUser(): FirebaseUser?
    
    suspend fun createUserProfile(
        userId: String,
        email: String?,
        displayName: String?,
        isPremium: Boolean = false
    ): Resource<Unit>
    
    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Resource<Unit>
    
    suspend fun deleteUserAccount(
        userId: String
    ): Resource<Unit>
    
    suspend fun sendPasswordResetEmail(
        email: String
    ): Resource<Unit>
}

