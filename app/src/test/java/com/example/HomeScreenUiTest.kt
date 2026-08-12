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
import com.example.ui.screens.HomeScreen
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
 * Vérifie que HomeScreen (section 60/61 de AGENTS.md) reste correct après le passage aux tokens
 * Material3 — s'affiche en clair ET en sombre. C'était le bug de mode sombre le plus visible du
 * rattrapage : `containerColor = Color.White` codé en dur sur CHAQUE carte de la grille produits,
 * l'écran que le gérant regarde en continu à la caisse, sans même la tentative (fautive) de gestion
 * du mode sombre qu'avaient SalesHistoryScreen/SettingsScreen au départ.
 *
 * Vérifie `home_settings_button`/`search_input` (rendus par `HomeHeaderContent`, donc présents dans
 * l'état vide comme dans l'état avec produits) plutôt que la grille elle-même : une base fraîchement
 * construite ici n'a aucun produit, et forcer une insertion nécessiterait un appel `suspend` en
 * dehors de tout contexte de coroutine, inutile pour ce que ce test cherche à vérifier.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeScreenUiTest {

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
            ligneVenteLotDao = db.ligneVenteLotDao(),
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
    fun `grille produits visible en clair`() = verifieGrilleVisible(darkTheme = false)

    @Test
    fun `grille produits visible en sombre`() = verifieGrilleVisible(darkTheme = true)

    private fun verifieGrilleVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                HomeScreen(
                    viewModel = buildViewModel(),
                    onNavigateToAddProduct = {},
                    onNavigateToList = {},
                    onNavigateToSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("home_settings_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_input").assertIsDisplayed()
    }
}
