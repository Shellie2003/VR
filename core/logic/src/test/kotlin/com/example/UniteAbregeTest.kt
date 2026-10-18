package com.example

import com.example.util.FormatUtil
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

    @Test
    fun `la forme longue reproduit exactement les libelles de l'historique des ventes`() {
        // Les quatre premiers étaient écrits dans une table locale de `SalesHistoryScreen`, qui
        // avait déjà divergé de celle de la carte produit. Ces cas figent ce que cet écran
        // affichait AVANT l'unification : l'unifier ne devait changer aucun libellé de production.
        assertEquals("kg", longue("Kilogramme"))
        assertEquals("Litatra", longue("Litre"))
        assertEquals("pcs", longue("Pièce"))
        assertEquals("paq", longue("Paquet"))
        assertEquals("Kapoaka", longue("Kapoaka"))
    }

    @Test
    fun `la forme longue rend le libelle brut pour les unites que l'historique n'abregeait pas`() {
        // Sa table locale ne connaissait ni carton, ni sac, ni boîte, ni bouteille : elle tombait
        // donc sur le libellé saisi, avec sa majuscule. La forme longue le reproduit tel quel —
        // sans quoi l'unification aurait modifié ces quatre libellés au passage.
        assertEquals("Carton", longue("Carton"))
        assertEquals("Sac", longue("Sac"))
        assertEquals("Boîte", longue("Boîte"))
        assertEquals("Bouteille", longue("Bouteille"))
    }

    @Test
    fun `les deux formes ne divergent que la ou la place le justifie`() {
        // L'écart entre les deux vocabulaires est ASSUMÉ et limité : il ne porte que sur le litre
        // et le kapoaka, dont la forme longue tient sur une ligne de liste mais pas dans une
        // pastille de 9sp. Partout ailleurs, les deux formes doivent dire exactement la même chose
        // — une divergence qui s'étendrait silencieusement est précisément ce qu'on vient de
        // supprimer.
        // Le litre et le kapoaka divergent par choix de place ; carton/sac/boîte/bouteille parce
        // que la forme longue y rend le libellé brut (ce que l'historique affichait déjà).
        val divergencesAssumees = setOf(
            "litre", "l", "tasse", "kapoaka", "carton", "sac", "boîte", "boite", "bouteille"
        )
        val unites = listOf("Kilogramme", "kg", "Pièce", "pcs", "Paquet", "Plaquette", "Casier", "")
        for (unite in unites) {
            if (unite.lowercase() in divergencesAssumees) continue
            assertEquals(
                "Les deux formes doivent concorder pour « $unite »",
                UniteAbrege.abreviation(unite, UniteAbrege.Forme.COURTE),
                UniteAbrege.abreviation(unite, UniteAbrege.Forme.LONGUE)
            )
        }
    }

    @Test
    fun `la forme courte est celle employee par defaut`() {
        // Le badge de la carte produit et `FormatUtil.formatQty` doivent tous deux obtenir la
        // forme compacte sans avoir à la demander : c'est le cas d'usage le plus fréquent.
        assertEquals("L", UniteAbrege.abreviation("Litre"))
        assertEquals("kap", UniteAbrege.abreviation("Kapoaka"))
    }

    @Test
    fun `formatQty emploie la meme table que le badge de la carte produit`() {
        // C'était la troisième copie du vocabulaire, et la seule qui divergeait de façon visible :
        // elle ne connaissait pas carton/sac/boîte/bouteille, donc le même produit se lisait
        // « 3 Carton » dans l'écran Stock et « Total: 3 ctn » sur sa carte d'accueil.
        assertEquals("3 ctn", FormatUtil.formatQty(3.0, "Carton"))
        assertEquals("3 sac", FormatUtil.formatQty(3.0, "Sac"))
        assertEquals("3 bt", FormatUtil.formatQty(3.0, "Boîte"))
        assertEquals("3 btl", FormatUtil.formatQty(3.0, "Bouteille"))
        // Et les cinq unités qu'elle connaissait déjà sont rendues exactement comme avant.
        assertEquals("3 kg", FormatUtil.formatQty(3.0, "Kilogramme"))
        assertEquals("3 L", FormatUtil.formatQty(3.0, "Litre"))
        assertEquals("3 pcs", FormatUtil.formatQty(3.0, "Pièce"))
        assertEquals("3 paq", FormatUtil.formatQty(3.0, "Paquet"))
        assertEquals("3 kap", FormatUtil.formatQty(3.0, "Kapoaka"))
    }

    @Test
    fun `formatQty sans unite n'ajoute aucun suffixe ni espace`() {
        // La moitié des appels passent une unité vide — exports, reçus, quantités retournables.
        // Un espace de trop y laisserait une colonne mal alignée ou un `.trim()` qui ne trime rien.
        assertEquals("3", FormatUtil.formatQty(3.0, ""))
        assertEquals("2.50", FormatUtil.formatQty(2.5, ""))
    }

    private fun longue(unite: String): String =
        UniteAbrege.abreviation(unite, UniteAbrege.Forme.LONGUE)

    private fun libelle(stock: Double, unite: String): String {
        val (valeur, abreviation) = UniteAbrege.valeurEtAbreviation(stock, unite)
        return if (unite.isEmpty()) valeur else "$valeur $abreviation"
    }
}
