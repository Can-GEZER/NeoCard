package com.cangzr.neocard.data.model

data class PromoCode(
    val id: String = "",
    val code: String = "",
    val usageLimit: Int = 0,
    val usedCount: Int = 0,
    val createdAt: String = "",
    val isActive: Boolean = true,
    val usedBy: List<String> = emptyList()
)

data class UserPromoUsage(
    val userId: String = "",
    val promoCodeId: String = "",
    val promoCode: String = "",
    val usedAt: Long = 0L,
    val premiumDays: Int = 7
)
