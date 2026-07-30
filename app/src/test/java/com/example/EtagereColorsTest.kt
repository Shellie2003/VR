package com.example

import com.example.util.EtagereColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EtagereColorsTest {

    @Test
    fun `hex valide en majuscules est accepte tel quel`() {
        assertEquals("#4CAF50", EtagereColors.normaliser("#4CAF50"))
    }

    @Test
    fun `hex valide en minuscules est normalise en majuscules`() {
        assertEquals("#4CAF50", EtagereColors.normaliser("#4caf50"))
    }

    @Test
    fun `espaces autour de la valeur sont tolérés`() {
        assertEquals("#4CAF50", EtagereColors.normaliser("  #4caf50  "))
    }

    @Test
    fun `null ou vide -- aucune couleur`() {
        assertNull(EtagereColors.normaliser(null))
        assertNull(EtagereColors.normaliser(""))
        assertNull(EtagereColors.normaliser("   "))
    }

    @Test
    fun `format sans dieze est rejete`() {
        assertNull(EtagereColors.normaliser("4CAF50"))
    }

    @Test
    fun `format avec canal alpha (8 chiffres) est rejete`() {
        assertNull(EtagereColors.normaliser("#FF4CAF50"))
    }

    @Test
    fun `nom de couleur ou valeur corrompue est rejete`() {
        assertNull(EtagereColors.normaliser("green"))
        assertNull(EtagereColors.normaliser("#GGGGGG"))
        assertNull(EtagereColors.normaliser("rgb(0,0,0)"))
    }

    @Test
    fun `la palette par defaut ne contient que des hex valides`() {
        EtagereColors.PALETTE.forEach { hex ->
            assertEquals(hex, EtagereColors.normaliser(hex))
        }
    }
}
