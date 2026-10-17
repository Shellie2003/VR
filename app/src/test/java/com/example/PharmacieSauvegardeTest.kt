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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
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
 * Les champs du mode pharmacie (`dci`, `dosage`, `formeGalenique`, `surOrdonnance`) existaient dans
 * l'entité, dans la migration et dans le formulaire — mais n'étaient portés par **aucune**
 * sauvegarde ni synchronisation. Conséquences réelles, constatées par lecture du code avant
 * correctif : une pharmacie qui restaurait ses données sur un nouveau téléphone perdait toutes ses
 * molécules, et la recherche par principe actif — la raison d'être du champ — cessait de trouver
 * quoi que ce soit, sans aucun message.
 *
 * Le chemin « catalogue » omettait en plus `wholesalePrice`, et là l'effet se compte en argent : un
 * produit arrivant NEUF sur le second poste y était créé sans prix de gros, donc facturé au prix de
 * détail en mode Grossiste.
 *
 * Le dernier test est le plus important des quatre : il garde le correctif de ne pas CAUSER la
 * perte qu'il répare. La sauvegarde locale est rejouée à chaque démarrage, donc un fichier écrit
 * avant cette version (sans ces clés) ne doit pas effacer ce qui est déjà en base.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PharmacieSauvegardeTest {

    // Voir BackupRestoreDuplicationTest : un ViewModel réel lance des écritures sur viewModelScope
    // (Dispatchers.Main.immediate par défaut), que Robolectric ne pompe pas tout seul.
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

    /** Le médicament tel que le gérant d'une pharmacie le saisit réellement. */
    private fun medicament() = Product(
        name = "Doliprane 500 mg",
        price = 4500.0,
        category = "Pharmacie",
        stock = 25.0,
        unit = "Plaquette",
        barcode = "3400930000011",
        wholesalePrice = 4000.0,
        dci = "paracétamol",
        dosage = "500 mg",
        formeGalenique = "Comprimé",
        surOrdonnance = true
    )

    /** Attend qu'un produit identifié par son code-barres arrive en base (écritures asynchrones). */
    private suspend fun attendreProduit(depot: InventoryRepository, codeBarre: String): Product? {
        var trouve = depot.getProductByBarcode(codeBarre)
        repeat(30) {
            if (trouve == null) {
                delay(100)
                trouve = depot.getProductByBarcode(codeBarre)
            }
        }
        return trouve
    }

    @Test
    fun `la sauvegarde complete porte les champs pharmacie`() = runBlocking {
        val db = nouvelleBase()
        val depot = depot(db)
        val vm = InventoryViewModel(depot, ApplicationProvider.getApplicationContext())
        depot.insertProduct(medicament())

        val json = vm.getFullDatabaseJsonSync()

        // On lit le JSON lui-même plutôt que de se fier à un aller-retour : si la clé manque, c'est
        // ici qu'il faut le voir, et non à travers une restauration qui pourrait masquer l'absence.
        // `getJSONArray` et non `optString` : `SyncSerializer` stocke un vrai tableau JSON sous
        // cette clé, pas une chaîne. Lire une chaîne rendrait ce test faussement indulgent.
        val produits = org.json.JSONObject(json).getJSONArray("products")
            .let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
        val trouve = produits.firstOrNull { it.optString("barcode") == "3400930000011" }
        // L'assertion AVANT le `!!` : dans l'autre ordre, un médicament absent lèverait une NPE
        // nue, sans le message qui dit ce qui manquait.
        assertNotNull("Le médicament doit figurer dans la sauvegarde", trouve)
        val medicamentJson = trouve!!

        assertEquals("paracétamol", medicamentJson.optString("dci"))
        assertEquals("500 mg", medicamentJson.optString("dosage"))
        assertEquals("Comprimé", medicamentJson.optString("formeGalenique"))
        assertTrue(medicamentJson.optBoolean("surOrdonnance"))

        db.close()
    }

    @Test
    fun `restaurer sur un appareil neuf conserve la molecule et le prix de gros`() = runBlocking {
        val dbSource = nouvelleBase()
        val depotSource = depot(dbSource)
        val vmSource = InventoryViewModel(depotSource, ApplicationProvider.getApplicationContext())
        depotSource.insertProduct(medicament())
        val sauvegarde = vmSource.getFullDatabaseJsonSync()
        dbSource.close()

        // Un appareil entièrement neuf : c'est le scénario « j'ai perdu mon téléphone ».
        val dbCible = nouvelleBase()
        val depotCible = depot(dbCible)
        val vmCible = InventoryViewModel(depotCible, ApplicationProvider.getApplicationContext())
        vmCible.syncFullDatabaseSync(sauvegarde)

        val restaure = attendreProduit(depotCible, "3400930000011")
        assertNotNull("Le médicament doit être restauré", restaure)
        assertEquals("paracétamol", restaure!!.dci)
        assertEquals("500 mg", restaure.dosage)
        assertEquals("Comprimé", restaure.formeGalenique)
        assertTrue("La délivrance sur ordonnance doit survivre", restaure.surOrdonnance)
        assertEquals(4000.0, restaure.wholesalePrice ?: 0.0, 0.0)

        dbCible.close()
    }

    @Test
    fun `la synchronisation du catalogue porte le prix de gros et la molecule`() = runBlocking {
        val dbSource = nouvelleBase()
        val depotSource = depot(dbSource)
        val vmSource = InventoryViewModel(depotSource, ApplicationProvider.getApplicationContext())
        depotSource.insertProduct(medicament())
        val catalogue = vmSource.getAllProductsJsonSync()
        dbSource.close()

        val dbCible = nouvelleBase()
        val depotCible = depot(dbCible)
        val vmCible = InventoryViewModel(depotCible, ApplicationProvider.getApplicationContext())
        vmCible.syncAllProductsSync(catalogue)

        val recu = attendreProduit(depotCible, "3400930000011")
        assertNotNull("Le produit doit arriver sur le second poste", recu)
        // LE point de ce test : sans le correctif, ce prix valait null et le mode Grossiste
        // facturait 4 500 au lieu de 4 000, sans que rien ne le signale.
        assertEquals(4000.0, recu!!.wholesalePrice ?: 0.0, 0.0)
        assertEquals("paracétamol", recu.dci)
        assertTrue(recu.surOrdonnance)

        dbCible.close()
    }

    @Test
    fun `une sauvegarde ancienne sans ces cles n'efface pas les molecules deja saisies`() = runBlocking {
        val db = nouvelleBase()
        val depot = depot(db)
        val vm = InventoryViewModel(depot, ApplicationProvider.getApplicationContext())
        depot.insertProduct(medicament())

        // On fabrique exactement ce qu'une sauvegarde écrite AVANT ce correctif contient : le même
        // produit, sans les quatre clés pharmacie ni le prix de gros.
        val json = org.json.JSONObject(vm.getFullDatabaseJsonSync())
        val produits: JSONArray = json.getJSONArray("products")
        for (i in 0 until produits.length()) {
            produits.getJSONObject(i).apply {
                remove("dci")
                remove("dosage")
                remove("formeGalenique")
                remove("surOrdonnance")
            }
        }
        // Le TABLEAU, pas sa représentation en chaîne : remis sous forme de chaîne, la
        // désérialisation échouerait et ce test réussirait sans avoir rien exercé.
        json.put("products", produits)

        vm.syncFullDatabaseSync(json.toString())

        // Laisser la fusion s'exécuter : ici le produit existe déjà, donc on ne peut pas attendre
        // son apparition — on attend un délai franc puis on vérifie que rien n'a été effacé.
        delay(1500)
        val apres = depot.getProductByBarcode("3400930000011")
        assertNotNull(apres)
        assertEquals("La molécule ne doit pas être effacée par une sauvegarde ancienne", "paracétamol", apres!!.dci)
        assertEquals("500 mg", apres.dosage)
        assertTrue("La mention sur ordonnance ne doit pas être levée", apres.surOrdonnance)

        db.close()
    }
}
