package com.cangzr.neocard.ui.screens.carddetail.utils

object UrlUtils {
    
    /**
     * Sosyal medya URL'lerini düzenleme
     */
    fun formatSocialUrl(url: String, domain: String): String {
        return when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains(domain) -> "https://$url"
            else -> "https://$domain/$url"
        }
    }
}
