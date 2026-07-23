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
        private const val KEY_FIREBASE_DATABASE_URL = "key_firebase_database_url"
        private const val KEY_FIREBASE_BACKUP_TOKEN = "key_firebase_backup_token"
        private const val KEY_LAST_OVERDUE_DEBT_CHECK = "key_last_overdue_debt_check"
        private const val KEY_ACTIVE_VENDEUR_ID = "key_active_vendeur_id"
        private const val KEY_LAST_EXPIRY_CHECK = "key_last_expiry_check"
    }

    // C.4: yyyy-MM-dd of the last time an expiry-alert notification was shown, so we notify at
    // most once per day instead of on every app launch.
    var lastExpiryCheckDate: String
        get() = prefs.getString(KEY_LAST_EXPIRY_CHECK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_EXPIRY_CHECK, value).apply()

    // B.3/E.2: id of the currently "logged in" Vendeur/Gérant, remembered across app restarts so
    // the cashier doesn't have to re-enter their PIN every launch. -1 means "none active" (either
    // no accounts have been created yet, or nobody has logged in on this device).
    var activeVendeurId: Long
        get() = prefs.getLong(KEY_ACTIVE_VENDEUR_ID, -1L)
        set(value) = prefs.edit().putLong(KEY_ACTIVE_VENDEUR_ID, value).apply()

    // C.3: yyyy-MM-dd of the last time an overdue-debts notification was shown, so we notify at
    // most once per day instead of on every app launch.
    var lastOverdueDebtCheckDate: String
        get() = prefs.getString(KEY_LAST_OVERDUE_DEBT_CHECK, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_OVERDUE_DEBT_CHECK, value).apply()

    var firebaseDatabaseUrl: String
        get() = prefs.getString(KEY_FIREBASE_DATABASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_FIREBASE_DATABASE_URL, value).apply()

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
        get() = prefs.getBoolean(KEY_ACTIVATED, false)
        set(value) = prefs.edit().putBoolean(KEY_ACTIVATED, value).apply()

    var hasSeededNewCategories: Boolean
        get() = prefs.getBoolean("key_has_seeded_new_categories", false)
        set(value) = prefs.edit().putBoolean("key_has_seeded_new_categories", value).apply()

    var excludedProductIds: Set<String>
        get() = prefs.getStringSet("key_excluded_product_ids", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("key_excluded_product_ids", value).apply()

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

    /**
     * High-entropy, never-displayed token used only as the Firebase Realtime Database backup
     * path. Deliberately NOT the same as [installationId], which is a 6-digit number shown (and
     * copyable) on the activation screen and therefore guessable/brute-forceable in ~10^6 tries.
     * With open ".read"/".write" rules (the simplest setup for a non-technical shop owner), the
     * only thing standing between a random visitor and someone's backup data is this path being
     * unguessable, so it needs real entropy (a UUID), not a small public-facing number.
     */
    val firebaseBackupToken: String
        get() {
            var token = prefs.getString(KEY_FIREBASE_BACKUP_TOKEN, "") ?: ""
            if (token.isEmpty()) {
                token = java.util.UUID.randomUUID().toString()
                prefs.edit().putString(KEY_FIREBASE_BACKUP_TOKEN, token).apply()
            }
            return token
        }

    val isTrialExpired: Boolean
        get() = false

    val trialTimeRemainingMs: Long
        get() = 0L
}
