package com.cangzr.neocard.ui.screens.carddetail.utils

import android.content.Context
import com.cangzr.neocard.R

object ValidationUtils {
    
    fun validateName(value: String, context: Context): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.name_empty)
            value.length < 2 -> context.getString(R.string.name_min_length)
            else -> null
        }
    }

    fun validateSurname(value: String, context: Context): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.surname_empty)
            value.length < 2 -> context.getString(R.string.surname_min_length)
            else -> null
        }
    }

    fun validatePhone(value: String, context: Context): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.phone_empty)
            !android.util.Patterns.PHONE.matcher(value).matches() -> context.getString(R.string.phone_invalid)
            else -> null
        }
    }

    fun validateEmail(value: String, context: Context): String? {
        return when {
            value.isEmpty() -> context.getString(R.string.email_empty)
            !android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches() -> context.getString(R.string.email_invalid)
            else -> null
        }
    }
}
