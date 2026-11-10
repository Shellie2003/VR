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

    /**
     * La couleur en entier ARGB opaque, ou `null` si [hex] n'est pas exploitable.
     *
     * **Un entier et non un `Color`**, exactement pour la raison de `VarotraPalette` (section 84) :
     * `androidx.compose.ui.graphics.Color` et son équivalent Compose Multiplatform viennent de deux
     * artefacts différents, donc une valeur typée obligerait ce module à choisir un camp. Chaque
     * plateforme enveloppe l'entier dans SON type.
     *
     * Elle remplace le `android.graphics.Color.parseColor` qu'utilisait l'écran Android : cette
     * fonction n'existe pas hors d'Android, et le port du plan d'étagères sur le bureau aurait
     * donc dû écrire sa propre conversion — soit deux analyseurs de couleur pour un seul format.
     *
     * [normaliser] est appelée d'abord : une valeur corrompue (synchronisation partielle, ancien
     * format, nom de couleur) rend `null` plutôt que de lever. L'octet alpha est forcé à `FF`,
     * sans quoi la couleur serait **totalement invisible** — le piège épinglé par
     * `VarotraPaletteTest`.
     */
    fun versArgb(hex: String?): Int? {
        val normalise = normaliser(hex) ?: return null
        val rgb = normalise.removePrefix("#").toLongOrNull(16) ?: return null
        return (0xFF000000L or rgb).toInt()
    }
}
