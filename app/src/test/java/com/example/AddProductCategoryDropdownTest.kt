package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.repository.InventoryRepository
import com.example.ui.screens.AddProductScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
 * Demande utilisateur : « dans le formulaire d'ajout, si je clique la catégorie "Hafa" puis saisis
 * la nouvelle catégorie, elle doit être incluse dans la liste déroulante pour les autres articles ».
 *
 * La catégorie personnalisée était bien ENREGISTRÉE sur le produit (`finalCategory` reprend la
 * saisie quand « Hafa » est sélectionné), mais la liste déroulante n'affichait que la liste figée
 * `standardCategories` : la catégorie créée restait donc invisible pour les articles suivants, et
 * il fallait la ressaisir à l'identique à chaque fois — avec le risque de créer des doublons à la
 * moindre faute de frappe.
 *
 * Ce test part d'un produit déjà enregistré avec une catégorie personnalisée, ouvre la liste
 * déroulante d'un NOUVEAU produit, et exige que cette catégorie y figure.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class AddProductCategoryDropdownTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun installerDispatcherPrincipalDeTest() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurerDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    private fun buildRepository(db: AppDatabase) = InventoryRepository(
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

    @Test
    fun `une categorie personnalisee deja enregistree est proposee dans la liste deroulante`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = buildRepository(db)

        val categoriePerso = "Pieces detachees moto"
        // `insertProduct` est une fonction `suspend` : elle ne peut pas être appelée telle quelle
        // depuis un test non-suspend, d'où le `runBlocking` (piège déjà rencontré sur
        // HomeScreenUiTest, voir section 61 de AGENTS.md).
        runBlocking {
            repository.insertProduct(
                Product(
                    name = "Chaine moto",
                    price = 15000.0,
                    category = categoriePerso,
                    stock = 3.0
                )
            )
        }

        val viewModel = InventoryViewModel(repository, context)

        composeTestRule.setContent {
            MyApplicationTheme {
                AddProductScreen(
                    viewModel = viewModel,
                    editingProduct = null,
                    onSaveProduct = { _, _ -> },
                    onCancel = {}
                )
            }
        }

        // `categories` est un StateFlow en SharingStarted.WhileSubscribed : c'est le
        // `collectAsState()` de l'écran qui déclenche la requête Room. On attend qu'elle ait
        // convergé plutôt que de supposer que la première composition la contient déjà.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.categories.value.contains(categoriePerso)
        }

        composeTestRule.onNodeWithTag("product_category_select").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(categoriePerso).fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText(categoriePerso).assertIsDisplayed()

        db.close()
    }
}
