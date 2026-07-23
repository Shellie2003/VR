package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.InventoryRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Varotra", appName)
  }

  @Test
  fun `test checkoutSale decreases stock`() = runBlocking {
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

    // 1. Insert product
    val p = Product(
        id = 10,
        name = "Test Product",
        price = 1000.0,
        category = "Alimentation",
        stock = 10.0,
        unit = "Pièce",
        barcode = "123456"
    )
    repository.insertProduct(p)

    // Verify it was synced to Produit
    val pr = repository.produitDao.getProduitById(10L)
    assertNotNull("Produit should be synced", pr)
    assertEquals(10.0, pr!!.quantiteStock, 0.0)

    // 2. Perform checkout
    val saleItem = SoldItem(
        productId = 10,
        name = "Test Product",
        quantity = 2.0,
        price = 1000.0
    )
    val sale = Sale(
        id = 0,
        timestamp = System.currentTimeMillis(),
        totalAmount = 2000.0,
        items = listOf(saleItem)
    )

    repository.checkoutSale(sale)

    // 3. Verify stock decreased
    val updatedProduct = repository.getProductById(10)
    assertNotNull(updatedProduct)
    assertEquals(8.0, updatedProduct!!.stock, 0.0)

    val updatedProduit = repository.produitDao.getProduitById(10L)
    assertNotNull(updatedProduit)
    assertEquals(8.0, updatedProduit!!.quantiteStock, 0.0)

    db.close()
  }

  @Test
  fun `test flow and suspend DAO methods execute correctly`() = runBlocking {
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

    // Insert using suspend function
    val p = Product(
        id = 15,
        name = "Flow Product",
        price = 1500.0,
        category = "Drinks",
        stock = 25.0,
        unit = "Litre",
        barcode = "789101"
    )
    repository.insertProduct(p)

    // Verify getting single product using suspend function
    val retrieved = repository.getProductById(15)
    assertNotNull(retrieved)
    assertEquals("Flow Product", retrieved!!.name)

    // Verify collecting from flow
    val products = repository.allProducts.first()
    assertEquals(1, products.size)
    assertEquals("Flow Product", products[0].name)

    db.close()
  }
}
