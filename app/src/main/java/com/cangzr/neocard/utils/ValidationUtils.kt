package com.cangzr.neocard.utils

import java.util.regex.Pattern

/**
 * ValidationUtils provides validation functions for user input fields.
 * 
 * This utility class contains validation logic for form fields including email format,
 * phone number format, URL patterns, and string length checks. All validation methods
 * return [ValidationResult] which can be either [ValidationResult.Valid] or
 * [ValidationResult.Invalid] with a user-friendly error message.
 * 
 * **Supported Validations:**
 * - Name and surname (length, character validation)
 * - Email (format, length, domain validation)
 * - Phone (format, length, numeric validation)
 * - Website URLs (pattern validation)
 * - Social media URLs (LinkedIn, GitHub, Twitter, Instagram, Facebook)
 * - Company, title, bio (length validation)
 * 
 * @see [ValidationResult] Validation result type
 * @see com.cangzr.neocard.ui.screens.createcard.viewmodels.CreateCardViewModel ViewModel using validations
 * 
 * @since 1.0
 */
object ValidationUtils {
    
    // Validation constants
    private const val MIN_PHONE_LENGTH = 7
    private const val MAX_PHONE_LENGTH = 20
    private const val MIN_NAME_LENGTH = 2
    private const val MAX_NAME_LENGTH = 50
    private const val MAX_EMAIL_LENGTH = 100
    private const val MAX_COMPANY_LENGTH = 100
    private const val MAX_TITLE_LENGTH = 100
    private const val MAX_BIO_LENGTH = 500
    
