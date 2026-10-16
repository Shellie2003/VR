package com.example

import com.example.util.OfflineActivationCode
import com.example.util.RecoveryCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verrouille l'algorithme du code d'activation hors-ligne sur des vecteurs de référence.
 *
 * Le fournisseur calcule les codes de son côté avec la one-liner python documentée dans
 * OfflineActivationCode et le guide de gestion. Si l'implémentation Kotlin dérive (changement de
 * troncature, d'encodage, de secret), les codes qu'il dicte au téléphone cessent de fonctionner
 * pour tous les nouveaux clients — silencieusement. Ces valeurs ont été produites par la
 * one-liner elle-même : tout écart fait échouer le test AVANT d'atteindre une boutique.
 */
class OfflineActivationCodeTest {

    @Test
    fun `codes identiques a la commande python du guide`() {
        assertEquals("64136619", OfflineActivationCode.compute("482913"))
        assertEquals("95803086", OfflineActivationCode.compute("123456"))
        assertEquals("71591786", OfflineActivationCode.compute("000001"))
    }

    @Test
    fun `saisie avec espaces accidentels acceptee`() {
        assertTrue(OfflineActivationCode.matches("482913", " 64136619 "))
    }

    @Test
    fun `mauvais code refuse`() {
        assertFalse(OfflineActivationCode.matches("482913", "00000000"))
        // L'ancienne formule affine ne doit plus rien activer.
        assertFalse(OfflineActivationCode.matches("482913", String.format("%06d", (482913L * 3 + 123456) % 1000000)))
    }
}

/** Même logique de non-régression pour le code de récupération des sauvegardes. */
class RecoveryCodeTest {

    @Test
    fun `normalisation tolère tirets, minuscules et confusions de dictée`() {
        val canonique = RecoveryCode.normalize("VRT-A3F9-K2M7-QP4X")
        assertEquals(canonique, RecoveryCode.normalize("a3f9 k2m7 qp4x"))
        // O→0 et I→1 : lettres hors alphabet, remplacées sans risque de collision.
        assertEquals(RecoveryCode.normalize("VRT-A3F0-K2M1-QP4X"), RecoveryCode.normalize("VRT-A3FO-K2MI-QP4X"))
    }

    @Test
    fun `saisie invalide refusee plutot qu'ecrasee`() {
        assertNull(RecoveryCode.normalize("TROP-COURT"))
        assertNull(RecoveryCode.normalize(""))
        assertNull(RecoveryCode.normalize("VRT-A3F9-K2M7-QP4X-EXTRA"))
    }

    @Test
    fun `generation puis format puis normalisation est stable`() {
        repeat(20) {
            val code = RecoveryCode.generate()
            assertEquals(code, RecoveryCode.normalize(RecoveryCode.format(code)))
        }
    }

    @Test
    fun `uuid historique re-saisi retrouve exactement sa forme stockee`() {
        // Les boutiques activées avant le code de récupération ont leur sauvegarde sous un UUID :
        // sa re-saisie doit reconstruire EXACTEMENT la chaîne stockée (minuscules, tirets aux
        // positions 8-4-4-4-12), sinon le chemin Firebase visé serait un coffre vide.
        val stocke = "550e8400-e29b-41d4-a716-446655440000"
        assertEquals(stocke, RecoveryCode.normalizeLegacyUuid(stocke))
        assertEquals(stocke, RecoveryCode.normalizeLegacyUuid("550E8400-E29B-41D4-A716-446655440000"))
        assertEquals(stocke, RecoveryCode.normalizeLegacyUuid("550e8400e29b41d4a716446655440000"))
        assertEquals(stocke, RecoveryCode.normalizeLegacyUuid(" 550e8400 e29b 41d4 a716 446655440000 "))
    }

    @Test
    fun `uuid invalide refuse`() {
        assertNull(RecoveryCode.normalizeLegacyUuid("pas-un-uuid"))
        assertNull(RecoveryCode.normalizeLegacyUuid("550e8400-e29b-41d4-a716-44665544000"))   // 31 hex
        assertNull(RecoveryCode.normalizeLegacyUuid("550e8400-e29b-41d4-a716-4466554400zz")) // non-hex
        // Un code moderne (12 caractères) ne doit jamais être pris pour un UUID.
        assertNull(RecoveryCode.normalizeLegacyUuid(RecoveryCode.generate()))
    }
}
