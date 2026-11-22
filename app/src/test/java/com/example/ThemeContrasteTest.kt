package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Remplacer `primary` sans remplacer `onPrimary` cassait le couple en thème sombre.
 *
 * Le schéma sombre a été conçu pour une primaire CLAIRE (menthe `0xFFC1ECD4`), donc son `onPrimary`
 * est du quasi-noir (`0xFF002114`). Or les quatre accents proposés au gérant sont tous SOMBRES :
 * laisser `onPrimary` en place donnait du quasi-noir sur un accent sombre — environ 1,1:1, donc un
 * libellé et une icône qui disparaissent.
 *
 * Le défaut était invisible depuis les sites d'appel : c'est `contentColorFor(primary)` qui va
 * chercher cette valeur, donc un bouton `containerColor = themeColor` **sans** `contentColor` la
 * recevait sans que rien ne le dise. D'où un test sur le mécanisme lui-même plutôt que sur un
 * bouton en particulier : une régression ici ne se verrait nulle part dans le code, seulement à
 * l'écran, et seulement en thème sombre.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeContrasteTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Les quatre valeurs que `InventoryViewModel.themeColor` peut prendre. Toutes sombres. */
    private val accents = listOf(
        "emerald/défaut" to Color(0xFF012D1D),
        "sunset" to Color(0xFFE65100),
        "indigo" to Color(0xFF1E3A8A),
        "rose" to Color(0xFF881337)
    )

    private fun verifier(sombre: Boolean) {
        val onPrimary = mutableMapOf<String, Color>()
        val derive = mutableMapOf<String, Color>()

        composeRule.setContent {
            accents.forEach { (nom, accent) ->
                MyApplicationTheme(darkTheme = sombre, primaryColorOverride = accent) {
                    onPrimary[nom] = MaterialTheme.colorScheme.onPrimary
                    derive[nom] = contentColorFor(MaterialTheme.colorScheme.primary)
                }
            }
        }

        accents.forEach { (nom, _) ->
            assertEquals(
                "onPrimary doit être blanc sur l'accent $nom (thème ${if (sombre) "sombre" else "clair"})",
                Color.White,
                onPrimary[nom]
            )
            // C'est CE chemin qui était cassé : un bouton sans `contentColor` explicite passe par lui.
            assertEquals(
                "contentColorFor(primary) doit rendre du blanc sur l'accent $nom",
                Color.White,
                derive[nom]
            )
        }
    }

    @Test
    fun `un accent sombre recoit du blanc en theme sombre`() = verifier(sombre = true)

    @Test
    fun `le theme clair reste inchange`() = verifier(sombre = false)

    @Test
    fun `sans accent choisi le theme garde ses propres couleurs`() {
        // Garde-fou : la correction ne doit toucher QUE la branche de surcharge. Sans surcharge, le
        // schéma sombre garde sa primaire claire et son `onPrimary` sombre, qui vont bien ensemble.
        var primary = Color.Unspecified
        var onPrimary = Color.Unspecified
        composeRule.setContent {
            MyApplicationTheme(darkTheme = true, primaryColorOverride = null) {
                primary = MaterialTheme.colorScheme.primary
                onPrimary = MaterialTheme.colorScheme.onPrimary
            }
        }
        assertEquals(Color(0xFFC1ECD4), primary)
        assertEquals(Color(0xFF002114), onPrimary)
    }
}
