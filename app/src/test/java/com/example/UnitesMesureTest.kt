package com.example

import com.example.util.ShopMode
import com.example.util.UnitesMesure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unités de vente proposées selon le métier de la boutique.
 *
 * L'enjeu principal de ces tests n'est pas le contenu des listes, c'est la NON-PERTE de données :
 * des milliers de produits déjà enregistrés portent une unité précise, et le formulaire doit
 * toujours pouvoir la réafficher — même si la boutique a changé de métier entre-temps, ou si le
 * produit vient d'un autre poste par la synchronisation multi-terminal.
 */
class UnitesMesureTest {

    @Test
    fun `le tronc commun est identique dans tous les metiers et garde son ordre`() {
        // « Pièce » est l'unité par défaut de tout produit existant : la déplacer ou la renommer
        // ferait que le menu n'afficherait plus l'unité réellement enregistrée.
        val attendu = listOf(
            "Pièce", "Litre", "Kilogramme", "Paquet", "Carton", "Sac", "Boîte", "Bouteille", "Tasse/Kapoaka"
        )
        for (mode in ShopMode.entries) {
            assertEquals("Tronc commun altéré en mode $mode", attendu, UnitesMesure.pourMode(mode).take(attendu.size))
        }
    }

    @Test
    fun `le mode detail ne propose que le tronc commun`() {
        assertEquals(9, UnitesMesure.pourMode(ShopMode.DETAIL).size)
    }

    @Test
    fun `la pharmacie propose ses conditionnements`() {
        val unites = UnitesMesure.pourMode(ShopMode.PHARMACIE)
        assertTrue(unites.contains("Plaquette"))
        assertTrue(unites.contains("Flacon"))
        assertTrue(unites.contains("Comprimé"))
        // et pas ceux du bar
        assertTrue(!unites.contains("Casier"))
    }

    @Test
    fun `le bar propose ses conditionnements`() {
        val unites = UnitesMesure.pourMode(ShopMode.BAR)
        assertTrue(unites.contains("Casier"))
        assertTrue(unites.contains("Caseau"))
        assertTrue(unites.contains("Canette"))
        assertTrue(!unites.contains("Plaquette"))
    }

    @Test
    fun `aucun doublon dans aucun metier`() {
        for (mode in ShopMode.entries) {
            val unites = UnitesMesure.pourMode(mode)
            assertEquals("Doublon en mode $mode", unites.size, unites.distinct().size)
        }
    }

    @Test
    fun `l'unite deja enregistree reste selectionnable meme si la boutique a change de metier`() {
        // Le cas qui ferait perdre une donnée : un médicament en « Plaquette » rouvert depuis une
        // boutique repassée en mode épicerie. Sans ce rattrapage, le menu afficherait une unité
        // vide et l'enregistrement écraserait « Plaquette » silencieusement.
        val unites = UnitesMesure.pourFormulaire(ShopMode.DETAIL, uniteActuelle = "Plaquette")

        assertTrue(unites.contains("Plaquette"))
        assertEquals("L'unité rattrapée doit être ajoutée à la fin", "Plaquette", unites.last())
    }

    @Test
    fun `une unite deja presente n'est pas ajoutee en double`() {
        val unites = UnitesMesure.pourFormulaire(ShopMode.DETAIL, uniteActuelle = "Litre")
        assertEquals(unites.size, unites.distinct().size)
        assertEquals(UnitesMesure.pourMode(ShopMode.DETAIL).size, unites.size)
    }

    @Test
    fun `la comparaison ignore la casse pour ne pas creer de faux doublon`() {
        val unites = UnitesMesure.pourFormulaire(ShopMode.DETAIL, uniteActuelle = "litre")
        assertEquals(UnitesMesure.pourMode(ShopMode.DETAIL).size, unites.size)
    }

    @Test
    fun `un produit neuf sans unite ne pollue pas la liste`() {
        assertEquals(UnitesMesure.pourMode(ShopMode.BAR), UnitesMesure.pourFormulaire(ShopMode.BAR, null))
        assertEquals(UnitesMesure.pourMode(ShopMode.BAR), UnitesMesure.pourFormulaire(ShopMode.BAR, "   "))
    }
}
