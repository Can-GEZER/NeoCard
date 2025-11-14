package com.cangzr.neocard.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val premium: Boolean = false,
    val profileImageUrl: String? = null
)
