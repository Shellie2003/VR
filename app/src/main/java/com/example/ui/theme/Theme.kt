package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    primaryColorOverride: Color? = null,
    content: @Composable () -> Unit,
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = if (primaryColorOverride != null) {
        baseScheme.copy(
            primary = primaryColorOverride,
            // `onPrimary` DOIT être remplacé avec `primary`, sinon le couple est cassé en thème
            // sombre. Le schéma sombre a été conçu pour une primaire CLAIRE (menthe `0xFFC1ECD4`),
            // donc son `onPrimary` est du quasi-noir (`0xFF002114`). Or les quatre accents
            // proposés au gérant sont tous SOMBRES (vert, orange brûlé, indigo, rose foncé) :
            // laisser `onPrimary` en place donnait du quasi-noir sur un accent sombre, soit un
            // rapport de contraste d'environ 1,1:1 — le libellé et l'icône disparaissent.
            // Invisible depuis les sites d'appel, car c'est `contentColorFor(primary)` qui va
            // chercher cette valeur : un bouton `containerColor = themeColor` SANS `contentColor`
            // la reçoit sans que rien ne le dise. Le blanc convient aux quatre accents, et dans
            // les deux thèmes, puisqu'ils sont tous sombres.
            onPrimary = Color.White,
            primaryContainer = primaryColorOverride.copy(alpha = 0.15f)
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
