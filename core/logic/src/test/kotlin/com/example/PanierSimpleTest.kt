package com.example

import com.example.util.PanierSimple
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le panier est le seul endroit de l'écran d'accueil où une erreur se traduirait par un mauvais
 * chiffre **devant le client**. Ces cas couvrent donc chaque transition, y compris celles qu'on
 * n'écrit jamais à la main : retirer un produit absent, retirer plus que la quantité présente.
 */
class PanierSimpleTest {

    @Test
    fun `ajouter cree la ligne puis l'incremente`() {
        var panier = PanierSimple.ajouter(emptyMap(), 7)
        assertEquals(1.0, panier[7])
        panier = PanierSimple.ajouter(panier, 7)
        assertEquals(2.0, panier[7])
    }

    @Test
    fun `retirer la derniere unite fait disparaitre la ligne`() {
        // Le point le plus important : une ligne à zéro laisserait un badge « 0 » collé sur un
        // produit que personne n'a commandé.
        val panier = PanierSimple.retirer(mapOf(7 to 1.0), 7)
        assertFalse("La ligne doit disparaître, pas tomber à zéro", panier.containsKey(7))
        assertTrue(panier.isEmpty())
    }

    @Test
    fun `retirer au dela de la quantite ne rend jamais un solde negatif`() {
        val panier = PanierSimple.retirer(mapOf(7 to 2.0), 7, quantite = 5.0)
        assertFalse(panier.containsKey(7))
    }

    @Test
    fun `retirer un produit absent ne change rien`() {
        // `assertSame` et non `assertEquals` : on vérifie que la MÊME référence est rendue, donc
        // que Compose ne recomposera pas pour un geste sans effet.
        val depart = mapOf(7 to 2.0)
        assertSame(depart, PanierSimple.retirer(depart, 99))
    }

    @Test
    fun `ajouter une quantite nulle ou negative est refuse sans rien changer`() {
        val depart = mapOf(7 to 2.0)
        assertSame(depart, PanierSimple.ajouter(depart, 7, quantite = 0.0))
        assertSame(depart, PanierSimple.ajouter(depart, 7, quantite = -3.0))
    }

    @Test
    fun `les lignes sont independantes`() {
        var panier = PanierSimple.ajouter(emptyMap(), 1)
        panier = PanierSimple.ajouter(panier, 2, quantite = 3.0)
        panier = PanierSimple.retirer(panier, 1)
        assertFalse(panier.containsKey(1))
        assertEquals(3.0, panier[2])
    }

    @Test
    fun `les quantites decimales sont conservees`() {
        // Un produit au kilo se vend en fractions : 0,5 kg puis 0,25 kg.
        var panier = PanierSimple.ajouter(emptyMap(), 4, quantite = 0.5)
        panier = PanierSimple.ajouter(panier, 4, quantite = 0.25)
        assertEquals(0.75, panier[4]!!, 1e-9)
    }

    @Test
    fun `le nombre d'articles additionne toutes les lignes`() {
        assertEquals(0.0, PanierSimple.nombreArticles(emptyMap()), 1e-9)
        assertEquals(4.5, PanierSimple.nombreArticles(mapOf(1 to 2.0, 2 to 2.5)), 1e-9)
    }
}
