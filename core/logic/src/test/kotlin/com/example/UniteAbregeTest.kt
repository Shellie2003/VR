package com.example

import com.example.util.UniteAbrege
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ces cas épinglent la table d'abréviations telle qu'elle existe sur Android, maintenant qu'une
 * seule source sert les deux plateformes. Une régression ici ne provoquerait aucune erreur : elle
 * ferait simplement lire au gérant deux vocabulaires différents selon l'écran qu'il regarde — le
 * genre d'écart qu'on ne remarque qu'en comparant les deux appareils côte à côte.
 */
class UniteAbregeTest {

    @Test
    fun `les abreviations des unites courantes sont celles d'Android`() {
        assertEquals("12 kg", libelle(12.0, "Kilogramme"))
        assertEquals("12 L", libelle(12.0, "Litre"))
        assertEquals("12 pcs", libelle(12.0, "Pièce"))
        assertEquals("12 paq", libelle(12.0, "Paquet"))
        assertEquals("12 ctn", libelle(12.0, "Carton"))
        assertEquals("12 sac", libelle(12.0, "Sac"))
        assertEquals("12 bt", libelle(12.0, "Boîte"))
        assertEquals("12 btl", libelle(12.0, "Bouteille"))
        // Le kapoaka est l'unité de mesure réelle des épiceries visées : son abréviation compte.
        assertEquals("12 kap", libelle(12.0, "Kapoaka"))
    }

    @Test
    fun `la casse de l'unite saisie est ignoree`() {
        assertEquals("5 kg", libelle(5.0, "KILOGRAMME"))
        assertEquals("5 kg", libelle(5.0, "kg"))
        assertEquals("5 bt", libelle(5.0, "boite"))
    }

    @Test
    fun `une unite propre a un metier s'affiche telle qu'elle a ete saisie`() {
        // Pharmacie et bar ont leurs propres unités (section 73) ; cette table ne les connaît pas
        // et ne doit surtout pas les remplacer par un repli muet.
        assertEquals("3 Plaquette", libelle(3.0, "Plaquette"))
        assertEquals("3 Casier", libelle(3.0, "Casier"))
    }

    @Test
    fun `le stock est affiche en entier, jamais en decimal`() {
        // La carte produit affiche un stock compact ; `12.0` doit se lire « 12 » et non « 12.0 ».
        assertEquals("12 kg", libelle(12.0, "Kilogramme"))
        assertEquals("12 kg", libelle(12.9, "Kilogramme"))
    }

    @Test
    fun `le prefixe suit la langue active`() {
        assertEquals("Tahiry: 12 kg", UniteAbrege.libelleBadgeStock(12.0, "Kilogramme", "mg"))
        assertEquals("Total: 12 kg", UniteAbrege.libelleBadgeStock(12.0, "Kilogramme", "fr"))
        assertEquals("Total: 12 kg", UniteAbrege.libelleBadgeStock(12.0, "Kilogramme", "en"))
    }

    @Test
    fun `une unite vide reproduit le libelle tronque de l'original`() {
        // « Tahir » est très probablement une coquille pour « Tahiry » dans le code Android. Le
        // comportement est reproduit à l'identique et signalé dans le KDoc : corriger un libellé
        // déjà en production est une décision du gérant, pas un effet de bord d'un portage.
        assertEquals("Total: Tahir", UniteAbrege.libelleBadgeStock(7.0, "", "fr"))
    }

    private fun libelle(stock: Double, unite: String): String {
        val (valeur, abreviation) = UniteAbrege.valeurEtAbreviation(stock, unite)
        return if (unite.isEmpty()) valeur else "$valeur $abreviation"
    }
}
