package com.cangzr.neocard.utils

import java.util.regex.Pattern

object ValidationUtils {
    
    // Email validation
    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return true // Boş email geçerli (opsiyonel alan)
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        )
        return emailPattern.matcher(email).matches()
    }
    
    // Phone validation - sadece rakam, +, -, (, ), boşluk karakterleri
    fun isValidPhone(phone: String): Boolean {
        if (phone.isEmpty()) return true // Boş telefon geçerli (opsiyonel alan)
        val phonePattern = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]+$")
        return phonePattern.matcher(phone).matches()
    }
    
    // Website URL validation
    fun isValidWebsite(website: String): Boolean {
        if (website.isEmpty()) return true // Boş website geçerli (opsiyonel alan)
        val websitePattern = Pattern.compile(
            "^(https?://)?(www\\.)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
        )
        return websitePattern.matcher(website).matches()
    }
    
    // LinkedIn validation
    fun isValidLinkedIn(linkedin: String): Boolean {
        if (linkedin.isEmpty()) return true
        val linkedinPattern = Pattern.compile(
            "^(https?://)?(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+/?$"
        )
        return linkedinPattern.matcher(linkedin).matches()
    }
    
    // GitHub validation
    fun isValidGitHub(github: String): Boolean {
        if (github.isEmpty()) return true
        val githubPattern = Pattern.compile(
            "^(https?://)?(www\\.)?github\\.com/[a-zA-Z0-9-]+/?$"
        )
        return githubPattern.matcher(github).matches()
    }
    
    // Twitter validation
    fun isValidTwitter(twitter: String): Boolean {
        if (twitter.isEmpty()) return true
        val twitterPattern = Pattern.compile(
            "^(https?://)?(www\\.)?(twitter\\.com|x\\.com)/[a-zA-Z0-9_]+/?$"
        )
        return twitterPattern.matcher(twitter).matches()
    }
    
    // Instagram validation
    fun isValidInstagram(instagram: String): Boolean {
        if (instagram.isEmpty()) return true
        val instagramPattern = Pattern.compile(
            "^(https?://)?(www\\.)?instagram\\.com/[a-zA-Z0-9_.]+/?$"
        )
        return instagramPattern.matcher(instagram).matches()
    }
    
    // Facebook validation
    fun isValidFacebook(facebook: String): Boolean {
        if (facebook.isEmpty()) return true
        val facebookPattern = Pattern.compile(
            "^(https?://)?(www\\.)?facebook\\.com/[a-zA-Z0-9.]+/?$"
        )
        return facebookPattern.matcher(facebook).matches()
    }
    
    // Phone input filter - sadece geçerli karakterleri kabul et
    fun filterPhoneInput(input: String): String {
        return input.filter { char ->
            char.isDigit() || char == '+' || char == '-' || char == '(' || char == ')' || char == ' '
        }
    }
    
    // Email input filter - geçerli email karakterleri
    fun filterEmailInput(input: String): String {
        return input.filter { char ->
            char.isLetterOrDigit() || char == '@' || char == '.' || char == '_' || char == '-' || char == '+'
        }
    }
    
    // Website input filter - geçerli URL karakterleri
    fun filterWebsiteInput(input: String): String {
        return input.filter { char ->
            char.isLetterOrDigit() || char == '.' || char == '/' || char == ':' || char == '-' || char == '_'
        }
    }
    
    // Social media input filter - genel sosyal medya karakterleri
    fun filterSocialInput(input: String): String {
        return input.filter { char ->
            char.isLetterOrDigit() || char == '.' || char == '/' || char == ':' || char == '-' || char == '_' || char == '@'
        }
    }
}
