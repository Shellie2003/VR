package com.example

import com.example.data.model.Restock
import com.example.data.model.Sale
import com.example.data.model.SoldItem
import com.example.util.FiltreHistorique
import com.example.util.RapportFinancier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Le filtrage de l'historique décide de **ce que le gérant compte comme la recette du jour**. Une
 * régression ici ne lèverait aucune erreur : elle afficherait une autre journée, ou la même journée
 * avec un total différent de celui du tableau de bord.
 */
class FiltreHistoriqueTest {

    /** Un instant fixe, pour ne pas dépendre du jour d'exécution en CI. */
    private fun instant(annee: Int, mois: Int, jour: Int, heure: Int, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(annee, mois, jour, heure, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun vente(horodatage: Long, montant: Double, vararg noms: String) = Sale(
        timestamp = horodatage,
        totalAmount = montant,
        items = noms.map { SoldItem(productId = 0, name = it, quantity = 1.0, price = montant) }
    )

    private fun reappro(
        horodatage: Long,
        produit: String,
        fournisseur: String? = null,
        unites: Double = 10.0,
        cout: Double = 1000.0
    ) = Restock(
        productId = 1,
        productName = produit,
        cartonsQuantity = 1.0,
        itemsPerCarton = unites,
        totalUnits = unites,
        totalCostPrice = cout,
        unitSellingPrice = 200.0,
        supplierId = null,
        supplierName = fournisseur,
        timestamp = horodatage
    )

    @Test
    fun `minuit appartient au jour, minuit du lendemain non`() {
        val jour = RapportFinancier.bornesDuJour(instant(2026, 0, 15, 14))
        assertTrue("Minuit doit appartenir au jour", jour.contient(jour.debut))
        // Borne haute EXCLUSIVE : c'est ce qui empêche une vente tombant à la milliseconde de
        // bascule d'être comptée dans les DEUX journées, donc le total du jour de dépasser le réel.
        assertFalse("Minuit du lendemain ne doit PAS appartenir au jour", jour.contient(jour.finExclusive))
        assertTrue(jour.contient(jour.finExclusive - 1))
    }

    @Test
    fun `une vente d'hier a 23h59 ne compte pas dans aujourd'hui`() {
        val aujourdhui = instant(2026, 0, 15, 10)
        val hierTard = instant(2026, 0, 14, 23, 59)
        val ventes = listOf(vente(aujourdhui, 1000.0, "Riz"), vente(hierTard, 5000.0, "Riz"))
        val duJour = FiltreHistorique.ventesDuJour(ventes, aujourdhui)
        assertEquals(1, duJour.size)
        assertEquals(1000.0, duJour.first().totalAmount, 0.001)
    }

    @Test
    fun `la recherche trouve un article qui n'est pas le premier de la vente`() {
        // `any` et non le premier article : une vente de dix lignes dont la neuvième porte
        // l'article cherché doit remonter, sinon le gérant conclut qu'il ne l'a pas vendu.
        val ventes = listOf(vente(1L, 3000.0, "Riz", "Savon", "Huile"))
        assertEquals(1, FiltreHistorique.chercherVentes(ventes, "huile").size)
        assertEquals(1, FiltreHistorique.chercherVentes(ventes, "SAVON").size)
        assertEquals(0, FiltreHistorique.chercherVentes(ventes, "zébu").size)
    }

    @Test
    fun `une recherche vide ou en espaces rend toute la liste`() {
        val ventes = listOf(vente(1L, 100.0, "Riz"), vente(2L, 200.0, "Eau"))
        assertEquals(2, FiltreHistorique.chercherVentes(ventes, "").size)
        assertEquals(2, FiltreHistorique.chercherVentes(ventes, "   ").size)
    }

    @Test
    fun `un reappro se trouve par son produit ou par son fournisseur`() {
        // « Qu'est-ce que j'ai pris chez untel ? » est la question qu'on pose à cet onglet, donc le
        // fournisseur compte autant que le produit.
        val liste = listOf(
            reappro(1L, "Riz Makalioka", "Grossiste Analakely"),
            reappro(2L, "Eau minérale", null)
        )
        assertEquals(1, FiltreHistorique.chercherReappros(liste, "analakely").size)
        assertEquals(1, FiltreHistorique.chercherReappros(liste, "riz").size)
        // Un fournisseur absent ne doit pas faire échouer la recherche sur les autres lignes.
        assertEquals(1, FiltreHistorique.chercherReappros(liste, "eau").size)
    }

    @Test
    fun `un reappro sans unite n'a pas de cout unitaire au lieu d'en avoir un infini`() {
        // La carte Android divisait `totalCostPrice / totalUnits` sans garde : à zéro unité, elle
        // affichait « Ar ∞ ». `null` dit la vérité — on ne connaît pas le coût unitaire d'une
        // livraison dont les pièces n'ont pas été comptées.
        assertNull(FiltreHistorique.coutUnitaireReappro(reappro(1L, "Ananas", unites = 0.0)))
        assertEquals(
            100.0,
            FiltreHistorique.coutUnitaireReappro(reappro(1L, "Riz", unites = 10.0, cout = 1000.0))!!,
            0.001
        )
    }

    @Test
    fun `la variation par rapport a la veille est calculee et non inventee`() {
        // L'écran Android affichait « +12% omaly » EN DUR, le même chiffre chaque jour, lu comme
        // une mesure. Ce test fixe le calcul réel.
        val aujourdhui = instant(2026, 0, 15, 10)
        val hier = instant(2026, 0, 14, 10)
        val ventes = listOf(vente(hier, 10000.0, "Riz"), vente(aujourdhui, 12000.0, "Riz"))
        assertEquals(20.0, FiltreHistorique.variationVeille(ventes, aujourdhui)!!, 0.001)
    }

    @Test
    fun `une baisse rend une variation negative`() {
        val aujourdhui = instant(2026, 0, 15, 10)
        val hier = instant(2026, 0, 14, 10)
        val ventes = listOf(vente(hier, 10000.0, "Riz"), vente(aujourdhui, 7500.0, "Riz"))
        assertEquals(-25.0, FiltreHistorique.variationVeille(ventes, aujourdhui)!!, 0.001)
    }

    @Test
    fun `aucune vente la veille ne rend pas une progression mais rien du tout`() {
        // Une progression depuis zéro n'est pas « +100 % », elle n'est pas exprimable. Rendre un
        // nombre ici afficherait une performance inventée, exactement le défaut qu'on corrige.
        val aujourdhui = instant(2026, 0, 15, 10)
        val ventes = listOf(vente(aujourdhui, 12000.0, "Riz"))
        assertNull(FiltreHistorique.variationVeille(ventes, aujourdhui))
    }

    @Test
    fun `la veille d'un premier du mois est bien le dernier jour du mois precedent`() {
        // L'instant de la veille est pris à `debut - 1 ms`, donc il traverse correctement les
        // frontières de mois et d'année — ce qu'un `- 86_400_000` ferait aussi, mais pas au
        // changement d'heure.
        val premierFevrier = instant(2026, 1, 1, 9)
        val trenteUnJanvier = instant(2026, 0, 31, 20)
        val ventes = listOf(vente(trenteUnJanvier, 4000.0, "Riz"), vente(premierFevrier, 2000.0, "Riz"))
        assertEquals(-50.0, FiltreHistorique.variationVeille(ventes, premierFevrier)!!, 0.001)
    }

    @Test
    fun `une liste vide ne leve pas`() {
        val maintenant = instant(2026, 0, 15, 10)
        assertTrue(FiltreHistorique.ventesDuJour(emptyList(), maintenant).isEmpty())
        assertTrue(FiltreHistorique.reapprosDuJour(emptyList(), maintenant).isEmpty())
        assertNull(FiltreHistorique.variationVeille(emptyList(), maintenant))
    }
}
