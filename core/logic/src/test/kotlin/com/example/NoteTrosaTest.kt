package com.example

import com.example.util.NoteTrosa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce découpage décide de ce que le gérant lit comme liste d'articles d'un trosa. Couper au mauvais
 * endroit ferait apparaître des articles que le client n'a jamais achetés — et le gérant les lui
 * réclamerait.
 */
class NoteTrosaTest {

    @Test
    fun `une note vide ou blanche ne produit aucune ligne`() {
        assertTrue(NoteTrosa.articles("").isEmpty())
        assertTrue(NoteTrosa.articles("   ").isEmpty())
    }

    @Test
    fun `les articles sont separes apres la parenthese fermante`() {
        assertEquals(
            listOf("Riz (2 x 3 500)", "Huile (1 x 9 800)"),
            NoteTrosa.articles("Riz (2 x 3 500), Huile (1 x 9 800)")
        )
    }

    @Test
    fun `un nom d'article contenant une virgule reste UN seul article`() {
        // Le cas qui justifie la regex : couper sur chaque virgule inventerait des articles.
        assertEquals(
            listOf("Lait concentré, sucré (1 x 4 000)", "Pain (2 x 500)"),
            NoteTrosa.articles("Lait concentré, sucré (1 x 4 000), Pain (2 x 500)")
        )
    }

    @Test
    fun `un article seul ressort tel quel`() {
        assertEquals(listOf("Riz (1 x 3 500)"), NoteTrosa.articles("Riz (1 x 3 500)"))
    }

    @Test
    fun `une note saisie a la main ressort en une seule ligne`() {
        // Format libre : mieux vaut l'afficher entière que la découper au hasard sur ses virgules.
        val note = "Paiement promis vendredi, voir avec le frère"
        assertEquals(listOf(note), NoteTrosa.articles(note))
    }

    @Test
    fun `les espaces autour des separateurs sont nettoyes`() {
        assertEquals(
            listOf("A (1 x 100)", "B (1 x 200)"),
            NoteTrosa.articles("A (1 x 100),     B (1 x 200)")
        )
    }
}
