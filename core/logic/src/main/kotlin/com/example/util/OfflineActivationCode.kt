package com.example.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Code d'activation HORS-LIGNE : le chemin de secours quand une boutique n'a aucun réseau le jour
 * de l'installation. Le client dicte son ID d'installation au fournisseur, qui calcule le code et
 * le lui dicte en retour.
 *
 * F3 — L'ancienne formule était `(id × 3 + 123456) mod 10^6` : calculable de tête par quiconque
 * l'avait lue une fois, et l'ID est affiché en clair sur l'écran de verrouillage. Le jour où la
 * formule fuitait (un post Facebook suffisait), toutes les installations du pays s'activaient
 * gratuitement. Désormais : les 8 premiers chiffres d'un HMAC-SHA256 à secret. Sans le secret, la
 * connaissance d'un couple (ID, code) — ou de mille — ne permet pas d'en déduire un autre.
 *
 * Limite assumée : le secret vit dans l'APK, un rétro-ingénieur peut l'extraire. C'est le plafond
 * indépassable de toute activation hors-ligne — le but est d'arrêter la fraude « bouche à
 * oreille », pas l'attaquant outillé ; contre lui, seule la vérification en ligne (liste
 * `appareils` de la licence) fait autorité, et elle reprend la main dès que l'appareil retrouve
 * du réseau.
 *
 * Calcul côté fournisseur (documenté aussi dans le guide de gestion) :
 * ```
 * python3 -c "import hmac,hashlib;print('%08d'%(int(hmac.new(b'<SECRET>',b'<ID>',hashlib.sha256).hexdigest()[:8],16)%10**8))"
 * ```
 *
 * ⚠ Changer [SECRET] invalide tous les codes non encore saisis (les appareils déjà activés
 * restent activés). Ne le faire qu'en cas de fuite avérée, et mettre à jour le guide.
 */
object OfflineActivationCode {

    private const val SECRET = "105784084d4164cd4cd21f86aa6e68891fd233c4609d64cf"

    /** Code attendu pour un ID d'installation donné : 8 chiffres, zéros de tête compris. */
    fun compute(installationId: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(installationId.trim().toByteArray(Charsets.UTF_8))
        // Les 4 premiers octets lus en entier non signé big-endian == les 8 premiers hex du
        // digest : c'est ce qui permet la one-liner python ci-dessus côté fournisseur.
        val v = ((digest[0].toLong() and 0xFF) shl 24) or
            ((digest[1].toLong() and 0xFF) shl 16) or
            ((digest[2].toLong() and 0xFF) shl 8) or
            (digest[3].toLong() and 0xFF)
        return String.format("%08d", v % 100_000_000L)
    }

    fun matches(installationId: String, saisie: String): Boolean =
        saisie.trim() == compute(installationId)
}
