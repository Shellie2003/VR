package com.example

import com.example.util.EtagereLayout
import com.example.util.EtagereLayout.Cote
import org.junit.Assert.assertEquals
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
}
