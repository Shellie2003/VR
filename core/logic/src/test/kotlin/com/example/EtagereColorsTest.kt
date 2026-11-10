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

    @Test
    fun `versArgb force l'octet alpha, sans quoi la couleur serait invisible`() {
        // Une valeur sans alpha (`0x2E7D32`) ne donnerait pas une couleur fausse mais un élément
        // TOTALEMENT transparent — le piège déjà épinglé par `VarotraPaletteTest`.
        assertEquals(0xFF2E7D32.toInt(), EtagereColors.versArgb("#2E7D32"))
        assertEquals(0xFF000000.toInt(), EtagereColors.versArgb("#000000"))
        assertEquals(0xFFFFFFFF.toInt(), EtagereColors.versArgb("#FFFFFF"))
    }

    @Test
    fun `versArgb accepte les minuscules puisque normaliser les accepte`() {
        assertEquals(EtagereColors.versArgb("#2E7D32"), EtagereColors.versArgb("#2e7d32"))
    }

    @Test
    fun `versArgb rend null sur une valeur inexploitable au lieu de lever`() {
        // Cette fonction remplace `android.graphics.Color.parseColor`, qui LÈVE sur une entrée
        // invalide. Une couleur de rayon corrompue (synchronisation partielle, ancien format) ne
        // doit jamais faire tomber l'écran du plan d'étagères.
        assertNull(EtagereColors.versArgb(null))
        assertNull(EtagereColors.versArgb(""))
        assertNull(EtagereColors.versArgb("green"))
        assertNull(EtagereColors.versArgb("#GGGGGG"))
        assertNull(EtagereColors.versArgb("2E7D32"))
    }

    @Test
    fun `toute la palette se convertit`() {
        EtagereColors.PALETTE.forEach { hex ->
            assertEquals("Conversion impossible pour $hex", true, EtagereColors.versArgb(hex) != null)
        }
    }
}
