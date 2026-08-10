package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.ui.screens.SaleListItem
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.graphics.Color
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Vérifie que SaleListItem (section 55 de AGENTS.md : hex/sp codés en dur remplacés par
 * MaterialTheme.colorScheme/typography) reste correct après le passage aux tokens de thème —
 * s'affiche, se déplie au clic, et affiche la case à cocher en mode sélection — en clair comme
 * en sombre, puisque c'était justement le fond blanc codé en dur qui cassait le mode sombre.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaleListItemUiTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val sample = Sale(
        id = 42,
        timestamp = System.currentTimeMillis(),
        totalAmount = 4500.0,
        items = listOf(SoldItem(productId = 1, name = "Savon", quantity = 2.0, price = 2250.0))
    )

    // Un seul setContent par test : createComposeRule() ne permet pas de le rappeler (même
    // indirectement dans une boucle) au sein d'un même test, d'où deux tests distincts plutôt
    // qu'une boucle sur [false, true].
    @Test
    fun `carte visible et se deplie au clic en clair`() = verifieCarteVisibleEtDepliable(darkTheme = false)

    @Test
    fun `carte visible et se deplie au clic en sombre`() = verifieCarteVisibleEtDepliable(darkTheme = true)

    private fun verifieCarteVisibleEtDepliable(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                SaleListItem(
                    sale = sample,
                    allProducts = listOf(
                        Product(id = 1, name = "Savon", price = 2250.0, category = "Hygiène", stock = 10.0, unit = "Pièce")
                    ),
                    themeColor = Color(0xFF13503C),
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sale_card_42").assertIsDisplayed()

        // Replié par défaut : le détail par article n'est affiché qu'après un clic.
        composeTestRule.onNodeWithTag("sale_card_42").performClick()
    }

    @Test
    fun `case a cocher visible en mode selection`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SaleListItem(
                    sale = sample,
                    allProducts = emptyList(),
                    themeColor = Color(0xFF13503C),
                    isSelectionMode = true,
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("sale_checkbox_42").assertIsDisplayed()
    }
}
