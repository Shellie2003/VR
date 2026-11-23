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
 * La barre de navigation basse n'avait **aucune** branche de thème : fond `Color.White` codé en dur,
 * séparateur en dur, et un vert d'onglet actif figé. Deux conséquences — une bande blanche de 68dp
 * restait sous chaque écran d'un corps sombre, et la couleur d'accent choisie par le gérant y était
 * ignorée. Ce fichier (`MainActivity.kt`) n'a jamais fait partie du périmètre du rattrapage UI/UX
 * des sections 57 à 67, qui ne couvrait que `ui/screens/`.
 *
 * Ce test suit le précédent des douze écrans migrés : il vérifie que la barre se compose dans les
 * deux thèmes. Il n'évalue pas le contraste — ce que les tokens `surface`/`onSurfaceVariant`
 * garantissent par construction, étant les couples prévus par Material3 et non une surcharge comme
 * celle corrigée en section 112.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BottomNavBarUiTest {

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

    private fun rendre(sombre: Boolean) {
        val viewModel = buildViewModel()
        composeTestRule.setContent {
            // L'accent est passé en surcharge, comme le fait toujours `MainActivity` : c'est ce qui
            // rend le chemin testé identique à la production (voir section 112).
            MyApplicationTheme(
                darkTheme = sombre,
                primaryColorOverride = androidx.compose.ui.graphics.Color(0xFF012D1D)
            ) {
                BottomNavBarSection(
                    viewModel = viewModel,
                    currentTab = ScreenTab.Fandraisana,
                    onTabSelected = {}
                )
            }
        }
    }

    @Test
    fun `la barre de navigation s'affiche en theme clair`() {
        rendre(sombre = false)

        composeTestRule.onNodeWithTag("nav_tab_fandraisana").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_manampy").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_dettes").assertIsDisplayed()
    }

    @Test
    fun `la barre de navigation s'affiche en theme sombre`() {
        // C'est le cas qui ne s'affichait pas correctement : la bande restait blanche.
        rendre(sombre = true)

        composeTestRule.onNodeWithTag("nav_tab_fandraisana").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_manampy").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_tab_dettes").assertIsDisplayed()
    }
}
