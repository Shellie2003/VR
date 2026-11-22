package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.partage.theme.VarotraTheme
import com.example.util.VarotraPalette
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Même défaut que côté Android, et il frappait plus fort ici : la fenêtre de bureau appelait
 * `VarotraTheme(sombre = sombre)` **sans accent**, donc `colorScheme.primary` valait la primaire du
 * schéma sombre — une menthe TRÈS CLAIRE (`0xFFC1ECD4`). Or les écrans partagés reçoivent
 * `colorScheme.primary` comme couleur de thème et y posent du blanc, exactement comme côté Android
 * où cette couleur est toujours un accent sombre. Le bouton « Valider » de la caisse était donc du
 * blanc sur menthe claire, soit environ 1,3:1.
 *
 * Deux moitiés au correctif : le point d'entrée passe maintenant l'accent de la marque, et le thème
 * remplace `onPrimary` en même temps que `primary`. Ce test verrouille la seconde — la première est
 * un argument d'appel dans la fonction `main`, que rien ici ne peut exécuter.
 */
class ThemeBureauContrasteTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `l'accent de la marque recoit du blanc dans les deux themes`() {
        val resultats = mutableMapOf<String, Pair<Color, Color>>()
        val accent = Color(VarotraPalette.LIGHT_PRIMARY)

        composeRule.setContent {
            listOf(true, false).forEach { sombre ->
                VarotraTheme(sombre = sombre, primaireRemplacee = accent) {
                    resultats[if (sombre) "sombre" else "clair"] =
                        MaterialTheme.colorScheme.onPrimary to contentColorFor(MaterialTheme.colorScheme.primary)
                }
            }
        }

        listOf("sombre", "clair").forEach { theme ->
            val (onPrimary, derive) = resultats.getValue(theme)
            assertEquals("onPrimary doit être blanc en thème $theme", Color.White, onPrimary)
            assertEquals("contentColorFor(primary) doit rendre du blanc en thème $theme", Color.White, derive)
        }
    }

    @Test
    fun `l'accent de la marque est bien une couleur sombre`() {
        // C'est la prémisse de tout le raisonnement : si cet accent devenait clair, poser du blanc
        // dessus redeviendrait illisible et ce correctif se retournerait contre lui-même.
        val accent = Color(VarotraPalette.LIGHT_PRIMARY)
        val luminance = 0.2126f * accent.red + 0.7152f * accent.green + 0.0722f * accent.blue
        org.junit.Assert.assertTrue(
            "L'accent de la marque doit rester sombre (luminance approchée $luminance)",
            luminance < 0.3f
        )
    }
}
