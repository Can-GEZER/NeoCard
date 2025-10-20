package com.cangzr.neocard.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.cangzr.neocard.data.CardType

// Yeni Data Class: UserCard
data class UserCard(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val phone: String = "",
    val email: String = "",
    val company: String = "",
    val title: String = "",
    val website: String = "",
    val linkedin: String = "",
    val instagram: String = "",
    val twitter: String = "",
    val facebook: String = "",
    val github: String = "",
    val backgroundType: String = "SOLID",
    val backgroundColor: String = "#FFFFFF",
    val selectedGradient: String = "",
    val profileImageUrl: String? = "",  // 👈 Burası önemli!
    val cardType: String = "Genel",
    val textStyles: Map<String, TextStyleDTO> = emptyMap(),
    val bio: String = "",  // Biyografi alanı
    val cv: String = "",    // CV linki
    val isPublic: Boolean = true  // Kartvizit paylaşım ayarı
) {
    // Kart verilerini Map'e dönüştürme fonksiyonu
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "surname" to surname,
            "phone" to phone,
            "email" to email,
            "company" to company,
            "title" to title,
            "website" to website,
            "linkedin" to linkedin,
            "instagram" to instagram,
            "twitter" to twitter,
            "facebook" to facebook,
            "github" to github,
            "backgroundType" to backgroundType,
            "backgroundColor" to backgroundColor,
            "selectedGradient" to selectedGradient,
            "profileImageUrl" to (profileImageUrl ?: ""),
            "cardType" to cardType,
            "textStyles" to textStyles,
            "bio" to bio,
            "cv" to cv,
            "isPublic" to isPublic
        )
    }
}

// Keşif Kartı Veri Sınıfı
data class ExploreUserCard(
    val id: String = "",
    val name: String = "",
    val surname: String = "",
    val title: String = "",
    val company: String = "",
    val cardType: String = CardType.BUSINESS.name,
    val profileImageUrl: String = "",
    val userId: String = "",
    val isPublic: Boolean = true
)

// Metin Stili DTO
data class TextStyleDTO(
    val fontSize: Float? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val color: String? = null
)
