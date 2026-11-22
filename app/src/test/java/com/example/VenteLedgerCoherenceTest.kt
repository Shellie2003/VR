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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `checkoutSale` écrit chaque vente dans DEUX journaux : `Sale` (l'historique affiché) et `Vente`
 * (le modèle relationnel, seul à porter `modePaiement`). `deleteSale` ne supprimait que le premier,
 * et `VenteDao.deleteVente` n'était appelée de nulle part.
 *
 * L'effet se compte en argent, et il accuse quelqu'un : la ligne `Vente` survivante est précisément
 * celle que compte la caisse — `soldeCaisse` (section 23) et `getCashSalesTotal`, qui calcule le
 * montant théorique à la fermeture, somment les `Vente` en ESPECES. Annuler une vente laissait donc
 * le solde théorique créditer de l'argent qui n'est pas dans le tiroir, donc un **manquant** au
 * comptage du soir — que `AnomalyDetector` (section 36) imputait à l'employé de permanence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VenteLedgerCoherenceTest {

    @Before
    fun installerDispatcherPrincipalDeTest() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurerDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

    private fun nouvelleBase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    private fun depot(db: AppDatabase) = InventoryRepository(
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

    private fun venteDeRiz(montant: Double = 3000.0) = Sale(
        timestamp = System.currentTimeMillis(),
        totalAmount = montant,
        items = listOf(SoldItem(productId = 1, name = "Riz", quantity = 2.0, price = montant / 2))
    )

    @Test
    fun `une vente supprimee de l'historique disparait aussi du journal relationnel`() = runBlocking {
        val db = nouvelleBase()
        val depot = depot(db)
        val vente = venteDeRiz()

        depot.checkoutSale(vente, decrementStock = false, modePaiement = "ESPECES")
        assertEquals(
            "La caisse doit d'abord compter la vente, sinon le test ne prouve rien",
            1,
            db.venteDao().getVentesAt(vente.timestamp).size
        )

        depot.deleteSale(vente)

        assertTrue(
            "La ligne du journal relationnel doit partir avec la vente",
            db.venteDao().getVentesAt(vente.timestamp).isEmpty()
        )
        db.close()
    }

    @Test
    fun `le total des ventes en especes ne compte plus une vente annulee`() = runBlocking {
        // C'est le chiffre exact utilisé pour le montant théorique à la fermeture de caisse : s'il
        // reste gonflé, le comptage du soir affiche un manquant qui n'existe pas.
        val db = nouvelleBase()
        val depot = depot(db)
        val gardee = venteDeRiz(montant = 5000.0)
        depot.checkoutSale(gardee, decrementStock = false, modePaiement = "ESPECES")
        delay(5)
        val annulee = venteDeRiz(montant = 3000.0)
        depot.checkoutSale(annulee, decrementStock = false, modePaiement = "ESPECES")

        assertEquals(8000.0, db.venteDao().getCashSalesTotal(0, Long.MAX_VALUE), 0.01)

        depot.deleteSale(annulee)

        assertEquals(
            "Seule la vente conservée doit encore être dans le tiroir",
            5000.0,
            db.venteDao().getCashSalesTotal(0, Long.MAX_VALUE),
            0.01
        )
        db.close()
    }

    @Test
    fun `une vente Mvola supprimee ne touche pas le total especes`() = runBlocking {
        // Garde-fou sur le ciblage : la suppression doit retirer LA bonne ligne, pas « une » ligne.
        val db = nouvelleBase()
        val depot = depot(db)
        val especes = venteDeRiz(montant = 5000.0)
        depot.checkoutSale(especes, decrementStock = false, modePaiement = "ESPECES")
        delay(5)
        val mvola = venteDeRiz(montant = 7000.0)
        depot.checkoutSale(mvola, decrementStock = false, modePaiement = "MVOLA")

        depot.deleteSale(mvola)

        assertEquals(5000.0, db.venteDao().getCashSalesTotal(0, Long.MAX_VALUE), 0.01)
        assertEquals(
            "La vente en espèces ne devait pas être touchée",
            1,
            db.venteDao().getVentesAt(especes.timestamp).size
        )
        db.close()
    }

    @Test
    fun `une vente sans ligne relationnelle se supprime sans erreur`() = runBlocking {
        // Cas d'une vente restaurée depuis une sauvegarde : l'historique la porte, le journal
        // relationnel non. On n'en trouve aucune, et la suppression se limite à l'historique —
        // exactement le comportement d'avant le correctif.
        val db = nouvelleBase()
        val depot = depot(db)
        val restauree = venteDeRiz()
        db.saleDao().insertSale(restauree)

        depot.deleteSale(restauree)

        assertTrue(db.venteDao().getVentesAt(restauree.timestamp).isEmpty())
        db.close()
    }

    @Test
    fun `un echec d'enregistrement de la vente est signale et le panier est conserve`() = runBlocking {
        // Le `catch (e: Throwable) { e.printStackTrace() }` qui enveloppait l'écriture neutralisait
        // `lancerProtege` sur l'écriture la plus importante de l'application : la vente échouait,
        // le panier se vidait, le reçu s'imprimait, le client repartait — et aucune vente
        // n'existait. Seul un journal invisible pour un épicier en gardait la trace.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = nouvelleBase()
        val depot = depot(db)
        val produit = Product(name = "Riz", price = 1500.0, category = "Alimentation", stock = 10.0)
        val produitId = depot.insertProduct(produit)
        val vm = InventoryViewModel(depot, context)

        vm.addToCart(produit.copy(id = produitId), 2.0)
        assertEquals(1, vm.cart.value.size)
        vm.consommerErreurOperation()

        // Injection de l'échec : on SUPPRIME la table des ventes. `db.close()` ne marche pas pour
        // cela — Room rouvre tranquillement une base en mémoire (vide, recréée au schéma), donc
        // l'écriture RÉUSSIT et il n'y a aucune erreur à signaler. Supprimer la table fait échouer
        // `insertSale` de façon déterministe, la transaction est annulée, et c'est bien le chemin
        // d'échec réel qu'on veut éprouver. La cause importe peu : ce qui est vérifié ici, c'est
        // que l'échec devient VISIBLE pour le vendeur.
        db.openHelper.writableDatabase.execSQL("DROP TABLE sales")
        vm.checkoutCart()

        var message = vm.erreurOperation.value
        repeat(30) {
            if (message == null) {
                delay(50)
                message = vm.erreurOperation.value
            }
        }

        assertNotNull("Un échec d'écriture de la vente doit être signalé au vendeur", message)
        assertTrue(
            "Le message doit nommer l'opération, pas rester générique : $message",
            message!!.contains("vente")
        )
        assertFalse(
            "Le panier doit rester intact pour que la vente soit re-tentable",
            vm.cart.value.isEmpty()
        )
        db.close()
    }
}
