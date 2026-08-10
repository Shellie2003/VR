package com.example

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepository
import com.example.ui.screens.SettingsScreen
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
 * Vérifie que SettingsScreen (section 57 de AGENTS.md : hex codés en dur remplacés par
 * MaterialTheme.colorScheme, voir aussi la nouvelle carte "Mise à jour") reste correct après le
 * passage aux tokens de thème — s'affiche en clair ET en sombre, puisque c'était justement l'ancien
 * `isDark == background comparé à un hex précis` qui pouvait casser silencieusement le mode sombre
 * dès que la palette changeait.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsScreenUiTest {

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
    fun `ecran des parametres visible en clair`() = verifieEcranVisible(darkTheme = false)

    @Test
    fun `ecran des parametres visible en sombre`() = verifieEcranVisible(darkTheme = true)

    private fun verifieEcranVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
                // Taille explicite : cet écran entier est en fillMaxSize() + défilement, et
                // createComposeRule() sans Activity hôte ne garantit pas de dimensions de fenêtre
                // réalistes sous Robolectric — sans ça, assertIsDisplayed() peut échouer sur un
                // écran pourtant correctement composé (bornes racine nulles ou non déterministes).
                // Hauteur généreuse pour que tout le contenu défilant tienne sans geste de défilement.
                Box(modifier = Modifier.size(400.dp, 2400.dp)) {
                    SettingsScreen(
                        viewModel = buildViewModel(),
                        onNavigateToHistory = {},
                        onNavigateToCommission = {},
                        onNavigateToBarcodes = {},
                        onNavigateToHome = {},
                        onNavigateToSync = {},
                        onNavigateToCaisseMouvements = {},
                        onNavigateToDashboard = {},
                        onNavigateToPeremption = {},
                        onNavigateToEpicerie = {},
                        onNavigateToSecurite = {},
                        onNavigateToEtagere = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("settings_epicerie_button").assertIsDisplayed()
        // La carte de mise à jour interne (voir UpdateManager) : présente mais jamais cliquée ici,
        // un vrai clic ferait un appel réseau sortant vers R2 — indésirable et non déterministe
        // dans un test unitaire (même principe déjà suivi pour les boutons Firebase de cet écran).
        composeTestRule.onNodeWithTag("update_check_button").assertIsDisplayed()
    }
}
