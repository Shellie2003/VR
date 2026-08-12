package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
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

    // createAndroidComposeRule<ComponentActivity>() plutôt que createComposeRule() nu : héberge le
    // contenu dans une vraie Activity Robolectric (fenêtre/decor view réels, configuration d'écran
    // réaliste) — indispensable pour un écran entier en fillMaxSize() + défilement. createComposeRule()
    // sans Activity a fait échouer assertIsDisplayed() ("The component is not displayed!") même après
    // avoir forcé une taille explicite via un Box englobant, ce qui pointait vers un problème plus
    // profond que la seule taille de fenêtre (probablement l'absence de vraie fenêtre/root réels).
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
    fun `ecran des parametres visible en clair`() = verifieEcranVisible(darkTheme = false)

    @Test
    fun `ecran des parametres visible en sombre`() = verifieEcranVisible(darkTheme = true)

    private fun verifieEcranVisible(darkTheme: Boolean) {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = darkTheme) {
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

        composeTestRule.onNodeWithTag("settings_epicerie_button").assertIsDisplayed()
        // La carte de mise à jour interne (voir UpdateManager) est loin dans l'écran défilant — sur
        // la hauteur d'écran réelle simulée par l'Activity Robolectric, elle peut être hors du
        // viewport initial. performScrollTo() fait défiler jusqu'à la rendre visible avant de
        // vérifier, exactement comme un utilisateur le ferait. Jamais cliquée ici : un vrai clic
        // ferait un appel réseau sortant vers R2 — indésirable et non déterministe dans un test
        // unitaire (même principe déjà suivi pour les boutons Firebase de cet écran).
        composeTestRule.onNodeWithTag("update_check_button").performScrollTo().assertIsDisplayed()
    }
}
