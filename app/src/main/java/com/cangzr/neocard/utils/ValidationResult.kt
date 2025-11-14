package com.cangzr.neocard.utils

sealed class ValidationResult {
    data object Valid : ValidationResult()
    
    data class Invalid(val message: String) : ValidationResult()
    
    fun isValid(): Boolean = this is Valid
    
    fun getErrorOrNull(): String? = when (this) {
        is Invalid -> message
        is Valid -> null
    }
}

