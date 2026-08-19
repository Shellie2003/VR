package com.example

import com.example.data.model.Etagere
import com.example.data.model.NiveauEtagere
import com.example.data.model.Product
import com.example.ui.viewmodel.InventoryViewModel.EtagereAvecNiveaux
import com.example.ui.viewmodel.InventoryViewModel.NiveauAvecProduits
import com.example.util.RechercheEtagere
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * « Où est rangé ce produit ? » — la recherche inverse de celle qui existait déjà dans l'écran
 * Étagère (laquelle sert à choisir un produit à POSER dans une case).
 *
 * Testée sans Compose ni base de données : c'est `indexEtagere` qui pilote le défilement animé de
 * l'écran, donc une erreur d'index conduirait le vendeur vers la mauvaise étagère — exactement le
 * genre de bug qu'un test d'interface ne rattraperait pas facilement.
 */
class RechercheEtagereTest {

    private fun produit(id: Int, nom: String, codeBarre: String = "") =
        Product(id = id, name = nom, price = 100.0, category = "Test", stock = 5.0, barcode = codeBarre)

    private fun niveau(id: Long, nom: String, produits: List<Product>) = NiveauAvecProduits(
        niveau = NiveauEtagere(id = id, etagereId = 0, position = 0, nom = nom),
        liens = emptyList(),
        produits = produits
    )

    private fun etagere(id: Long, nom: String, niveaux: List<NiveauAvecProduits>) = EtagereAvecNiveaux(
        etagere = Etagere(id = id, nom = nom, position = 0),
        niveaux = niveaux
    )

    private val plan = listOf(
        etagere(1, "Étagère A", listOf(
            niveau(10, "Haut", listOf(produit(100, "Farine Voila", "611100")))
        )),
        etagere(2, "Étagère B", listOf(
            niveau(20, "Haut", listOf(produit(200, "Savon Cali"))),
            niveau(21, "Bas", listOf(produit(201, "Sucre roux"), produit(202, "Café Taf")))
        )),
        etagere(3, "Réserve", listOf(
            niveau(30, "", listOf(produit(201, "Sucre roux")))
        ))
    )

    @Test
    fun `l'index renvoye est celui de l'etagere dans le plan, pas un identifiant`() {
        val resultats = RechercheEtagere.chercher(plan, "Café")

        assertEquals(1, resultats.size)
        // « Étagère B » est la DEUXIÈME du plan : index 1, alors que son id vaut 2. Confondre les
        // deux ferait défiler vers la mauvaise étagère.
        assertEquals(1, resultats[0].indexEtagere)
        assertEquals(2L, resultats[0].etagereId)
        assertEquals(21L, resultats[0].niveauId)
        assertEquals("Étagère B", resultats[0].etagereNom)
    }

    @Test
    fun `un produit range a deux endroits remonte les deux emplacements`() {
        // Cas réel : un article courant est en rayon ET en réserve. Renvoyer le premier trouvé
        // enverrait parfois le vendeur au mauvais endroit.
        val resultats = RechercheEtagere.chercher(plan, "Sucre")

        assertEquals(2, resultats.size)
        assertEquals(listOf(1, 2), resultats.map { it.indexEtagere })
        assertEquals(listOf("Étagère B", "Réserve"), resultats.map { it.etagereNom })
    }

    @Test
    fun `la recherche par code-barres fonctionne - le geste le plus rapide au comptoir`() {
        val resultats = RechercheEtagere.chercher(plan, "611100")

        assertEquals(1, resultats.size)
        assertEquals("Farine Voila", resultats[0].produitNom)
    }

    @Test
    fun `la casse et les espaces autour ne changent rien`() {
        assertEquals(1, RechercheEtagere.chercher(plan, "  savon  ").size)
        assertEquals(1, RechercheEtagere.chercher(plan, "SAVON").size)
    }

    @Test
    fun `une requete trop courte ne renvoie rien plutot que tout le plan`() {
        assertTrue(RechercheEtagere.chercher(plan, "").isEmpty())
        assertTrue(RechercheEtagere.chercher(plan, " ").isEmpty())
        assertTrue(RechercheEtagere.chercher(plan, "a").isEmpty())
    }

    @Test
    fun `un produit absent du plan ne remonte pas, meme s'il existe au catalogue`() {
        // La recherche ne parcourt que ce qui est RANGÉ : un produit jamais placé n'a pas
        // d'emplacement à montrer.
        assertTrue(RechercheEtagere.chercher(plan, "Introuvable").isEmpty())
    }

    @Test
    fun `un plan vide ne fait pas echouer la recherche`() {
        assertTrue(RechercheEtagere.chercher(emptyList(), "Farine").isEmpty())
    }

    @Test
    fun `un niveau sans nom reste exploitable - l'ecran affichera un libelle par defaut`() {
        val resultats = RechercheEtagere.chercher(plan, "Sucre")
        val enReserve = resultats.first { it.etagereNom == "Réserve" }
        assertEquals("", enReserve.niveauNom)
    }
}
