package com.cangzr.neocard.data.repository.impl

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.common.safeApiCall
import com.cangzr.neocard.data.repository.AuthRepository
import com.cangzr.neocard.utils.ReferralCodeManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val referralRepository: com.cangzr.neocard.data.repository.ReferralRepository
) : AuthRepository {

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): Resource<FirebaseUser> = safeApiCall {
        val authResult = auth.signInWithEmailAndPassword(email, password).await()
        authResult.user ?: throw Exception("User is null after sign in")
    }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Resource<FirebaseUser> = safeApiCall {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        val user = authResult.user ?: throw Exception("User is null after sign up")

        val profileResult = createUserProfile(
            userId = user.uid,
            email = email,
            displayName = displayName,
            isPremium = false
        )

        if (profileResult is Resource.Error) {
            throw profileResult.exception
        }

        user
    }

    override suspend fun signInWithGoogle(
        account: GoogleSignInAccount
    ): Resource<FirebaseUser> = safeApiCall {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        val authResult = auth.signInWithCredential(credential).await()
        val user = authResult.user ?: throw Exception("User is null after Google sign in")

        val userDoc = firestore.collection("users")
            .document(user.uid)
            .get()
            .await()

        if (!userDoc.exists()) {
            val profileResult = createUserProfile(
                userId = user.uid,
                email = user.email,
                displayName = user.displayName,
                isPremium = false
            )

            if (profileResult is Resource.Error) {
                throw profileResult.exception
            }
        }

        user
    }

    override suspend fun signOut(): Resource<Unit> = safeApiCall {
        auth.signOut()
    }

    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    override suspend fun createUserProfile(
        userId: String,
        email: String?,
        displayName: String?,
        isPremium: Boolean
    ): Resource<Unit> = safeApiCall {
        val referralCodeResult = referralRepository.generateReferralCode(userId)
        val referralCode = if (referralCodeResult is Resource.Success) {
            referralCodeResult.data
        } else {
            null
        }

        val userData = hashMapOf(
            "id" to userId,
            "email" to email,
            "displayName" to displayName,
            "premium" to isPremium,
            "connectRequests" to emptyList<String>(),
            "connected" to emptyList<String>(),
            "referralCount" to 0L
        )

        referralCode?.let {
            userData["referralCode"] = it
        }

        firestore.collection("users")
            .document(userId)
            .set(userData)
            .await()

    }

    override suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Resource<Unit> = safeApiCall {
        firestore.collection("users")
            .document(userId)
            .update(updates)
            .await()
    }

    override suspend fun deleteUserAccount(
        userId: String
    ): Resource<Unit> = safeApiCall {
        val cardsSnapshot = firestore.collection("users")
            .document(userId)
            .collection("cards")
            .get()
            .await()

        val batch = firestore.batch()

        cardsSnapshot.documents.forEach { cardDoc ->
            batch.delete(cardDoc.reference)

            val publicCardRef = firestore.collection("public_cards")
                .document(cardDoc.id)
            batch.delete(publicCardRef)
        }

        val notificationsSnapshot = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .get()
            .await()

        notificationsSnapshot.documents.forEach { notificationDoc ->
            batch.delete(notificationDoc.reference)
        }

        val userRef = firestore.collection("users").document(userId)
        batch.delete(userRef)

        batch.commit().await()

        auth.currentUser?.delete()?.await()
    }

    override suspend fun sendPasswordResetEmail(
        email: String
    ): Resource<Unit> = safeApiCall {
        auth.sendPasswordResetEmail(email).await()
    }
}

