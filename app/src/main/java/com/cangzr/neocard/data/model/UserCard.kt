package com.cangzr.neocard.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.cangzr.neocard.data.CardType

/**
 * UserCard data model representing a business card or contact card in the NeoCard application.
 * 
 * This class contains all information needed to display and manage a digital business card,
 * including contact information, social media links, visual customization options, and
 * card visibility settings.
 * 
 * @param id Unique card identifier
 * @param name Card owner's first name
 * @param surname Card owner's last name
 * @param phone Contact phone number
 * @param email Contact email address
 * @param company Company or organization name
 * @param title Job title or position
 * @param website Personal or company website URL
 * @param linkedin LinkedIn profile URL
 * @param instagram Instagram profile URL
 * @param twitter Twitter/X profile URL
 * @param facebook Facebook profile URL
 * @param github GitHub profile URL
 * @param backgroundType Background type: "SOLID" or "GRADIENT"
 * @param backgroundColor Background color in hex format (e.g., "#FFFFFF")
 * @param selectedGradient Gradient preset name if backgroundType is "GRADIENT"
 * @param profileImageUrl Optional URL to profile image
 * @param cardType Card category type (e.g., "Business", "Personal", "Genel")
 * @param textStyles Map of text style configurations for different text elements
 * @param bio Biography or about section text
 * @param cv CV or resume document URL
 * @param isPublic Whether the card is publicly visible in explore section
 * 
 * @see [CardType] Available card types
 * @see [TextStyleDTO] Text styling configuration
 * @see com.cangzr.neocard.domain.usecase.SaveCardUseCase Saving cards
 * @see com.cangzr.neocard.domain.usecase.GetUserCardsUseCase Retrieving user cards
 * 
 * @since 1.0
 */
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
    val profileImageUrl: String? = "",
    val cardType: String = "Genel",
    val textStyles: Map<String, TextStyleDTO> = emptyMap(),
    val bio: String = "",
    val cv: String = "",
    val isPublic: Boolean = true
) {
    /**
     * Converts the UserCard instance to a [Map] format suitable for Firestore storage.
     * 
     * This method is used when saving the card to Firestore, converting all properties
     * to a key-value map format that Firestore can store.
     * 
     * @return Map containing all card properties as key-value pairs
     * 
     * @see com.cangzr.neocard.data.repository.CardRepository Card repository operations
     */
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

/**
 * ExploreUserCard data model for displaying cards in the explore/public section.
 * 
 * This is a simplified version of [UserCard] used specifically for public card listings
 * to reduce data transfer and improve performance when showing multiple cards.
 * 
 * @param id Unique card identifier
 * @param name Card owner's first name
 * @param surname Card owner's last name
 * @param title Job title or position
 * @param company Company or organization name
 * @param cardType Card category type
 * @param profileImageUrl Profile image URL
 * @param userId Owner's user ID
 * @param isPublic Whether the card is publicly visible
 * 
 * @see [UserCard] Full card model
 * @see com.cangzr.neocard.domain.usecase.GetExploreCardsUseCase Explore cards use case
 * 
 * @since 1.0
 */
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

/**
 * TextStyleDTO data model for card text styling configuration.
 * 
 * This class defines how text elements appear on a card, including font size,
 * weight, style, and color. Used in [UserCard.textStyles] map.
 * 
 * @param fontSize Text font size in sp units, or null for default
 * @param isBold Whether text should be bold
 * @param isItalic Whether text should be italic
 * @param isUnderlined Whether text should be underlined
 * @param color Text color in hex format (e.g., "#000000"), or null for default
 * 
 * @see [UserCard] Card that uses text styles
 * @see com.cangzr.neocard.ui.screens.createcard.viewmodels.TextStyle TextStyle in ViewModel
 * 
 * @since 1.0
 */
data class TextStyleDTO(
    val fontSize: Float? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val color: String? = null
)
