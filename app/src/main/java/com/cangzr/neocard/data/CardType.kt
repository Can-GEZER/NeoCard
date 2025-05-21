package com.cangzr.neocard.data

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cangzr.neocard.R

enum class CardType {
    BUSINESS,
    PERSONAL,
    EDUCATION,
    SOCIAL,
    FREELANCE;

    fun getTitle(): String = when (this) {
        BUSINESS -> "İş Kartviziti"
        PERSONAL -> "Kişisel Kartvizit"
        EDUCATION -> "Eğitim Kartviziti"
        SOCIAL -> "Sosyal Kartvizit"
        FREELANCE -> "Freelance Kartvizit"
    }

    fun getIcon(): Int = when (this) {
        BUSINESS -> R.drawable.business
        PERSONAL -> R.drawable.personal
        EDUCATION -> R.drawable.education
        SOCIAL -> R.drawable.social
        FREELANCE -> R.drawable.freelance
    }

    @Composable
    fun getColor(): Color = when (this) {
        BUSINESS -> MaterialTheme.colorScheme.primary
        PERSONAL -> MaterialTheme.colorScheme.secondary
        EDUCATION -> Color(0xFF4CAF50) // Yeşil
        SOCIAL -> Color(0xFFE91E63) // Pembe
        FREELANCE -> Color(0xFFFF9800) // Turuncu
    }
} 