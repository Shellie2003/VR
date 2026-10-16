package com.example.util

import java.security.SecureRandom

/**
 * Code de récupération de la sauvegarde : l'adresse, sur Firebase, du coffre d'une boutique.
 *
 * Contraintes qui ont dicté le format :
 *  - **recopiable à la main** par un épicier, éventuellement dicté au téléphone. D'où un alphabet
 *    sans les caractères qu'on confond en écrivant (I/1, O/0, L, U/V) et des groupes de 4 ;
 *  - **impossible à deviner**, puisque les règles Firebase autorisent la lecture de n'importe quel
 *    chemin `backups/<code>` : 12 caractères sur un alphabet de 32 = 60 bits d'entropie, soit
 *    ~10^18 combinaisons. Un balayage exhaustif est hors de portée ;
 *  - **tolérant à la saisie** : minuscules, espaces, tirets manquants ou en trop, préfixe oublié,
 *    et les confusions classiques (O→0, I→1) sont rattrapées silencieusement.
 */
object RecoveryCode {

    /** Alphabet Crockford base32 : ni I, ni L, ni O, ni U — les quatre pièges de la dictée. */
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private const val LENGTH = 12
    private const val PREFIX = "VRT"

    private val random = SecureRandom()

    /** Nouveau code aléatoire, sous sa forme normalisée (12 caractères, sans tirets). */
    fun generate(): String = buildString {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** Forme lisible affichée à l'écran et communiquée au fournisseur : VRT-A3F9-K2M7-QP4X. */
    fun format(code: String): String {
        val normalise = normalize(code) ?: return code
        return PREFIX + "-" + normalise.chunked(4).joinToString("-")
    }

    /**
     * Jetons HISTORIQUES : avant l'introduction du code de récupération, le chemin de sauvegarde
     * était un UUID (36 caractères, minuscules, avec tirets) jamais affiché. Les boutiques
     * activées à cette époque ont toujours leur sauvegarde sous ce chemin — et l'écran Paramètres
     * leur affiche désormais ce UUID comme « code de récupération ». Il faut donc pouvoir le
     * RE-SAISIR sur un nouveau téléphone, exactement sous sa forme stockée (minuscules, tirets
     * aux positions 8-4-4-4-12), sinon ces clients-là seraient les seuls à ne pas pouvoir
     * récupérer leurs données.
     */
    fun normalizeLegacyUuid(saisie: String): String? {
        val hex = saisie.trim().lowercase().replace("-", "").replace(" ", "")
        if (hex.length != 32 || hex.any { it !in "0123456789abcdef" }) return null
        return buildString {
            append(hex, 0, 8); append('-')
            append(hex, 8, 12); append('-')
            append(hex, 12, 16); append('-')
            append(hex, 16, 20); append('-')
            append(hex, 20, 32)
        }
    }

    /**
     * Ramène une saisie utilisateur à la forme canonique, ou null si elle ne peut pas être un code
     * valide. Un null doit **toujours** être traité comme un refus : accepter une saisie douteuse
     * reviendrait à pointer la boutique vers un coffre vide et à perdre l'accès au vrai.
     */
    fun normalize(saisie: String): String? {
        val nettoye = saisie.trim()
            .uppercase()
            .removePrefix(PREFIX)
            .replace("-", "")
            .replace(" ", "")
            // Confusions de dictée et de recopie : ces lettres ne sont pas dans l'alphabet, donc
            // les remplacer ne peut jamais transformer un code valide en un autre code valide.
            .replace('O', '0')
            .replace('I', '1')
            .replace('L', '1')
            .replace('U', 'V')

        if (nettoye.length != LENGTH) return null
        if (nettoye.any { it !in ALPHABET }) return null
        return nettoye
    }
}
