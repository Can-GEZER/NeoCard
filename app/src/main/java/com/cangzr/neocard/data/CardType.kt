package com.cangzr.neocard.data

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.cangzr.neocard.R

enum class CardType {
    BUSINESS,
    PERSONAL,
    EDUCATION,
    SOCIAL,
    FREELANCE;

    fun getTitle(context: Context): String = when (this) {
        BUSINESS -> context.getString(R.string.card_type_business)
        PERSONAL -> context.getString(R.string.card_type_personal)
        EDUCATION -> context.getString(R.string.card_type_education)
        SOCIAL -> context.getString(R.string.card_type_social)
        FREELANCE -> context.getString(R.string.card_type_freelance)
    }

    @Composable
    fun getTitle(): String {
        val context = LocalContext.current
        return getTitle(context)
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