package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.data.repository.InventoryRepository
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression test for the exponential duplication bug: restoring the local safety backup used to
 * re-insert every sale/debt/restock/cash movement as "new" because the dedup check read
 * allSales.value (a StateFlow with SharingStarted.WhileSubscribed(5000)) instead of the actual
 * database — that StateFlow only starts collecting once a UI subscriber shows up, so it could
 * still hold its initial empty list at the point syncFullDatabaseSync() runs on app startup.
 * Since this restore runs on every launch once a backup exists, and re-saves the (now bigger)
 * state right after, the bug doubled every historical record on every single app restart.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreDuplicationTest {

    // Voir le commentaire équivalent dans DeletionTombstoneTest : le ViewModel construit ici lance
    // aussi des écritures de fond sur viewModelScope (Dispatchers.Main.immediate par défaut, ex.
    // son init block qui peut semer des produits d'exemple) — un dispatcher de test sans Looper réel
    // les rend déterministes plutôt que dépendantes d'un pompage explicite de la file Robolectric.
    /**
     * Injecte une **vente témoin**, absente de la base, dans le JSON à restaurer.
     *
     * Les deux boucles d'attente d'origine attendaient que le nombre de ventes vaille 1 — soit
     * exactement ce que l'assertion vérifie ensuite, et aussi l'état de DÉPART. Comme
     * `syncFullDatabaseSync` est lancée sans être attendue, elles sortaient à la première lecture,
     * avant la moindre écriture : le test passait **sans avoir exercé la restauration**, donc sans
     * pouvoir voir revenir la duplication qu'il est censé garder.
     *
     * Le témoin est une VENTE et non un produit, et ce détail est porteur : la fusion traite les
     * produits AVANT les ventes, donc un témoin produit prouverait seulement que la fusion a
     * commencé. Il est ajouté à la FIN du tableau, qui est parcouru dans l'ordre, donc son
     * apparition prouve que toutes les ventes précédentes ont déjà été traitées.
     *
     * Son horodatage est volontairement très ancien et éloigné de celui de la vraie vente : la
     * correspondance des ventes se fait sur l'instant, à la minute près.
     */
    private fun avecVenteTemoin(json: String, horodatage: Long): String {
        val racine = org.json.JSONObject(json)
        // `sales` est un vrai tableau JSON, pas une chaîne (section 85) : le relire avec
        // `optString` puis le réécrire casserait la désérialisation, et le test réussirait alors
        // sans rien exercer du tout.
        val ventes = racine.getJSONArray("sales")
        val article = org.json.JSONObject()
            .put("productId", 999)
            .put("name", "Temoin")
            .put("quantity", 1.0)
            .put("price", 7.0)
        ventes.put(
            org.json.JSONObject()
                .put("timestamp", horodatage)
                .put("totalAmount", 7.0)
                .put("items", org.json.JSONArray().put(article))
        )
        racine.put("sales", ventes)
        return racine.toString()
    }

    private suspend fun attendreVenteTemoin(repository: InventoryRepository, horodatage: Long) {
        repeat(60) {
            if (repository.allSales.first().any { it.timestamp == horodatage }) return
            delay(50)
        }
        throw AssertionError("La restauration n'a jamais abouti : la vente témoin $horodatage est absente")
    }

    @Before
    fun installerDispatcherPrincipalDeTest() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurerDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    @Test
    fun `restoring the same backup twice does not duplicate a sale`() = runBlocking {
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

        val viewModel = InventoryViewModel(repository, context)

        // 1. Record a real sale, exactly like a normal checkout.
        val product = Product(
            id = 20,
            name = "Regression Product",
            price = 1000.0,
            category = "Alimentation",
            stock = 10.0,
            unit = "Pièce",
            barcode = "555111"
        )
        repository.insertProduct(product)

        val sale = Sale(
            timestamp = System.currentTimeMillis(),
            totalAmount = 2000.0,
            items = listOf(SoldItem(productId = 20, name = "Regression Product", quantity = 2.0, price = 1000.0))
        )
        repository.checkoutSale(sale)

        assertEquals("Sanity check before restore", 1, repository.allSales.first().size)

        // 2. Simulate exactly what happens on every app restart when a local safety backup file
        // exists: export the current state, then "restore" that same JSON back in.
        val temoin1 = 1_600_000_000_000L
        val backupJson = avecVenteTemoin(viewModel.getFullDatabaseJsonSync(), temoin1)
        viewModel.syncFullDatabaseSync(backupJson)

        // `syncFullDatabaseSync` est lancée sans être attendue : on attend que la vente témoin
        // arrive, ce qui prouve que la fusion des ventes a tourné, AVANT d'affirmer qu'elle n'a
        // rien dupliqué.
        attendreVenteTemoin(repository, temoin1)

        val sales = repository.allSales.first()
        assertEquals(
            "Restoring the same backup must not duplicate the sale",
            1,
            sales.count { it.timestamp == sale.timestamp }
        )
        assertEquals("Seules la vente d'origine et le témoin doivent exister", 2, sales.size)
        assertEquals(2000.0, sales.first { it.timestamp == sale.timestamp }.totalAmount, 0.0)

        // 3. A second restore pass (e.g. a second app restart) must still be a no-op.
        // Témoin différent : celui de la première passe existe désormais en base, donc il ne
        // prouverait plus rien sur cette seconde restauration. Écarté de plus d'une minute du
        // premier, la correspondance des ventes se faisant sur l'instant.
        val temoin2 = 1_600_000_200_000L
        val secondBackupJson = avecVenteTemoin(viewModel.getFullDatabaseJsonSync(), temoin2)
        viewModel.syncFullDatabaseSync(secondBackupJson)
        attendreVenteTemoin(repository, temoin2)

        val salesAfterSecondRestore = repository.allSales.first()
        assertEquals(
            "A second restore must still not duplicate the sale",
            1,
            salesAfterSecondRestore.count { it.timestamp == sale.timestamp }
        )
        assertEquals(
            "La seconde passe n'ajoute que son propre témoin",
            3,
            salesAfterSecondRestore.size
        )

        db.close()
    }
}