    /**
     * Validates name field
     * @param name The name to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateName(name: String, isRequired: Boolean = true): ValidationResult {
        return when {
            name.isEmpty() && isRequired -> ValidationResult.Invalid("İsim gereklidir")
            name.isEmpty() && !isRequired -> ValidationResult.Valid
            name.length < MIN_NAME_LENGTH -> ValidationResult.Invalid("İsim en az $MIN_NAME_LENGTH karakter olmalıdır")
            name.length > MAX_NAME_LENGTH -> ValidationResult.Invalid("İsim en fazla $MAX_NAME_LENGTH karakter olabilir")
            !name.matches(Regex("^[a-zA-ZğüşıöçĞÜŞİÖÇ\\s]+$")) -> ValidationResult.Invalid("İsim sadece harf içerebilir")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates surname field
     * @param surname The surname to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateSurname(surname: String, isRequired: Boolean = false): ValidationResult {
        return when {
            surname.isEmpty() && isRequired -> ValidationResult.Invalid("Soyisim gereklidir")
            surname.isEmpty() && !isRequired -> ValidationResult.Valid
            surname.length < MIN_NAME_LENGTH -> ValidationResult.Invalid("Soyisim en az $MIN_NAME_LENGTH karakter olmalıdır")
            surname.length > MAX_NAME_LENGTH -> ValidationResult.Invalid("Soyisim en fazla $MAX_NAME_LENGTH karakter olabilir")
            !surname.matches(Regex("^[a-zA-ZğüşıöçĞÜŞİÖÇ\\s]+$")) -> ValidationResult.Invalid("Soyisim sadece harf içerebilir")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Enhanced email validation with format and length checks
     * @param email The email to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateEmail(email: String, isRequired: Boolean = false): ValidationResult {
        return when {
            email.isEmpty() && isRequired -> ValidationResult.Invalid("E-posta adresi gereklidir")
            email.isEmpty() && !isRequired -> ValidationResult.Valid
            email.length > MAX_EMAIL_LENGTH -> ValidationResult.Invalid("E-posta adresi çok uzun")
            !isValidEmailFormat(email) -> ValidationResult.Invalid("Geçersiz e-posta formatı")
            !email.contains("@") -> ValidationResult.Invalid("E-posta '@' karakteri içermelidir")
            !email.substringAfter("@").contains(".") -> ValidationResult.Invalid("Geçersiz e-posta alan adı")
            email.count { it == '@' } > 1 -> ValidationResult.Invalid("E-posta tek '@' karakteri içermelidir")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Enhanced phone validation with length and numeric checks
     * @param phone The phone number to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validatePhone(phone: String, isRequired: Boolean = false): ValidationResult {
        // Remove formatting characters for length check
        val digitsOnly = phone.filter { it.isDigit() }
        
        return when {
            phone.isEmpty() && isRequired -> ValidationResult.Invalid("Telefon numarası gereklidir")
            phone.isEmpty() && !isRequired -> ValidationResult.Valid
            !isValidPhoneFormat(phone) -> ValidationResult.Invalid("Geçersiz telefon formatı")
            digitsOnly.length < MIN_PHONE_LENGTH -> ValidationResult.Invalid("Telefon numarası en az $MIN_PHONE_LENGTH rakam içermelidir")
            digitsOnly.length > MAX_PHONE_LENGTH -> ValidationResult.Invalid("Telefon numarası çok uzun")
            digitsOnly.isEmpty() -> ValidationResult.Invalid("Telefon numarası rakam içermelidir")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Enhanced website URL validation with pattern check
     * @param website The website URL to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateWebsite(website: String, isRequired: Boolean = false): ValidationResult {
        return when {
            website.isEmpty() && isRequired -> ValidationResult.Invalid("Website adresi gereklidir")
            website.isEmpty() && !isRequired -> ValidationResult.Valid
            !isValidUrlPattern(website) -> ValidationResult.Invalid("Geçersiz website adresi formatı")
            !website.contains(".") -> ValidationResult.Invalid("Website alan adı içermelidir")
            website.length < 4 -> ValidationResult.Invalid("Website adresi çok kısa")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates company name
     * @param company The company name to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateCompany(company: String, isRequired: Boolean = false): ValidationResult {
        return when {
            company.isEmpty() && isRequired -> ValidationResult.Invalid("Şirket adı gereklidir")
            company.isEmpty() && !isRequired -> ValidationResult.Valid
            company.length > MAX_COMPANY_LENGTH -> ValidationResult.Invalid("Şirket adı çok uzun")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates title/position
     * @param title The title to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateTitle(title: String, isRequired: Boolean = false): ValidationResult {
        return when {
            title.isEmpty() && isRequired -> ValidationResult.Invalid("Ünvan gereklidir")
            title.isEmpty() && !isRequired -> ValidationResult.Valid
            title.length > MAX_TITLE_LENGTH -> ValidationResult.Invalid("Ünvan çok uzun")
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates bio/description
     * @param bio The bio to validate
     * @param isRequired Whether the field is required
     * @return ValidationResult
     */
    fun validateBio(bio: String, isRequired: Boolean = false): ValidationResult {
        return when {
            bio.isEmpty() && isRequired -> ValidationResult.Invalid("Bio gereklidir")
            bio.isEmpty() && !isRequired -> ValidationResult.Valid
            bio.length > MAX_BIO_LENGTH -> ValidationResult.Invalid("Bio en fazla $MAX_BIO_LENGTH karakter olabilir")
            else -> ValidationResult.Valid
        }
    }
    
    // Legacy compatibility methods (Boolean return type)
    
    /**
     * Email format validation (legacy)
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return true
        return isValidEmailFormat(email)
    }
    
    /**
     * Phone format validation (legacy)
     */
    fun isValidPhone(phone: String): Boolean {
        if (phone.isEmpty()) return true
        return isValidPhoneFormat(phone)
    }
    
    // Private helper methods
    
