package com.example.util

import android.content.Context

/**
 * Fiche d'identité de l'épicerie, telle qu'elle apparaît en en-tête des documents imprimés.
 *
 * Elle est lue directement depuis les préférences par [depuis] au moment de générer un PDF :
 * ainsi le logo et les coordonnées se retrouvent automatiquement sur TOUS les documents (reçus de
 * caisse, rapports, exports de stock, de dettes, de caisse), sans avoir à faire circuler ces
 * informations à travers chaque écran qui déclenche un export.
 */
data class ShopInfo(
    val nom: String,
    val adresse: String = "",
    val telephone: String = "",
    val nif: String = "",
    val stat: String = "",
    val logoPath: String = "",
    val piedDePage: String = ""
) {
    /** Les lignes de coordonnées à imprimer sous le nom, dans l'ordre, en sautant les vides. */
    fun lignesCoordonnees(): List<String> = buildList {
        if (adresse.isNotBlank()) add(adresse)
        if (telephone.isNotBlank()) add("Tél: $telephone")
        val identifiants = listOfNotNull(
            nif.takeIf { it.isNotBlank() }?.let { "NIF: $it" },
            stat.takeIf { it.isNotBlank() }?.let { "STAT: $it" }
        )
        if (identifiants.isNotEmpty()) add(identifiants.joinToString("  •  "))
    }

    companion object {
        fun depuis(context: Context): ShopInfo {
            val prefs = AppPreferences(context)
            return ShopInfo(
                nom = prefs.groceryName,
                adresse = prefs.shopAddress,
                telephone = prefs.shopPhone,
                nif = prefs.shopNif,
                stat = prefs.shopStat,
                logoPath = prefs.shopLogoPath,
                piedDePage = prefs.shopReceiptFooter
            )
        }
    }
}
