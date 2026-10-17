package com.example.ui.partage.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.util.VarotraPalette

/**
 * Le thème de Varotra pour la version de bureau.
 *
 * Il ne redéfinit aucune couleur : il enveloppe les entiers de [VarotraPalette] dans le type
 * `Color` de Compose Multiplatform, là où `:app/ui/theme/Color.kt` les enveloppe dans celui
 * d'androidx. C'est ce qui lève la dette assumée à l'ouverture du chantier desktop — la fenêtre de
 * bureau portait jusqu'ici quatre couleurs recopiées à la main, faute de pouvoir lire un thème qui
 * vivait dans un module Android.
 *
 * `primaireRemplacee` reprend le principe de `primaryColorOverride` côté Android : le gérant
 * choisit sa couleur d'accent dans les paramètres, et tout l'écran s'y conforme. La dérivation du
 * conteneur (`alpha = 0.15f`) est identique, pour que les deux plateformes rendent la même nuance.
 */
@Composable
fun VarotraTheme(
    sombre: Boolean = false,
    primaireRemplacee: Color? = null,
    contenu: @Composable () -> Unit
) {
    val base = if (sombre) SchemaSombre else SchemaClair
    val schema = if (primaireRemplacee != null) {
        base.copy(
            primary = primaireRemplacee,
            primaryContainer = primaireRemplacee.copy(alpha = 0.15f)
        )
    } else {
        base
    }

    MaterialTheme(colorScheme = schema, content = contenu)
}

private val SchemaClair = lightColorScheme(
    primary = Color(VarotraPalette.LIGHT_PRIMARY),
    onPrimary = Color(VarotraPalette.LIGHT_ON_PRIMARY),
    primaryContainer = Color(VarotraPalette.LIGHT_PRIMARY_CONTAINER),
    onPrimaryContainer = Color(VarotraPalette.LIGHT_ON_PRIMARY_CONTAINER),
    secondary = Color(VarotraPalette.LIGHT_SECONDARY),
    onSecondary = Color(VarotraPalette.LIGHT_ON_SECONDARY),
    secondaryContainer = Color(VarotraPalette.LIGHT_SECONDARY_CONTAINER),
    onSecondaryContainer = Color(VarotraPalette.LIGHT_ON_SECONDARY_CONTAINER),
    background = Color(VarotraPalette.LIGHT_BACKGROUND),
    onBackground = Color(VarotraPalette.LIGHT_ON_BACKGROUND),
    surface = Color(VarotraPalette.LIGHT_SURFACE),
    onSurface = Color(VarotraPalette.LIGHT_ON_SURFACE),
    surfaceVariant = Color(VarotraPalette.LIGHT_SURFACE_VARIANT),
    onSurfaceVariant = Color(VarotraPalette.LIGHT_ON_SURFACE_VARIANT),
    outline = Color(VarotraPalette.LIGHT_OUTLINE),
    outlineVariant = Color(VarotraPalette.LIGHT_OUTLINE_VARIANT),
    error = Color(VarotraPalette.LIGHT_ERROR),
    onError = Color(VarotraPalette.LIGHT_ON_ERROR),
    errorContainer = Color(VarotraPalette.LIGHT_ERROR_CONTAINER),
    onErrorContainer = Color(VarotraPalette.LIGHT_ON_ERROR_CONTAINER)
)

private val SchemaSombre = darkColorScheme(
    primary = Color(VarotraPalette.DARK_PRIMARY),
    onPrimary = Color(VarotraPalette.DARK_ON_PRIMARY),
    primaryContainer = Color(VarotraPalette.DARK_PRIMARY_CONTAINER),
    onPrimaryContainer = Color(VarotraPalette.DARK_ON_PRIMARY_CONTAINER),
    secondary = Color(VarotraPalette.DARK_SECONDARY),
    onSecondary = Color(VarotraPalette.DARK_ON_SECONDARY),
    secondaryContainer = Color(VarotraPalette.DARK_SECONDARY_CONTAINER),
    onSecondaryContainer = Color(VarotraPalette.DARK_ON_SECONDARY_CONTAINER),
    background = Color(VarotraPalette.DARK_BACKGROUND),
    onBackground = Color(VarotraPalette.DARK_ON_BACKGROUND),
    surface = Color(VarotraPalette.DARK_SURFACE),
    onSurface = Color(VarotraPalette.DARK_ON_SURFACE),
    surfaceVariant = Color(VarotraPalette.DARK_SURFACE_VARIANT),
    onSurfaceVariant = Color(VarotraPalette.DARK_ON_SURFACE_VARIANT),
    outline = Color(VarotraPalette.DARK_OUTLINE),
    outlineVariant = Color(VarotraPalette.DARK_OUTLINE_VARIANT),
    error = Color(VarotraPalette.DARK_ERROR),
    onError = Color(VarotraPalette.DARK_ON_ERROR),
    errorContainer = Color(VarotraPalette.DARK_ERROR_CONTAINER),
    onErrorContainer = Color(VarotraPalette.DARK_ON_ERROR_CONTAINER)
)
