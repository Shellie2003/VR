package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.util.VarotraPalette

// Les valeurs de la charte vivent désormais dans `:core:logic` (`VarotraPalette`), en entiers nus.
// Elles étaient écrites deux fois — ici, et en dur dans la fenêtre de bureau, qui ne peut pas lire
// le thème d'un module Android. Ce fichier ne fait plus que les envelopper dans le type `Color`
// d'androidx ; la version de bureau les enveloppe dans celui de Compose Multiplatform. C'est la
// seule façon de partager une palette entre deux artefacts Compose distincts, qui n'ont pas le
// même type `Color`.
//
// Aucune valeur n'a changé à l'occasion de ce déplacement : `VarotraPaletteTest` épingle les
// couleurs identitaires et vérifie que chacune porte bien son octet alpha.

// Light Theme Colors
val LightPrimary = Color(VarotraPalette.LIGHT_PRIMARY)
val LightOnPrimary = Color(VarotraPalette.LIGHT_ON_PRIMARY)
val LightPrimaryContainer = Color(VarotraPalette.LIGHT_PRIMARY_CONTAINER)
val LightOnPrimaryContainer = Color(VarotraPalette.LIGHT_ON_PRIMARY_CONTAINER)

val LightSecondary = Color(VarotraPalette.LIGHT_SECONDARY)
val LightOnSecondary = Color(VarotraPalette.LIGHT_ON_SECONDARY)
val LightSecondaryContainer = Color(VarotraPalette.LIGHT_SECONDARY_CONTAINER)
val LightOnSecondaryContainer = Color(VarotraPalette.LIGHT_ON_SECONDARY_CONTAINER)

val LightBackground = Color(VarotraPalette.LIGHT_BACKGROUND)
val LightOnBackground = Color(VarotraPalette.LIGHT_ON_BACKGROUND)
val LightSurface = Color(VarotraPalette.LIGHT_SURFACE)
val LightOnSurface = Color(VarotraPalette.LIGHT_ON_SURFACE)
val LightSurfaceVariant = Color(VarotraPalette.LIGHT_SURFACE_VARIANT) // surfaceContainer
val LightOnSurfaceVariant = Color(VarotraPalette.LIGHT_ON_SURFACE_VARIANT)

val LightOutline = Color(VarotraPalette.LIGHT_OUTLINE)
val LightOutlineVariant = Color(VarotraPalette.LIGHT_OUTLINE_VARIANT)

val LightError = Color(VarotraPalette.LIGHT_ERROR)
val LightOnError = Color(VarotraPalette.LIGHT_ON_ERROR)
val LightErrorContainer = Color(VarotraPalette.LIGHT_ERROR_CONTAINER)
val LightOnErrorContainer = Color(VarotraPalette.LIGHT_ON_ERROR_CONTAINER)

// Dark Theme Colors
val DarkPrimary = Color(VarotraPalette.DARK_PRIMARY)
val DarkOnPrimary = Color(VarotraPalette.DARK_ON_PRIMARY)
val DarkPrimaryContainer = Color(VarotraPalette.DARK_PRIMARY_CONTAINER)
val DarkOnPrimaryContainer = Color(VarotraPalette.DARK_ON_PRIMARY_CONTAINER)

val DarkSecondary = Color(VarotraPalette.DARK_SECONDARY)
val DarkOnSecondary = Color(VarotraPalette.DARK_ON_SECONDARY)
val DarkSecondaryContainer = Color(VarotraPalette.DARK_SECONDARY_CONTAINER)
val DarkOnSecondaryContainer = Color(VarotraPalette.DARK_ON_SECONDARY_CONTAINER)

val DarkBackground = Color(VarotraPalette.DARK_BACKGROUND)
val DarkOnBackground = Color(VarotraPalette.DARK_ON_BACKGROUND)
val DarkSurface = Color(VarotraPalette.DARK_SURFACE)
val DarkOnSurface = Color(VarotraPalette.DARK_ON_SURFACE)
val DarkSurfaceVariant = Color(VarotraPalette.DARK_SURFACE_VARIANT) // surfaceContainer
val DarkOnSurfaceVariant = Color(VarotraPalette.DARK_ON_SURFACE_VARIANT)

val DarkOutline = Color(VarotraPalette.DARK_OUTLINE)
val DarkOutlineVariant = Color(VarotraPalette.DARK_OUTLINE_VARIANT)

val DarkError = Color(VarotraPalette.DARK_ERROR)
val DarkOnError = Color(VarotraPalette.DARK_ON_ERROR)
val DarkErrorContainer = Color(VarotraPalette.DARK_ERROR_CONTAINER)
val DarkOnErrorContainer = Color(VarotraPalette.DARK_ON_ERROR_CONTAINER)
