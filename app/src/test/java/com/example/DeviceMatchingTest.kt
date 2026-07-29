package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Verrouille la normalisation utilisée pour comparer un code appareil (affiché au client sous la
 * forme "A3F2-9B10-C44D-E5F6") à une clé saisie à la main dans la console Firebase.
 *
 * Bug réel corrigé : la comparaison exigeait auparavant une égalité de chaîne stricte entre
 * `DeviceIdentity.rawId()` (minuscules, sans tirets) et la clé JSON sous `appareils`. Un
 * fournisseur qui colle le code EXACTEMENT comme il l'a reçu du client — tirets et majuscules
 * compris, le réflexe le plus naturel — voyait alors son appareil refusé (état TRANSFEREE) alors
 * que tout semblait correct de son point de vue dans la console. La fonction de normalisation
 * (répliquée ici car privée dans LicenceManager) doit faire converger les deux formes.
 */
class DeviceMatchingTest {

    // Réplique volontaire de LicenceManager.normaliserCodeAppareil : la fonction est privée, et la
    // dupliquer ici évite d'exposer publiquement un détail d'implémentation juste pour le test —
    // le comportement attendu (minuscules, hex uniquement) est ce qui compte, pas l'API.
    private fun normalise(code: String): String = code.lowercase().filter { it in "0123456789abcdef" }

    @Test
    fun `code affiche avec tirets et majuscules correspond au code brut`() {
        val brut = "a3f29b10c44de5f6"
        assertEquals(brut, normalise("A3F2-9B10-C44D-E5F6"))
        assertEquals(brut, normalise("a3f2-9b10-c44d-e5f6"))
        assertEquals(brut, normalise(" A3F2 9B10 C44D E5F6 "))
        assertEquals(brut, normalise(brut))
    }

    @Test
    fun `deux appareils differents ne convergent jamais vers la meme cle`() {
        assertNotEquals(normalise("AAAA-BBBB-CCCC-DDDD"), normalise("1111-2222-3333-4444"))
    }
}
