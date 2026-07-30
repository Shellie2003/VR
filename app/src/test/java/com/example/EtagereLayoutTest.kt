package com.example

import com.example.data.model.ProduitNiveau
import com.example.util.EtagereLayout
import com.example.util.EtagereLayout.Cote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EtagereLayoutTest {

    @Test
    fun `premiere position toujours 0, quel que soit le cote`() {
        assertEquals(0, EtagereLayout.nouvellePosition(emptyList(), Cote.DEBUT))
        assertEquals(0, EtagereLayout.nouvellePosition(emptyList(), Cote.FIN))
    }

    @Test
    fun `un seul element existant`() {
        assertEquals(-1, EtagereLayout.nouvellePosition(listOf(0), Cote.DEBUT))
        assertEquals(1, EtagereLayout.nouvellePosition(listOf(0), Cote.FIN))
    }

    @Test
    fun `positions non contigues et desordonnees`() {
        assertEquals(-2, EtagereLayout.nouvellePosition(listOf(-1, 0, 1), Cote.DEBUT))
        assertEquals(2, EtagereLayout.nouvellePosition(listOf(-1, 0, 1), Cote.FIN))
        assertEquals(1, EtagereLayout.nouvellePosition(listOf(5, 2, 8), Cote.DEBUT))
        assertEquals(9, EtagereLayout.nouvellePosition(listOf(5, 2, 8), Cote.FIN))
    }

    @Test
    fun `un ajout ne modifie jamais les positions existantes`() {
        // Propriété clé : la fonction calcule seulement la position du NOUVEL élément, elle ne
        // touche jamais à la liste reçue (les positions existantes restent stables ailleurs).
        val existantes = listOf(0, 1, 2)
        EtagereLayout.nouvellePosition(existantes, Cote.DEBUT)
        assertEquals(listOf(0, 1, 2), existantes)
    }

    // --- diffEmplacementUnique : sélecteur de rayon de la fiche produit ---

    private fun lien(id: Long, niveauId: Long) = ProduitNiveau(id = id, niveauId = niveauId, produitId = 42)

    @Test
    fun `aucun emplacement existant, choix d'un niveau -- simple ajout`() {
        val diff = EtagereLayout.diffEmplacementUnique(emptyList(), niveauChoisiId = 5L)
        assertTrue(diff.liensASupprimer.isEmpty())
        assertEquals(5L, diff.niveauAAjouter)
    }

    @Test
    fun `aucun emplacement existant, aucun choix -- rien a faire`() {
        val diff = EtagereLayout.diffEmplacementUnique(emptyList(), niveauChoisiId = null)
        assertTrue(diff.liensASupprimer.isEmpty())
        assertNull(diff.niveauAAjouter)
    }

    @Test
    fun `deja au bon endroit -- aucune ecriture`() {
        val diff = EtagereLayout.diffEmplacementUnique(listOf(lien(1, niveauId = 5L)), niveauChoisiId = 5L)
        assertTrue(diff.liensASupprimer.isEmpty())
        assertNull(diff.niveauAAjouter)
    }

    @Test
    fun `changement d'emplacement -- l'ancien est retire, le nouveau ajoute`() {
        val diff = EtagereLayout.diffEmplacementUnique(listOf(lien(1, niveauId = 5L)), niveauChoisiId = 9L)
        assertEquals(listOf(1L), diff.liensASupprimer)
        assertEquals(9L, diff.niveauAAjouter)
    }

    @Test
    fun `choix vide -- tous les emplacements existants sont retires`() {
        val diff = EtagereLayout.diffEmplacementUnique(listOf(lien(1, niveauId = 5L), lien(2, niveauId = 9L)), niveauChoisiId = null)
        assertEquals(listOf(1L, 2L), diff.liensASupprimer)
        assertNull(diff.niveauAAjouter)
    }

    @Test
    fun `plusieurs emplacements existants (many-to-many depuis l'ecran Etagere) -- convergence vers un seul`() {
        // Un produit ajouté à 2 niveaux depuis l'écran Étagère, puis dont on choisit un
        // emplacement unique dans la fiche produit : les deux anciens sont retirés, un seul reste.
        val diff = EtagereLayout.diffEmplacementUnique(
            listOf(lien(1, niveauId = 5L), lien(2, niveauId = 9L)),
            niveauChoisiId = 5L
        )
        assertEquals(listOf(2L), diff.liensASupprimer)
        assertNull(diff.niveauAAjouter) // déjà présent dans le niveau 5, rien à ajouter
    }
}
