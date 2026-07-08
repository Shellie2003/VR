package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VarotraPrimaryFixedDim,
    onPrimary = VarotraPrimary,
    primaryContainer = VarotraPrimaryContainer,
    onPrimaryContainer = VarotraOnPrimaryContainer,
    secondary = VarotraSecondaryContainer,
    onSecondary = VarotraOnSecondaryContainer,
    background = VarotraOnBackground,
    surface = VarotraOnBackground,
    onBackground = VarotraBackground,
    onSurface = VarotraBackground,
    surfaceVariant = VarotraOnSurfaceVariant,
    onSurfaceVariant = VarotraOutlineVariant,
    outline = VarotraOutline,
    error = VarotraError,
    errorContainer = VarotraErrorContainer,
    onErrorContainer = VarotraOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = VarotraPrimary,
    onPrimary = VarotraBackground,
    primaryContainer = VarotraPrimaryContainer,
    onPrimaryContainer = VarotraOnPrimaryContainer,
    secondary = VarotraSecondary,
    secondaryContainer = VarotraSecondaryContainer,
    onSecondary = VarotraBackground,
    onSecondaryContainer = VarotraOnSecondaryContainer,
    background = VarotraBackground,
    surface = VarotraSurface,
    onBackground = VarotraOnBackground,
    onSurface = VarotraOnSurface,
    surfaceVariant = VarotraSurfaceContainer,
    onSurfaceVariant = VarotraOnSurfaceVariant,
    outline = VarotraOutline,
    outlineVariant = VarotraOutlineVariant,
    error = VarotraError,
    errorContainer = VarotraErrorContainer,
    onErrorContainer = VarotraOnErrorContainer
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
