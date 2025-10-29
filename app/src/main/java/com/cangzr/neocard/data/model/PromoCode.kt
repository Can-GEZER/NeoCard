package com.cangzr.neocard.data.model

/**
 * PromoCode data model representing a promotional code for premium subscriptions.
 * 
 * Promo codes can be redeemed by users to receive premium subscription benefits
 * for a specified duration.
 * 
 * @param id Unique promo code identifier
 * @param code Promo code string that users enter (e.g., "PREMIUM2024")
 * @param usageLimit Maximum number of times this code can be used (0 = unlimited)
 * @param usedCount Current number of times this code has been used
 * @param createdAt ISO timestamp string when the promo code was created
 * @param isActive Whether the promo code is currently active
 * @param usedBy List of user IDs who have used this promo code
 * 
 * @see [UserPromoUsage] Individual promo code usage records
 * @see com.cangzr.neocard.ui.screens.profile.components.PromoCodeRedeemCard Promo code redemption UI
 * 
 * @since 1.0
 */
data class PromoCode(
    val id: String = "",
    val code: String = "",
    val usageLimit: Int = 0,
    val usedCount: Int = 0,
    val createdAt: String = "",
    val isActive: Boolean = true,
    val usedBy: List<String> = emptyList()
)

/**
 * UserPromoUsage data model tracking when a user redeemed a promo code.
 * 
 * This class maintains a record of promo code redemptions for individual users,
 * including when they used it and what benefits they received.
 * 
 * @param userId ID of the user who redeemed the promo code
 * @param promoCodeId ID of the promo code that was redeemed
 * @param promoCode The actual promo code string that was used
 * @param usedAt Timestamp in milliseconds when the code was redeemed
 * @param premiumDays Number of premium days granted by this promo code
 * 
 * @see [PromoCode] The promo code that was used
 * 
 * @since 1.0
 */
data class UserPromoUsage(
    val userId: String = "",
    val promoCodeId: String = "",
    val promoCode: String = "",
    val usedAt: Long = 0L,
    val premiumDays: Int = 7
)
