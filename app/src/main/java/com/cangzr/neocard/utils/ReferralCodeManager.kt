package com.cangzr.neocard.utils

import android.content.Context
import android.content.SharedPreferences

object ReferralCodeManager {
    private const val PREF_NAME = "referral_pref"
    private const val KEY_REFERRAL_CODE = "pending_referral_code"

    fun saveReferralCode(context: Context, referralCode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_REFERRAL_CODE, referralCode).apply()
    }

    fun getAndClearReferralCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_REFERRAL_CODE, null)
        if (code != null) {
            prefs.edit().remove(KEY_REFERRAL_CODE).apply()
        }
        return code
    }

    fun getReferralCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REFERRAL_CODE, null)
    }

    fun clearReferralCode(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_REFERRAL_CODE).apply()
    }
}

