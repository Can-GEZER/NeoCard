package com.cangzr.neocard.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.cangzr.neocard.data.CardType

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
    val skills: List<Skill> = emptyList(),
    val isPublic: Boolean = true
) {
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
            "skills" to skills.map { it.toMap() },
            "isPublic" to isPublic
        )
    }
    
    companion object {
        fun fromMap(id: String, map: Map<String, Any>): UserCard {
            val skillsList = (map["skills"] as? List<*>)?.mapNotNull { 
                if (it is Map<*, *>) {
                    Skill.fromMap(it as Map<String, Any>)
                } else null
            } ?: emptyList()
            
            return UserCard(
                id = id,
                name = map["name"] as? String ?: "",
                surname = map["surname"] as? String ?: "",
                phone = map["phone"] as? String ?: "",
                email = map["email"] as? String ?: "",
                company = map["company"] as? String ?: "",
                title = map["title"] as? String ?: "",
                website = map["website"] as? String ?: "",
                linkedin = map["linkedin"] as? String ?: "",
                instagram = map["instagram"] as? String ?: "",
                twitter = map["twitter"] as? String ?: "",
                facebook = map["facebook"] as? String ?: "",
                github = map["github"] as? String ?: "",
                backgroundType = map["backgroundType"] as? String ?: "SOLID",
                backgroundColor = map["backgroundColor"] as? String ?: "#FFFFFF",
                selectedGradient = map["selectedGradient"] as? String ?: "",
                profileImageUrl = map["profileImageUrl"] as? String,
                cardType = map["cardType"] as? String ?: "Genel",
                textStyles = (map["textStyles"] as? Map<*, *>)?.mapNotNull { (key, value) ->
                    if (value is Map<*, *>) {
                        val styleMap = value as Map<String, Any>
                        key.toString() to TextStyleDTO(
                            fontSize = (styleMap["fontSize"] as? Number)?.toFloat(),
                            isBold = styleMap["isBold"] as? Boolean ?: false,
                            isItalic = styleMap["isItalic"] as? Boolean ?: false,
                            isUnderlined = styleMap["isUnderlined"] as? Boolean ?: false,
                            color = styleMap["color"] as? String
                        )
                    } else null
                }?.toMap() ?: emptyMap(),
                bio = map["bio"] as? String ?: "",
                cv = map["cv"] as? String ?: "",
                skills = skillsList,
                isPublic = map["isPublic"] as? Boolean ?: true
            )
        }
    }
}

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

data class TextStyleDTO(
    val fontSize: Float? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val color: String? = null
)