    private fun isValidEmailFormat(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
        )
        return emailPattern.matcher(email).matches()
    }
    
    private fun isValidPhoneFormat(phone: String): Boolean {
        val phonePattern = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]+$")
        return phonePattern.matcher(phone).matches()
    }
    
    private fun isValidUrlPattern(url: String): Boolean {
        val websitePattern = Pattern.compile(
            "^(https?://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
        )
        return websitePattern.matcher(url).matches()
    }
    
    // Website URL validation (legacy)
    fun isValidWebsite(website: String): Boolean {
        if (website.isEmpty()) return true
        return isValidUrlPattern(website)
    }
    
    // Social media validation methods
    
    /**
     * LinkedIn URL validation
     */
    fun validateLinkedIn(linkedin: String): ValidationResult {
        if (linkedin.isEmpty()) return ValidationResult.Valid
        val linkedinPattern = Pattern.compile(
            "^(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+/?$"
        )
        return if (linkedinPattern.matcher(linkedin).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Geçersiz LinkedIn profil adresi")
        }
    }
    
    /**
     * GitHub URL validation
     */
    fun validateGitHub(github: String): ValidationResult {
        if (github.isEmpty()) return ValidationResult.Valid
        val githubPattern = Pattern.compile(
            "^(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9-]+/?$"
        )
        return if (githubPattern.matcher(github).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Geçersiz GitHub profil adresi")
        }
    }
    
    /**
     * Twitter/X URL validation
     */
    fun validateTwitter(twitter: String): ValidationResult {
        if (twitter.isEmpty()) return ValidationResult.Valid
        val twitterPattern = Pattern.compile(
            "^(https?://)?(www\\.)?(twitter\\.com|x\\.com)/[a-zA-Z0-9_]+/?$"
        )
        return if (twitterPattern.matcher(twitter).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Geçersiz Twitter/X profil adresi")
        }
    }
    
    /**
     * Instagram URL validation
     */
    fun validateInstagram(instagram: String): ValidationResult {
        if (instagram.isEmpty()) return ValidationResult.Valid
        val instagramPattern = Pattern.compile(
            "^(https?://)?(www\\.)?instagram\\.com/[a-zA-Z0-9_.]+/?$"
        )
        return if (instagramPattern.matcher(instagram).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Geçersiz Instagram profil adresi")
        }
    }
    
    /**
     * Facebook URL validation
     */
    fun validateFacebook(facebook: String): ValidationResult {
        if (facebook.isEmpty()) return ValidationResult.Valid
        val facebookPattern = Pattern.compile(
            "^(https?://)?(www\\.)?facebook\\.com/[a-zA-Z0-9.]+/?$"
        )
        return if (facebookPattern.matcher(facebook).matches()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid("Geçersiz Facebook profil adresi")
        }
    }
    
    // Legacy social media validation (Boolean return type)
    
    fun isValidLinkedIn(linkedin: String): Boolean {
        if (linkedin.isEmpty()) return true
        val linkedinPattern = Pattern.compile(
            "^(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+/?$"
        )
        return linkedinPattern.matcher(linkedin).matches()
    }
    
    fun isValidGitHub(github: String): Boolean {
        if (github.isEmpty()) return true
        val githubPattern = Pattern.compile(
            "^(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9-]+/?$"
        )
        return githubPattern.matcher(github).matches()
    }
    
    fun isValidTwitter(twitter: String): Boolean {
        if (twitter.isEmpty()) return true
        val twitterPattern = Pattern.compile(
            "^(https?://)?(www\\.)?(twitter\\.com|x\\.com)/[a-zA-Z0-9_]+/?$"
        )
        return twitterPattern.matcher(twitter).matches()
    }
    
    fun isValidInstagram(instagram: String): Boolean {
        if (instagram.isEmpty()) return true
        val instagramPattern = Pattern.compile(
            "^(https?://)?(www\\.)?instagram\\.com/[a-zA-Z0-9_.]+/?$"
        )
        return instagramPattern.matcher(instagram).matches()
    }
    
    fun isValidFacebook(facebook: String): Boolean {
        if (facebook.isEmpty()) return true
        val facebookPattern = Pattern.compile(
            "^(https?://)?(www\\.)?facebook\\.com/[a-zA-Z0-9.]+/?$"
        )
        return facebookPattern.matcher(facebook).matches()
    }
    
    // Input filter methods
    
    /**
     * Phone input filter - only valid characters
     */
    fun filterPhoneInput(input: String): String {
        return input.filter { char ->
            char.isDigit() || char == '+' || char == '-' || char == '(' || char == ')' || char == ' '
        }
    }
    
    /**
     * Email input filter - valid email characters
     */
    fun filterEmailInput(input: String): String {
        return input.filter { char ->
            char.isLetterOrDigit() || char == '@' || char == '.' || char == '_' || char == '-' || char == '+'
        }
    }
    
    /**
     * Website input filter - valid URL characters
     */
    fun filterWebsiteInput(input: String): String {
        return input.filter { char ->
            char.isLetterOrDigit() || char == '.' || char == '/' || char == ':' || char == '-' || char == '_'
        }
    }
    
    /**
     * Social media input filter - common social media characters
     */
    fun filterSocialInput(input: String): String {
        return input.filter { char ->
            char.isLetterOrDigit() || char == '.' || char == '/' || char == ':' || char == '-' || char == '_' || char == '@'
        }
    }
}
