package com.cangzr.neocard.data.repository.impl

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.common.safeApiCall
import com.cangzr.neocard.data.repository.ReferralRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseReferralRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReferralRepository {

    override suspend fun generateReferralCode(userId: String): Resource<String> = safeApiCall {
        val existingCode = getReferralCode(userId)
        if (existingCode is Resource.Success && existingCode.data != null) {
            return@safeApiCall existingCode.data!!
        }

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(userId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(8)
            .uppercase()

        val referralCode = "REF$hash"

        firestore.collection("users")
            .document(userId)
            .update("referralCode", referralCode)
            .await()

        referralCode
    }

    override suspend fun getReferralCode(userId: String): Resource<String?> = safeApiCall {
        val userDoc = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        userDoc.getString("referralCode")
    }

    override suspend fun validateReferralCode(referralCode: String): Resource<String?> = safeApiCall {
        if (referralCode.length != 11 || !referralCode.startsWith("REF")) {
            return@safeApiCall null
        }
        
        val querySnapshot = firestore.collection("users")
            .whereEqualTo("referralCode", referralCode)
            .limit(1)
            .get()
            .await()

        if (querySnapshot.isEmpty) {
            null
        } else {
            querySnapshot.documents.first().id
        }
    }

    override suspend fun createReferral(
        referrerId: String,
        referredId: String,
        referralCode: String
    ): Resource<Unit> = safeApiCall {
        if (referrerId.isBlank() || referredId.isBlank() || referralCode.isBlank()) {
            throw IllegalArgumentException("Invalid referral data")
        }
        
        if (referrerId == referredId) {
            throw IllegalArgumentException("User cannot refer themselves")
        }
        
        val referrerDoc = firestore.collection("users")
            .document(referrerId)
            .get()
            .await()
        
        val referrerCode = referrerDoc.getString("referralCode")
        if (referrerCode != referralCode) {
            throw IllegalArgumentException("Invalid referral code")
        }
        
        val existingQuery = firestore.collection("referrals")
            .whereEqualTo("referredId", referredId)
            .limit(1)
            .get()
            .await()

        if (!existingQuery.isEmpty) {
            throw Exception("User already has a referral")
        }

        val referralData = mapOf(
            "referrerId" to referrerId,
            "referredId" to referredId,
            "referralCode" to referralCode,
            "createdAt" to System.currentTimeMillis(),
            "rewardGiven" to false
        )

        val referralRef = firestore.collection("referrals")
            .add(referralData)
            .await()

        firestore.collection("users")
            .document(referredId)
            .update("referredBy", referrerId)
            .await()

        firestore.collection("users")
            .document(referrerId)
            .get()
            .await()
            .let { userDoc ->
                val currentCount = (userDoc.getLong("referralCount") ?: 0L).toInt()
                firestore.collection("users")
                    .document(referrerId)
                    .update("referralCount", currentCount + 1)
                    .await()
            }

        giveReferralReward(referrerId, referralRef.id)
    }

    override suspend fun isUserReferred(userId: String): Resource<Boolean> = safeApiCall {
        val userDoc = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        userDoc.getString("referredBy") != null
    }

    override suspend fun giveReferralReward(referrerId: String, referralId: String): Resource<Unit> = safeApiCall {
        val referralDoc = firestore.collection("referrals")
            .document(referralId)
            .get()
            .await()

        if (referralDoc.getBoolean("rewardGiven") == true) {
            return@safeApiCall // Reward already given
        }

        firestore.collection("referrals")
            .document(referralId)
            .update("rewardGiven", true)
            .await()

        val premiumDuration = 7 * 24 * 60 * 60 * 1000L
        val userDoc = firestore.collection("users")
            .document(referrerId)
            .get()
            .await()

        val currentEndTime = userDoc.getLong("premiumEndTime") ?: 0L
        val currentTime = System.currentTimeMillis()

        val newEndTime = if (currentEndTime > currentTime) {
            currentEndTime + premiumDuration
        } else {
            currentTime + premiumDuration
        }

        firestore.collection("users")
            .document(referrerId)
            .update(
                mapOf(
                    "premium" to true,
                    "premiumEndTime" to newEndTime
                )
            )
            .await()
    }
}

