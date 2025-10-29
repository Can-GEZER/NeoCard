package com.cangzr.neocard.utils

/**
 * Validation result sealed class
 * Represents the result of a validation operation
 */
sealed class ValidationResult {
    /**
     * Validation passed
     */
    data object Valid : ValidationResult()
    
    /**
     * Validation failed with error message
     * @param message User-friendly error message
     */
    data class Invalid(val message: String) : ValidationResult()
    
    /**
     * Check if validation is valid
     */
    fun isValid(): Boolean = this is Valid
    
    /**
     * Get error message if invalid
     */
    fun getErrorOrNull(): String? = when (this) {
        is Invalid -> message
        is Valid -> null
    }
}

