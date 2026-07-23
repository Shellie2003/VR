package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
import com.example.data.repository.InventoryRepository
import com.example.ui.viewmodel.InventoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression test for the "deleted data reappears after a restore/sync" bug reported by the user:
 * syncFullDatabaseSync() is purely additive by design (so a restore never loses data), which meant
 * a product deleted locally could be silently resurrected if an older backup or another device's
 * sync payload — one generated before the deletion — was applied afterwards. Deletion tombstones
 * (DeletedRecord) fix this by recording every deletion by a stable natural key and refusing to
 * re-insert a record whose key is tombstoned.
 *
 * Assertions below check for a specific product by barcode rather than raw list sizes, since a
 * freshly constructed InventoryViewModel may asynchronously seed sample/template products in the
 * background (see its init block) — unrelated noise that a size-based assertion would be sensitive
 * to but a barcode lookup is not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DeletionTombstoneTest {

    private fun buildRepository(db: AppDatabase): InventoryRepository = InventoryRepository(
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
        deletedRecordDao = db.deletedRecordDao()
    )

    @Test
    fun `restoring a stale backup does not resurrect a deleted product`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = buildRepository(db)
        val viewModel = InventoryViewModel(repository, context)

        // 1. Create a product and capture a backup while it still exists (the "stale" backup).
        val product = Product(
            id = 42,
            name = "Tombstone Product",
            price = 500.0,
            category = "Alimentation",
            stock = 5.0,
            unit = "Pièce",
            barcode = "999888"
        )
        repository.insertProduct(product)
        val staleBackupJson = viewModel.getFullDatabaseJsonSync()
        assertTrue(
            "Sanity check: the stale backup must actually contain the product",
            staleBackupJson.contains("Tombstone Product")
        )

        // 2. Delete it, exactly like a gérant would from the UI.
        val stored = repository.getProductByBarcode("999888")
        assertNotNull("Sanity check before deletion", stored)
        viewModel.deleteProduct(stored!!)

        repeat(30) {
            if (repository.getProductByBarcode("999888") != null) delay(100)
        }
        assertNull("Product must be gone right after deletion", repository.getProductByBarcode("999888"))

        // 3. Restore the stale backup (simulates an old local safety backup file surviving a
        // migration, a manual Firebase restore, or a peer device syncing in with outdated data).
        viewModel.syncFullDatabaseSync(staleBackupJson)

        // syncFullDatabaseSync is fire-and-forget; give it time to (not) resurrect the product.
        repeat(15) { delay(100) }

        assertNull(
            "The deleted product must not be resurrected by restoring a backup that predates the deletion",
            repository.getProductByBarcode("999888")
        )

        db.close()
    }

    @Test
    fun `an incoming tombstone deletes the matching local record`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Device A: creates and deletes a product, producing a tombstone-carrying backup.
        val dbA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repositoryA = buildRepository(dbA)
        val viewModelA = InventoryViewModel(repositoryA, context)

        val product = Product(
            id = 7,
            name = "Cross Device Product",
            price = 750.0,
            category = "Alimentation",
            stock = 3.0,
            unit = "Pièce",
            barcode = "111222"
        )
        repositoryA.insertProduct(product)
        viewModelA.deleteProduct(repositoryA.getProductByBarcode("111222")!!)

        repeat(30) {
            if (repositoryA.getProductByBarcode("111222") != null) delay(100)
        }
        val tombstoneJson = viewModelA.getFullDatabaseJsonSync()
        assertTrue(
            "Sanity check: the backup after deletion must carry the tombstone",
            tombstoneJson.contains("\"entityType\":\"product\"")
        )

        // Device B: still has the product (never saw the deletion) — receiving Device A's payload
        // (e.g. via P2P sync or a shared Firebase backup) must delete it locally too.
        // Device A's own deletion above already wrote a local safety backup file via the shared
        // Robolectric application context; clear it first so Device B's ViewModel init doesn't
        // race an automatic restore against the manual setup below (both use the same context,
        // since Robolectric only provides one Application instance per test).
        java.io.File(context.filesDir, "database_safety_backup.json").delete()
        val dbB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repositoryB = buildRepository(dbB)
        val viewModelB = InventoryViewModel(repositoryB, context)
        repositoryB.insertProduct(product.copy(id = 0))

        assertNotNull("Sanity check: Device B has the product before syncing", repositoryB.getProductByBarcode("111222"))

        viewModelB.syncFullDatabaseSync(tombstoneJson)

        repeat(30) {
            if (repositoryB.getProductByBarcode("111222") != null) delay(100)
        }

        assertNull(
            "Device B must delete its own copy once it learns the product was deleted elsewhere",
            repositoryB.getProductByBarcode("111222")
        )

        dbA.close()
        dbB.close()
    }
}
