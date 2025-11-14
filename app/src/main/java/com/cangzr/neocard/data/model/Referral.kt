package com.cangzr.neocard.data.model

data class Referral(
    val id: String = "",
    val referrerId: String = "",
    val referredId: String = "",
    val referralCode: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val rewardGiven: Boolean = false
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "referrerId" to referrerId,
            "referredId" to referredId,
            "referralCode" to referralCode,
            "createdAt" to createdAt,
            "rewardGiven" to rewardGiven
        )
    }
}

