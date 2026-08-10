package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepository
import com.example.ui.screens.CommissionScreen
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
 * Vérifie que CommissionScreen (section 57 de AGENTS.md : hex codés en dur remplacés par
 * MaterialTheme.colorScheme) reste correct après le passage aux tokens de thème — s'affiche en
 * clair ET en sombre, puisque c'était justement le fond racine `Color.White` codé en dur (comme
 * SalesHistoryScreen et CommissionScreen n'avaient au départ aucune gestion du mode sombre du tout,
 * contrairement à SettingsScreen/SalesHistoryScreen qui avaient au moins un `isDark` défaillant) qui
 * cassait entièrement le mode sombre sur cet écran.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CommissionScreenUiTest {

    @get:Rule val composeTestRule = createComposeRule()

    // Voir DeletionTombstoneTest : InventoryViewModel écrit sur viewModelScope sans dispatcher
    // explicite (Dispatchers.Main.immediate), qui reste bloqué sous le Looper Robolectric en pause
    // sans ce dispatcher de test.
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
    fun `ecran commission visible en clair`() = verifieEcranVisible(darkTheme = false)

    @Test
    fun `ecran commission visible en sombre`() = verifieEcranVisible(darkTheme = true)

    private fun verifieEcranVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                CommissionScreen(
                    viewModel = buildViewModel(),
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("validate_commission_restock_button").assertIsDisplayed()
    }
}
