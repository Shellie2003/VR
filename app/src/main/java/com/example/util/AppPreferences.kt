package com.example.util

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LANG = "key_language"
        private const val KEY_ACTIVATED = "key_is_activated"
        private const val KEY_INSTALLATION_ID = "key_installation_id"
        private const val KEY_TRIAL_START = "key_trial_start"
        private const val KEY_GROCERY_NAME = "key_grocery_name"
        private const val KEY_COLOR_THEME = "key_color_theme"
        private const val KEY_SHOP_MODE = "key_shop_mode"
        private const val KEY_THEME_MODE = "key_theme_mode"
    }

    var language: String
        get() = prefs.getString(KEY_LANG, "mg") ?: "mg"
        set(value) = prefs.edit().putString(KEY_LANG, value).apply()

    var shopMode: String
        get() = prefs.getString(KEY_SHOP_MODE, "retail") ?: "retail"
        set(value) = prefs.edit().putString(KEY_SHOP_MODE, value).apply()

    var groceryName: String
        get() = prefs.getString(KEY_GROCERY_NAME, "Varotra") ?: "Varotra"
        set(value) = prefs.edit().putString(KEY_GROCERY_NAME, value).apply()

    var colorTheme: String
        get() = prefs.getString(KEY_COLOR_THEME, "emerald") ?: "emerald"
        set(value) = prefs.edit().putString(KEY_COLOR_THEME, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "light") ?: "light"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var isActivated: Boolean
        get() = true
        set(value) = prefs.edit().putBoolean(KEY_ACTIVATED, value).apply()

    var trialStartTime: Long
        get() {
            val start = prefs.getLong(KEY_TRIAL_START, 0L)
            if (start == 0L) {
                val now = System.currentTimeMillis()
                prefs.edit().putLong(KEY_TRIAL_START, now).apply()
                return now
            }
            return start
        }
        set(value) = prefs.edit().putLong(KEY_TRIAL_START, value).apply()

    val installationId: String
        get() {
            var instId = prefs.getString(KEY_INSTALLATION_ID, "") ?: ""
            if (instId.isEmpty()) {
                val randomNum = (100000..999999).random()
                instId = randomNum.toString()
                prefs.edit().putString(KEY_INSTALLATION_ID, instId).apply()
            }
            return instId
        }

    val isTrialExpired: Boolean
        get() = false

    val trialTimeRemainingMs: Long
        get() = 0L
}
