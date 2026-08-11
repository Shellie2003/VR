package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Vendeur
import com.example.data.repository.InventoryRepository
import com.example.ui.viewmodel.InventoryViewModel
import com.example.util.AppPreferences
import com.example.util.RecoveryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Rapport utilisateur : « parfois, en ouvrant l'app et en allant directement dans Paramètres via
 * l'icône de l'accueil, le PIN gérant n'est pas demandé ». Cause : `activeVendeurId` est persisté
 * dans les préférences de l'appareil et relu tel quel à chaque construction du ViewModel — un
 * gérant qui s'était connecté puis n'avait jamais explicitement cliqué "déconnexion" restait donc
 * reconnu comme gérant indéfiniment, y compris après une fermeture complète de l'app. Ces tests
 * reproduisent exactement ce scénario (préférence déjà écrite AVANT la construction du ViewModel,
 * comme le ferait un vrai redémarrage à froid de l'app) et vérifient le correctif : les droits
 * gérant exigent désormais une saisie de PIN réussie PENDANT l'exécution en cours.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GerantPinSessionTest {

    @Before
    fun installerDispatcherPrincipalDeTest() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurerDispatcherPrincipal() {
        Dispatchers.resetMain()
    }

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
        deletedRecordDao = db.deletedRecordDao(),
        auditLogDao = db.auditLogDao(),
        etagereDao = db.etagereDao()
    )

    /**
     * `allVendeurs` est un `StateFlow` construit avec `SharingStarted.WhileSubscribed` : la requête
     * Room sous-jacente ne démarre que si quelque chose s'abonne réellement au flux (ce que fait
     * `collectAsState()` côté Compose en production). Un simple polling de `.value` sans abonnement
     * ne déclenche jamais le chargement — d'où l'abonnement explicite ici avant d'attendre.
     */
    private suspend fun attendreVendeursCharges(viewModel: InventoryViewModel) = coroutineScope {
        val job = launch { viewModel.allVendeurs.collect {} }
        repeat(30) {
            if (viewModel.allVendeurs.value.isEmpty()) delay(100)
        }
        job.cancel()
    }

    @Test
    fun `une session gerant persistee depuis une precedente ouverture de l'app ne donne pas les droits sans re-saisie du PIN`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = buildRepository(db)

        val gerantId = repository.insertVendeur(
            Vendeur(nom = "Koto", pinHash = Vendeur.hashPin("1234"), role = Vendeur.ROLE_GERANT, actif = true)
        )

        // Simule exactement le bug signalé : une session gérant déjà persistée AVANT la
        // construction du ViewModel, comme le laisserait une fermeture complète de l'app sans
        // déconnexion explicite au préalable.
        AppPreferences(context).activeVendeurId = gerantId

        val viewModel = InventoryViewModel(repository, context)
        attendreVendeursCharges(viewModel)

        assertTrue(
            "Le PIN doit être redemandé même si un gérant était persisté comme actif au lancement",
            viewModel.requiresGerantPinForSettings()
        )
        assertFalse("estGerant ne doit pas être vrai sans vérification du PIN pendant cette exécution", viewModel.estGerant)

        // Une fois le bon PIN saisi PENDANT cette exécution, l'accès est accordé normalement.
        assertNotNull(viewModel.verifyGerantPin("1234"))
        assertFalse(viewModel.requiresGerantPinForSettings())
        assertTrue(viewModel.estGerant)

        db.close()
    }

    @Test
    fun `logoutVendeur retire les droits gerant de la session en cours`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = buildRepository(db)
        repository.insertVendeur(
            Vendeur(nom = "Koto", pinHash = Vendeur.hashPin("1234"), role = Vendeur.ROLE_GERANT, actif = true)
        )
        val viewModel = InventoryViewModel(repository, context)
        attendreVendeursCharges(viewModel)

        val gerant = viewModel.allVendeurs.value.first()
        assertNotNull(viewModel.verifyGerantPin("1234"))
        assertTrue(viewModel.estGerant)

        viewModel.logoutVendeur()

        assertFalse(viewModel.estGerant)
        assertTrue(viewModel.requiresGerantPinForSettings())

        db.close()
    }

    @Test
    fun `code de recuperation valide permet de reinitialiser un PIN gerant oublie`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = buildRepository(db)
        val gerantId = repository.insertVendeur(
            Vendeur(nom = "Koto", pinHash = Vendeur.hashPin("1234"), role = Vendeur.ROLE_GERANT, actif = true)
        )
        val viewModel = InventoryViewModel(repository, context)
        attendreVendeursCharges(viewModel)

        val codeCorrect = AppPreferences(context).recoveryCodeFormatted

        assertFalse(
            "Un code de récupération incorrect ne doit jamais être accepté",
            viewModel.codeRecuperationValide("VRT-0000-0000-0000")
        )
        assertTrue(viewModel.codeRecuperationValide(codeCorrect))

        assertFalse(
            "Un mauvais code ne doit pas réinitialiser le PIN",
            viewModel.reinitialiserPinGerant("VRT-0000-0000-0000", gerantId, "9999")
        )
        // L'ancien PIN doit toujours fonctionner : rien n'a dû changer suite au refus ci-dessus.
        assertNotNull(viewModel.verifyGerantPin("1234"))
        viewModel.logoutVendeur()

        assertTrue(viewModel.reinitialiserPinGerant(codeCorrect, gerantId, "9999"))
        // La réinitialisation réussie connecte immédiatement ce gérant, sans lui faire ressaisir
        // le PIN qu'il vient de choisir.
        assertTrue(viewModel.estGerant)
        assertFalse(viewModel.requiresGerantPinForSettings())

        // Nouvelle session : l'ancien PIN ne doit plus fonctionner, le nouveau si.
        viewModel.logoutVendeur()
        assertNull("L'ancien PIN doit être révoqué après réinitialisation", viewModel.verifyGerantPin("1234"))
        assertNotNull(viewModel.verifyGerantPin("9999"))

        db.close()
    }

    @Test
    fun `le code de recuperation normalise (minuscules, sans tirets) est accepte`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = buildRepository(db)
        val viewModel = InventoryViewModel(repository, context)

        val codeStocke = AppPreferences(context).firebaseBackupToken
        val saisieDesordonnee = RecoveryCode.format(codeStocke).lowercase().replace("-", " ")

        assertTrue(viewModel.codeRecuperationValide(saisieDesordonnee))

        db.close()
    }
}
