package com.example

import com.example.util.NombreUtil
import com.example.util.enDecimal
import com.example.util.enEntier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verrouille la lecture des saisies numériques.
 *
 * Test volontairement **sans Robolectric** : il n'a besoin d'aucun environnement Android, donc il
 * s'exécute réellement dans la CI aujourd'hui, alors que les tests Robolectric du projet échouent
 * à l'amorçage (dette connue). Une régression ici ne se verrait pas à l'écran — elle produirait un
 * montant faux, pas un plantage.
 */
class NombreUtilTest {

    @Test
    fun `virgule decimale acceptee, c'est le clavier français et malgache`() {
        assertEquals(12.5, NombreUtil.lireDecimal("12,5")!!, 0.0001)
        assertEquals(0.5, NombreUtil.lireDecimal("0,5")!!, 0.0001)
        // Le cas qui donnait zéro sans rien dire, et faussait la monnaie rendue.
        assertEquals(1500.75, NombreUtil.lireDecimal("1500,75")!!, 0.0001)
    }

    @Test
    fun `point decimal toujours accepte, le comportement d'avant ne change pas`() {
        assertEquals(12.5, NombreUtil.lireDecimal("12.5")!!, 0.0001)
        assertEquals(3.0, NombreUtil.lireDecimal("3")!!, 0.0001)
        assertEquals(-4.25, NombreUtil.lireDecimal("-4.25")!!, 0.0001)
    }

    @Test
    fun `separateurs de milliers ecartes, y compris les espaces d'un copier-coller`() {
        assertEquals(150000.0, NombreUtil.lireDecimal("150 000")!!, 0.0001)
        // Espace insécable (U+00A0) et fin insécable (U+202F) : ce que colle un presse-papier.
        assertEquals(150000.0, NombreUtil.lireDecimal("150 000")!!, 0.0001)
        assertEquals(150000.0, NombreUtil.lireDecimal("150 000")!!, 0.0001)
    }

    @Test
    fun `le dernier separateur est le decimal, dans les deux conventions`() {
        assertEquals(1500.50, NombreUtil.lireDecimal("1.500,50")!!, 0.0001)
        assertEquals(1500.50, NombreUtil.lireDecimal("1,500.50")!!, 0.0001)
    }

    @Test
    fun `devise ecrite a la main toleree`() {
        assertEquals(8000.0, NombreUtil.lireDecimal("8000 Ar")!!, 0.0001)
        assertEquals(8000.0, NombreUtil.lireDecimal("8 000 ariary")!!, 0.0001)
        assertEquals(8000.0, NombreUtil.lireDecimal("8000MGA")!!, 0.0001)
    }

    @Test
    fun `une saisie qui n'est pas un nombre reste refusee`() {
        // Le point capital : on tolère la forme, jamais le contenu. Renvoyer 0 ici rendrait une
        // faute de frappe indiscernable d'un montant nul.
        assertNull(NombreUtil.lireDecimal("abc"))
        assertNull(NombreUtil.lireDecimal(""))
        assertNull(NombreUtil.lireDecimal("   "))
        assertNull(NombreUtil.lireDecimal(null))
        assertNull(NombreUtil.lireDecimal("12,,5"))
        assertNull(NombreUtil.lireDecimal("1-2"))
        assertNull(NombreUtil.lireDecimal("Ar"))
    }

    @Test
    fun `les entiers refusent une partie decimale, comme toIntOrNull avant eux`() {
        assertEquals(3, NombreUtil.lireEntier("3"))
        assertEquals(1500, NombreUtil.lireEntier("1 500"))
        assertNull(NombreUtil.lireEntier("3.7"))
        assertNull(NombreUtil.lireEntier("3,7"))
        assertNull(NombreUtil.lireEntier("abc"))
    }

    @Test
    fun `les extensions se comportent comme l'objet`() {
        assertEquals(12.5, "12,5".enDecimal()!!, 0.0001)
        assertEquals(1500, "1 500".enEntier())
        assertNull("abc".enDecimal())
        assertNull((null as String?).enDecimal())
    }
}
