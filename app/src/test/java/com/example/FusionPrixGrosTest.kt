package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Product
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La fusion d'un produit DÉJÀ présent (`existing.copy(...)` dans `syncFullDatabaseSync`) nommait 21
 * champs mais pas `wholesalePrice` : un tarif de gros saisi sur un poste n'arrivait donc jamais sur
 * les autres, qui continuaient de facturer au prix de détail en mode Grossiste. Même famille que les
 * sections 32 et 79 — deux prix pour le même carton, sans qu'aucun écran ne le signale.
 *
 * Le second test est celui qui garde le correctif de causer la perte qu'il répare, et le piège est
 * réel : la sérialisation écrit `wholesalePrice ?: 0.0`, donc un produit sans tarif de gros arrive
 * en `0.0` et non en `null`. Un `?:` naïf aurait pris ce zéro pour une valeur et écrasé le tarif
 * local — à CHAQUE démarrage, puisque la sauvegarde locale est rejouée à chaque lancement
 * (section 45). Et un tarif de gros à zéro n'est pas visible comme une erreur : `PriceUtil`
 * exigeant `> 0.0`, le mode Grossiste retombe simplement sur le prix de détail.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FusionPrixGrosTest {

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

    private val codeBarre = "6001234567890"

    private fun carton(prixGros: Double?) = Product(
        name = "Carton de savon",
        price = 1200.0,
        category = "Droguerie",
        stock = 40.0,
        unit = "Carton",
        barcode = codeBarre,
        wholesalePrice = prixGros
    )

    /**
     * Injecte un produit **témoin**, absent de la base cible, dans le JSON à restaurer.
     *
     * Sans lui, ces tests ne prouvaient rien : le produit dont on veut vérifier le tarif de gros
     * existe DÉJÀ sur l'appareil cible, donc l'attendre le rend instantanément — avec sa valeur
     * d'AVANT la fusion, `syncFullDatabaseSync` étant lancée sans être attendue. C'est exactement
     * la boucle d'attente vide corrigée dans `BackupRestoreDuplicationTest` (section 111), et je
     * l'ai commise ici dans le même lot. Le témoin donne un signe POSITIF : son apparition prouve
     * que la fusion est allée jusqu'au bout.
     */
    private fun avecTemoin(json: String, codeTemoin: String): String {
        val racine = org.json.JSONObject(json)
        // `products` est un vrai tableau JSON, pas une chaîne (section 85).
        val produits = racine.getJSONArray("products")
        produits.put(
            org.json.JSONObject()
                .put("name", "Temoin $codeTemoin")
                .put("price", 1.0)
                .put("category", "Alimentation")
                .put("stock", 1.0)
                .put("barcode", codeTemoin)
        )
        racine.put("products", produits)
        return racine.toString()
    }

    private suspend fun attendreFusion(depot: InventoryRepository, codeTemoin: String) {
        repeat(60) {
            if (depot.getProductByBarcode(codeTemoin) != null) return
            delay(50)
        }
        throw AssertionError("La fusion n'a jamais abouti : le produit témoin $codeTemoin est absent")
    }

    @Test
    fun `un tarif de gros saisi sur un poste arrive sur un poste ou le produit existe deja`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val dbSource = nouvelleBase()
        val depotSource = depot(dbSource)
        depotSource.insertProduct(carton(prixGros = 9000.0))
        val sauvegarde = avecTemoin(
            InventoryViewModel(depotSource, context).getFullDatabaseJsonSync(),
            "TEMOIN-GROS-1"
        )

        val dbCible = nouvelleBase()
        val depotCible = depot(dbCible)
        depotCible.insertProduct(carton(prixGros = null))
        val vmCible = InventoryViewModel(depotCible, context)

        vmCible.syncFullDatabaseSync(sauvegarde)
        attendreFusion(depotCible, "TEMOIN-GROS-1")

        val fusionne = depotCible.getProductByBarcode(codeBarre)
        assertNotNull(fusionne)
        assertEquals(
            "Le tarif de gros doit traverser la fusion, sinon les deux postes facturent différemment",
            9000.0,
            fusionne!!.wholesalePrice ?: 0.0,
            0.01
        )

        dbSource.close()
        dbCible.close()
    }

    @Test
    fun `une sauvegarde d'un poste sans tarif de gros n'efface pas le tarif local`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val dbSource = nouvelleBase()
        val depotSource = depot(dbSource)
        depotSource.insertProduct(carton(prixGros = null))
        val brute = InventoryViewModel(depotSource, context).getFullDatabaseJsonSync()
        assertTrue(
            "La sauvegarde doit bien écrire un zéro, sinon le test ne reproduit pas le piège",
            brute.contains("\"wholesalePrice\":0")
        )
        val sauvegarde = avecTemoin(brute, "TEMOIN-GROS-2")

        val dbCible = nouvelleBase()
        val depotCible = depot(dbCible)
        depotCible.insertProduct(carton(prixGros = 9000.0))
        val vmCible = InventoryViewModel(depotCible, context)

        vmCible.syncFullDatabaseSync(sauvegarde)
        // Ici le témoin est INDISPENSABLE : la valeur attendue (9000) est aussi la valeur de
        // départ, donc toute attente portant sur elle sortirait avant la fusion et le test
        // passerait sans jamais l'exercer.
        attendreFusion(depotCible, "TEMOIN-GROS-2")

        val fusionne = depotCible.getProductByBarcode(codeBarre)
        assertNotNull(fusionne)
        assertEquals(
            "Un zéro venu d'un poste sans tarif de gros ne doit jamais écraser le tarif local",
            9000.0,
            fusionne!!.wholesalePrice ?: 0.0,
            0.01
        )

        dbSource.close()
        dbCible.close()
    }
}
