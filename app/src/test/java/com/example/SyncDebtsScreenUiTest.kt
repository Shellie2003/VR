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
import com.example.ui.screens.DebtsScreen
import com.example.ui.screens.SyncScreen
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
 * Vérifie `SyncScreen` et `DebtsScreen` (section 65 de AGENTS.md) en clair ET en sombre après le
 * passage aux tokens Material3.
 *
 * `SyncScreen` portait encore la comparaison `isDark` défectueuse déjà corrigée sur
 * `SalesHistoryScreen`/`SettingsScreen` (`colorScheme.background == Color(0xFF002114)`) : dès qu'une
 * palette sombre change de fond, la comparaison devient fausse et TOUT l'écran repasse en couleurs
 * claires sur un fond sombre. `DebtsScreen`, lui, mélangeait `Color(0xFFD32F2F)` et
 * `colorScheme.error` pour exactement la même sémantique (dette en retard / impayée).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SyncDebtsScreenUiTest {

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
    fun `ecran de synchronisation visible en clair`() = verifieSyncVisible(darkTheme = false)

    @Test
    fun `ecran de synchronisation visible en sombre`() = verifieSyncVisible(darkTheme = true)

    @Test
    fun `ecran des dettes visible en clair`() = verifieDettesVisible(darkTheme = false)

    @Test
    fun `ecran des dettes visible en sombre`() = verifieDettesVisible(darkTheme = true)

    private fun verifieSyncVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                SyncScreen(viewModel = buildViewModel(), onNavigateBack = {})
            }
        }

        composeTestRule.onNodeWithTag("sync_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sync_status_card").assertIsDisplayed()
    }

    private fun verifieDettesVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                DebtsScreen(viewModel = buildViewModel())
            }
        }

        composeTestRule.onNodeWithTag("debt_search_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_debt_button").assertIsDisplayed()
    }
}
