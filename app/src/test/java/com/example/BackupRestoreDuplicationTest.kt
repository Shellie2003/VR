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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
            venteDao = db.venteDao(),
            lignesVenteDao = db.lignesVenteDao(),
            restockDao = db.restockDao(),
            mouvementCaisseDao = db.mouvementCaisseDao(),
            caisseSessionDao = db.caisseSessionDao(),
            vendeurDao = db.vendeurDao()
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
        val backupJson = viewModel.getFullDatabaseJsonSync()
        viewModel.syncFullDatabaseSync(backupJson)

        // syncFullDatabaseSync is fire-and-forget (launched in viewModelScope on Dispatchers.IO),
        // so poll briefly for it to finish instead of asserting immediately.
        var sales = repository.allSales.first()
        repeat(30) {
            if (sales.size != 1) {
                delay(100)
                sales = repository.allSales.first()
            }
        }

        assertEquals("Restoring the same backup must not duplicate the sale", 1, sales.size)
        assertEquals(2000.0, sales.first().totalAmount, 0.0)

        // 3. A second restore pass (e.g. a second app restart) must still be a no-op.
        val secondBackupJson = viewModel.getFullDatabaseJsonSync()
        viewModel.syncFullDatabaseSync(secondBackupJson)
        var salesAfterSecondRestore = repository.allSales.first()
        repeat(30) {
            if (salesAfterSecondRestore.size != 1) {
                delay(100)
                salesAfterSecondRestore = repository.allSales.first()
            }
        }
        assertEquals("A second restore must still not duplicate the sale", 1, salesAfterSecondRestore.size)

        db.close()
    }
}
