package com.cangzr.neocard.data.model

/**
 * User data model representing a user in the NeoCard application.
 * 
 * This class contains user profile information including authentication details,
 * display information, and premium status.
 * 
 * @param id Unique user identifier (typically Firebase Auth UID)
 * @param email User's email address
 * @param displayName User's display name for the application
 * @param premium Whether the user has active premium subscription
 * @param profileImageUrl Optional URL to user's profile image
 * 
 * @see com.cangzr.neocard.data.repository.UserRepository User operations
 * @see com.cangzr.neocard.data.repository.AuthRepository Authentication operations
 * 
 * @since 1.0
 */
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val premium: Boolean = false,
    val profileImageUrl: String? = null
)
