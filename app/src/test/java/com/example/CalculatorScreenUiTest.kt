package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepository
import com.example.ui.screens.CalculatorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Vérifie que CalculatorScreen — l'écran de caisse, le plus utilisé de l'app — reste correct après
 * le passage aux tokens Material3, en clair ET en sombre.
 *
 * Deux bugs de mode sombre y étaient corrigés : `containerColor = Color.White` codé en dur sur
 * chaque carte produit de la caisse (même bug que HomeScreen), et surtout `Color.White` utilisé
 * comme couleur de contenu sur la carte de facturation remplie en `colorScheme.primary` — en mode
 * sombre `primary` est une teinte claire, donc du blanc sur clair devenait illisible.
 *
 * Les assertions portent sur `calculator_product_search` et `calculator_checkout_button`, tous deux
 * rendus dans la disposition téléphone (le chemin par défaut) quel que soit l'état du panier.
 *
 * `qualifiers` fixe un gabarit de téléphone réaliste et HAUT (411x891dp, format Pixel), pour deux
 * raisons : la disposition de caisse est une `Column` NON défilante dont le contenu à hauteur fixe
 * (en-tête + sélecteur de produits 231dp + carte de facturation ~186dp) dépasse le petit écran par
 * défaut de Robolectric — le bas est alors rogné et `assertIsDisplayed()` échoue, sans que
 * `performScrollTo()` puisse aider faute de conteneur défilant (contrairement à `SettingsScreen`/
 * `CommissionScreen`, section 61) ; et la largeur doit rester SOUS 600dp pour tester bien la
 * disposition téléphone, `CalculatorScreen` basculant sur une disposition tablette à deux panneaux
 * au-delà (`screenWidthDp >= 600`), laquelle ne porte pas ces `testTag`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class CalculatorScreenUiTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun installerDispatcherPrincipalDeTest() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurerDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(): InventoryViewModel {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = InventoryRepository(
            database = db,
            productDao = db.productDao(),
            saleDao = db.saleDao(),
            debtDao = db.debtDao(),
            produitDao = db.produitDao(),
            uniteProduitDao = db.uniteProduitDao(),
            reglePrixDao = db.reglePrixDao(),
            fournisseurDao = db.fournisseurDao(),
            mouvementStockDao = db.mouvementStockDao(),
            lotProduitDao = db.lotProduitDao(),
            venteDao = db.venteDao(),
            lignesVenteDao = db.lignesVenteDao(),
            restockDao = db.restockDao(),
            mouvementCaisseDao = db.mouvementCaisseDao(),
            caisseSessionDao = db.caisseSessionDao(),
            vendeurDao = db.vendeurDao(),
            retourDao = db.retourDao(),
            deletedRecordDao = db.deletedRecordDao(),
            auditLogDao = db.auditLogDao(),
            etagereDao = db.etagereDao()
        )
        return InventoryViewModel(repository, context)
    }

    @Test
    fun `ecran de caisse visible en clair`() = verifieCaisseVisible(darkTheme = false)

    @Test
    fun `ecran de caisse visible en sombre`() = verifieCaisseVisible(darkTheme = true)

    private fun verifieCaisseVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                CalculatorScreen(
                    viewModel = buildViewModel(),
                    onNavigateToHome = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("calculator_product_search").assertIsDisplayed()
        composeTestRule.onNodeWithTag("calculator_checkout_button").assertIsDisplayed()
    }
}
