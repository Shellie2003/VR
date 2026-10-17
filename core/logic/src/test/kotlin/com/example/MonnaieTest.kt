package com.example

import com.example.data.model.CartItem
import com.example.util.Monnaie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * C'est le calcul de l'argent rendu au client. Une régression ici ne lève aucune erreur et ne se
 * voit nulle part : elle fait rendre la mauvaise somme, et le manque n'apparaît qu'au comptage du
 * soir sans qu'on puisse dire quelle vente l'a causé.
 */
class MonnaieTest {

    private fun ligne(prix: Double, quantite: Double, taxable: Boolean = false, taux: Double = 0.0) =
        CartItem(
            id = "product_1",
            name = "Article",
            price = prix,
            quantity = quantite,
            unit = "Pièce",
            productId = 1,
            taxable = taxable,
            tauxTaxe = taux
        )

    @Test
    fun `le total somme les lignes quantites decimales comprises`() {
        val panier = listOf(ligne(3500.0, 2.0), ligne(9800.0, 0.25))
        assertEquals(3500.0 * 2 + 9800.0 * 0.25, Monnaie.total(panier), 1e-9)
    }

    @Test
    fun `un panier vide totalise zero et non une erreur`() {
        // La caisse affiche le total avant qu'un article soit ajouté : ce cas est le plus fréquent
        // de tous, et il ne doit surtout pas lever.
        assertEquals(0.0, Monnaie.total(emptyList()), 1e-9)
        assertEquals(0.0, Monnaie.totalTaxe(emptyList()), 1e-9)
    }

    @Test
    fun `la TVA est isolee du prix TTC et ne s'y ajoute pas`() {
        // Section 20 : le prix payé par le client ne change jamais ; `tauxTaxe` ne sert qu'à dire
        // quelle part de ce prix est de la taxe. Un total de 11 500 à 15 % contient 1 500 de TVA.
        val panier = listOf(ligne(11500.0, 1.0, taxable = true, taux = 15.0))
        assertEquals(11500.0, Monnaie.total(panier), 1e-9)
        assertEquals(1500.0, Monnaie.totalTaxe(panier), 1e-6)
    }

    @Test
    fun `une ligne non taxable ne declare aucune taxe meme avec un taux renseigne`() {
        val panier = listOf(ligne(11500.0, 1.0, taxable = false, taux = 15.0))
        assertEquals(0.0, Monnaie.totalTaxe(panier), 1e-9)
    }

    @Test
    fun `le rendu reste signe pour distinguer le manque du surplus`() {
        // Remplacer cette valeur signée par une valeur absolue ferait afficher « rendu » sur un
        // paiement insuffisant : le vendeur rendrait de l'argent sur une vente non couverte.
        assertEquals(2500.0, Monnaie.rendu(total = 7500.0, recu = 10000.0), 1e-9)
        assertEquals(-500.0, Monnaie.rendu(total = 7500.0, recu = 7000.0), 1e-9)
    }

    @Test
    fun `un paiement exact est suffisant`() {
        // La frontière du `>=` : au centime près, c'est le cas le plus courant en espèces.
        assertTrue(Monnaie.suffisant(total = 7500.0, recu = 7500.0))
        assertEquals(0.0, Monnaie.rendu(total = 7500.0, recu = 7500.0), 1e-9)
        assertFalse(Monnaie.suffisant(total = 7500.0, recu = 7499.0))
    }

    @Test
    fun `rien de recu sur un panier non vide n'est pas suffisant`() {
        assertFalse(Monnaie.suffisant(total = 1000.0, recu = 0.0))
    }

    @Test
    fun `le libelle annonce le rendu dans les trois langues`() {
        assertTrue(Monnaie.libelleRendu(7500.0, 10000.0, "fr").startsWith("Rendu:"))
        assertTrue(Monnaie.libelleRendu(7500.0, 10000.0, "mg").startsWith("Famerenana:"))
        assertTrue(Monnaie.libelleRendu(7500.0, 10000.0, "en").startsWith("Change:"))
    }

    @Test
    fun `le libelle annonce le manque et jamais un montant negatif`() {
        // « Manque Ar -500 » ferait hésiter le vendeur à l'instant où il doit agir vite.
        listOf("fr", "mg", "en").forEach { langue ->
            val libelle = Monnaie.libelleRendu(total = 7500.0, recu = 7000.0, langue = langue)
            assertFalse("Signe négatif affiché en $langue : $libelle", libelle.contains("-"))
            assertTrue("Montant absent en $langue : $libelle", libelle.contains("500"))
        }
    }

    @Test
    fun `une langue inconnue retombe sur l'anglais`() {
        assertEquals(
            Monnaie.libelleRendu(7500.0, 10000.0, "en"),
            Monnaie.libelleRendu(7500.0, 10000.0, "xx")
        )
    }

    @Test
    fun `les coupures rapides sont celles de l'ecran de caisse`() {
        // Elles étaient recopiées trois fois dans `CalculatorScreen` ; ce test épingle la liste
        // pour qu'un changement soit un choix et non un oubli de mise à jour d'une des copies.
        assertEquals(listOf(1000, 2000, 5000, 10000, 20000), Monnaie.coupuresRapides)
    }
}
