package com.example.util

/**
 * Palette de couleurs pour distinguer visuellement les étagères et les cases (niveaux) du plan
 * d'étagères. Fonction pure, sans dépendance Android/Compose : la conversion en `Color` réelle se
 * fait à l'écran, une fois la valeur validée ici.
 */
object EtagereColors {

    val PALETTE: List<String> = listOf(
        "#F44336", "#FF9800", "#FFC107", "#4CAF50", "#009688", "#03A9F4",
        "#3F51B5", "#9C27B0", "#795548", "#607D8B", "#E91E63", "#8BC34A"
    )

    private val HEX_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

    /**
     * Normalise une couleur saisie ou reçue par synchronisation : seul un hex RGB à 6 chiffres
     * avec son `#` est accepté. Toute autre valeur (vide, corrompue par une synchronisation
     * partielle, ancien format, nom de couleur) est traitée comme « pas de couleur » plutôt que de
     * risquer un crash au moment de l'afficher.
     */
    fun normaliser(hex: String?): String? {
        if (hex.isNullOrBlank()) return null
        val nettoye = hex.trim()
        return if (HEX_REGEX.matches(nettoye)) nettoye.uppercase() else null
    }
}
