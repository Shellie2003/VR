package com.example

import com.example.util.MiseEnPageTableau
import com.example.util.TableauExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cette arithmétique décide de ce qui tient sur la feuille. Une erreur n'y lève rien : elle sort
 * la dernière colonne de la page, ou fait tourner l'impression sans jamais avancer. Le gérant
 * découvre alors que son inventaire imprimé n'a pas de colonne « prix » — après avoir imprimé
 * quarante pages.
 */
class MiseEnPageTableauTest {

    // --- Largeurs de colonnes -----------------------------------------------------------------

    @Test
    fun `les poids sont relatifs et remplissent exactement la largeur`() {
        val largeurs = MiseEnPageTableau.largeursColonnes(400f, listOf(1f, 1f, 2f))
        assertEquals(listOf(100f, 100f, 200f), largeurs)
        // L'invariant qui compte : ce qui dépasse la feuille ne s'imprime pas.
        assertEquals(400f, largeurs.sum(), 0.01f)
    }

    @Test
    fun `une somme de poids quelconque est normalisee`() {
        // Les poids de `Tableau` ne sont pas des pourcentages : ils disent seulement qu'une
        // colonne « Produit » mérite deux fois la place d'une colonne « Date ».
        val largeurs = MiseEnPageTableau.largeursColonnes(500f, listOf(1.2f, 0.8f))
        assertEquals(listOf(300f, 200f), largeurs)
    }

    @Test
    fun `un seul poids fautif rend TOUTES les colonnes egales, jamais une colonne invisible`() {
        // Ne mettre à zéro que la colonne fautive la rendrait invisible à l'impression — un prix
        // ou un solde qui disparaît sans que rien ne le signale. Un tableau régulier se remarque
        // et reste complet.
        assertEquals(
            listOf(100f, 100f, 100f),
            MiseEnPageTableau.largeursColonnes(300f, listOf(1f, -5f, 1f))
        )
        assertEquals(
            listOf(100f, 100f, 100f),
            MiseEnPageTableau.largeursColonnes(300f, listOf(1f, 0f, 1f))
        )
    }

    @Test
    fun `une largeur utile nulle ou negative ne produit pas de largeur negative`() {
        // Format exotique, marges trop grandes : la place utile peut manquer. Une largeur
        // négative ferait dessiner les colonnes à l'envers.
        MiseEnPageTableau.largeursColonnes(-100f, listOf(1f, 1f)).forEach {
            assertTrue("Largeur négative : $it", it >= 0f)
        }
    }

    @Test
    fun `aucune colonne, aucune largeur`() {
        assertEquals(emptyList<Float>(), MiseEnPageTableau.largeursColonnes(400f, emptyList()))
    }

    @Test
    fun `les positions se suivent sans trou ni chevauchement`() {
        val positions = MiseEnPageTableau.positionsColonnes(50f, listOf(100f, 200f, 100f))
        assertEquals(listOf(50f, 150f, 350f), positions)
    }

    @Test
    fun `les cinq tableaux reels tiennent dans la largeur de leur feuille`() {
        // Le test qui relie l'arithmétique aux vrais tableaux : si un `poidsColonnes` de
        // `TableauExport` contenait un zéro ou un négatif, ce test le dirait ici plutôt qu'à
        // l'impression.
        val tableaux = listOf(
            TableauExport.ventes(emptyList()),
            TableauExport.stock(emptyList()),
            TableauExport.dettes(emptyList()),
            TableauExport.mouvementsCaisse(emptyList()),
            TableauExport.reappros(emptyList())
        )
        tableaux.forEach { tableau ->
            val largeurs = MiseEnPageTableau.largeursColonnes(700f, tableau.poidsColonnes)
            assertEquals(tableau.titre, tableau.entetes.size, largeurs.size)
            assertEquals(tableau.titre, 700f, largeurs.sum(), 0.01f)
            largeurs.forEach { assertTrue("${tableau.titre} : colonne à $it", it > 0f) }
        }
    }

    // --- Découpage en pages --------------------------------------------------------------------

    @Test
    fun `le nombre de lignes par page tient compte de l en-tete`() {
        assertEquals(33, MiseEnPageTableau.lignesParPage(700f, 40f, 20f))
        assertEquals(4, MiseEnPageTableau.lignesParPage(100f, 20f, 20f))
    }

    @Test
    fun `au moins une ligne par page, meme quand rien ne tient`() {
        // LE cas qui compte : rendre zéro ferait tourner la boucle d'impression sans jamais
        // avancer — une impression qui ne s'arrête pas est bien pire qu'une ligne qui déborde.
        assertEquals(1, MiseEnPageTableau.lignesParPage(30f, 20f, 20f))
        assertEquals(1, MiseEnPageTableau.lignesParPage(0f, 0f, 20f))
        assertEquals(1, MiseEnPageTableau.lignesParPage(100f, 20f, 0f))
        assertEquals(1, MiseEnPageTableau.lignesParPage(100f, 20f, -5f))
    }

    @Test
    fun `un tableau vide fait une page et non zero`() {
        // Elle porte le titre et les en-têtes : c'est l'information « il n'y avait rien dans
        // cette liste ». Zéro page ferait croire à une panne d'impression.
        assertEquals(1, MiseEnPageTableau.nombreDePages(0, 10))
        assertEquals(IntRange.EMPTY, MiseEnPageTableau.lignesDeLaPage(0, 0, 10))
        // Mais une DEUXIÈME page vide n'a aucune raison d'exister.
        assertNull(MiseEnPageTableau.lignesDeLaPage(1, 0, 10))
    }

    @Test
    fun `le compte de pages est exact au pli`() {
        assertEquals(1, MiseEnPageTableau.nombreDePages(1, 10))
        assertEquals(1, MiseEnPageTableau.nombreDePages(10, 10))
        assertEquals(2, MiseEnPageTableau.nombreDePages(11, 10))
        assertEquals(3, MiseEnPageTableau.nombreDePages(25, 10))
    }

    @Test
    fun `les intervalles de pages couvrent toutes les lignes, sans trou ni doublon`() {
        val lignes = 25
        val parPage = 10
        val vues = mutableListOf<Int>()
        var page = 0
        while (true) {
            val intervalle = MiseEnPageTableau.lignesDeLaPage(page, lignes, parPage) ?: break
            vues.addAll(intervalle)
            page++
            // Garde-fou du test lui-même : si la règle ne terminait pas, le test doit échouer
            // plutôt que tourner indéfiniment en CI.
            assertTrue("Trop de pages", page < 100)
        }
        assertEquals((0 until lignes).toList(), vues)
        assertEquals(MiseEnPageTableau.nombreDePages(lignes, parPage), page)
    }

    @Test
    fun `la derniere page s arrete a la derniere ligne`() {
        assertEquals(0..9, MiseEnPageTableau.lignesDeLaPage(0, 25, 10))
        assertEquals(20..24, MiseEnPageTableau.lignesDeLaPage(2, 25, 10))
        assertNull(MiseEnPageTableau.lignesDeLaPage(3, 25, 10))
    }

    @Test
    fun `une page negative ou un decoupage nul ne rend rien`() {
        assertNull(MiseEnPageTableau.lignesDeLaPage(-1, 25, 10))
        assertNull(MiseEnPageTableau.lignesDeLaPage(0, 25, 0))
    }
}
