package com.example.util

/**
 * Les valeurs de la charte graphique de Varotra, en entiers ARGB nus.
 *
 * **Pourquoi ici et non dans un module d'interface.** Les couleurs de la marque étaient écrites
 * deux fois : une fois dans `:app/ui/theme/Color.kt` (en `androidx.compose.ui.graphics.Color`) et
 * une fois en dur dans la fenêtre de bureau, qui ne pouvait pas lire le thème d'un module Android
 * — dette explicitement assumée à l'ouverture du chantier desktop. Recopier la palette dans le
 * module d'interface partagé n'aurait pas levé cette dette, seulement déplacé la deuxième copie.
 *
 * Des entiers et non des `Color` : c'est précisément ce qui permet aux deux plateformes de lire la
 * même source. `androidx.compose.ui.graphics.Color` (Android) et son équivalent Compose
 * Multiplatform sont deux types distincts venant de deux artefacts différents ; une palette typée
 * obligerait donc à choisir un camp. Chaque plateforme enveloppe ces entiers dans SON type.
 *
 * Ces valeurs sont celles que les boutiques voient aujourd'hui : elles ne doivent pas changer à
 * l'occasion d'un déplacement de fichier. `VarotraPaletteTest` les épingle pour cette raison, et
 * pour cette raison seulement — un test qui recopie une constante ne vaut que pendant une
 * extraction, où le risque réel est la faute de frappe.
 */
object VarotraPalette {

    // --- Mode clair -----------------------------------------------------------------------------

    const val LIGHT_PRIMARY = 0xFF012D1DL
    const val LIGHT_ON_PRIMARY = 0xFFFFFFFFL
    const val LIGHT_PRIMARY_CONTAINER = 0xFF1B4332L
    const val LIGHT_ON_PRIMARY_CONTAINER = 0xFF86AF99L

    const val LIGHT_SECONDARY = 0xFF855300L
    const val LIGHT_ON_SECONDARY = 0xFFFFFFFFL
    const val LIGHT_SECONDARY_CONTAINER = 0xFFFEA619L
    const val LIGHT_ON_SECONDARY_CONTAINER = 0xFF684000L

    const val LIGHT_BACKGROUND = 0xFFFBF9F8L
    const val LIGHT_ON_BACKGROUND = 0xFF1B1C1CL
    const val LIGHT_SURFACE = 0xFFFBF9F8L
    const val LIGHT_ON_SURFACE = 0xFF1B1C1CL
    const val LIGHT_SURFACE_VARIANT = 0xFFEFEDEDL
    const val LIGHT_ON_SURFACE_VARIANT = 0xFF414844L

    const val LIGHT_OUTLINE = 0xFF717973L
    const val LIGHT_OUTLINE_VARIANT = 0xFFC1C8C2L

    const val LIGHT_ERROR = 0xFFBA1A1AL
    const val LIGHT_ON_ERROR = 0xFFFFFFFFL
    const val LIGHT_ERROR_CONTAINER = 0xFFFFDAD6L
    const val LIGHT_ON_ERROR_CONTAINER = 0xFF93000AL

    // --- Mode sombre ----------------------------------------------------------------------------

    const val DARK_PRIMARY = 0xFFC1ECD4L
    const val DARK_ON_PRIMARY = 0xFF002114L
    const val DARK_PRIMARY_CONTAINER = 0xFF1B4332L
    const val DARK_ON_PRIMARY_CONTAINER = 0xFF86AF99L

    const val DARK_SECONDARY = 0xFFFEA619L
    const val DARK_ON_SECONDARY = 0xFF684000L
    const val DARK_SECONDARY_CONTAINER = 0xFF653E00L
    const val DARK_ON_SECONDARY_CONTAINER = 0xFFFEA619L

    const val DARK_BACKGROUND = 0xFF002114L
    const val DARK_ON_BACKGROUND = 0xFFFBF9F8L
    const val DARK_SURFACE = 0xFF002114L
    const val DARK_ON_SURFACE = 0xFFFBF9F8L
    const val DARK_SURFACE_VARIANT = 0xFF1B4332L
    const val DARK_ON_SURFACE_VARIANT = 0xFFC1C8C2L

    const val DARK_OUTLINE = 0xFF8B938DL
    const val DARK_OUTLINE_VARIANT = 0xFF414844L

    const val DARK_ERROR = 0xFFFFB4ABL
    const val DARK_ON_ERROR = 0xFF690005L
    const val DARK_ERROR_CONTAINER = 0xFF93000AL
    const val DARK_ON_ERROR_CONTAINER = 0xFFFFDAD6L
}
