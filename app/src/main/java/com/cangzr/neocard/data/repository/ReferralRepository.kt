package com.cangzr.neocard.data.repository

import com.cangzr.neocard.common.Resource
import com.cangzr.neocard.data.model.Referral

interface ReferralRepository {
    suspend fun generateReferralCode(userId: String): Resource<String>
    
    suspend fun getReferralCode(userId: String): Resource<String?>
    
    suspend fun validateReferralCode(referralCode: String): Resource<String?>
    
    suspend fun createReferral(
        referrerId: String,
        referredId: String,
        referralCode: String
    ): Resource<Unit>
    
    suspend fun isUserReferred(userId: String): Resource<Boolean>
    
    suspend fun giveReferralReward(referrerId: String, referralId: String): Resource<Unit>
}

