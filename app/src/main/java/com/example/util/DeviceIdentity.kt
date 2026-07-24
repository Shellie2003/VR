package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

/**
 * Identité STABLE de l'appareil, utilisée pour lier chaque poste à sa licence.
 *
 * Pourquoi pas l'ID d'installation à 6 chiffres ? Parce qu'il vit dans les préférences de l'app :
 * effacer les données de l'app en fabrique un neuf, gratuitement. Un client pouvait donc se
 * présenter au fournisseur avec une « nouvelle identité » à volonté. `ANDROID_ID`, lui, est fourni
 * par le système : constant pour un couple (appareil, clé de signature de l'app), il survit aux
 * mises à jour et aux effacements de données de l'app, et ne change qu'à une réinitialisation
 * d'usine complète du téléphone — un coût suffisamment dissuasif.
 *
 * C'est ce code (pas l'ID à 6 chiffres) que le fournisseur enregistre dans la liste `appareils`
 * du nœud de licence : la liste désigne des téléphones physiques, pas des installations.
 */
object DeviceIdentity {

    private const val KEY_FALLBACK_DEVICE_ID = "key_fallback_device_id"

    /**
     * Forme canonique : hex minuscule, utilisée comme clé dans `licences/<n°>/appareils/<id>`.
     * `ANDROID_ID` fait normalement 16 caractères hex ; sur les rares appareils où il est absent
     * ou connu défectueux, on retombe sur un identifiant aléatoire persisté — moins robuste
     * (il saute avec les données de l'app) mais qui ne casse jamais l'activation.
     */
    @SuppressLint("HardwareIds")
    fun rawId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        // "9774d56d682e549c" : valeur unique partagée par toute une génération d'appareils bugués —
        // l'accepter reviendrait à donner le même « téléphone » à des milliers de clients.
        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            return androidId.lowercase()
        }
        val prefs = context.getSharedPreferences("pos_app_prefs", Context.MODE_PRIVATE)
        var fallback = prefs.getString(KEY_FALLBACK_DEVICE_ID, "") ?: ""
        if (fallback.isEmpty()) {
            fallback = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_FALLBACK_DEVICE_ID, fallback).apply()
        }
        return fallback
    }

    /**
     * Forme lisible affichée sur l'écran d'activation et dictée au fournisseur :
     * "A3F2-9B10-C44D-E5F6". Toujours dérivée de [rawId], jamais stockée séparément.
     */
    fun formatted(context: Context): String =
        rawId(context).uppercase().chunked(4).joinToString("-")
}
