package com.example.util

/**
 * Lecture tolérante des nombres saisis par le gérant.
 *
 * **Pourquoi.** L'application lisait les saisies avec `toDoubleOrNull()`, qui n'accepte que le
 * point décimal et aucun séparateur de milliers. Sur les 52 endroits où une saisie était lue, deux
 * seulement rattrapaient la virgule. Partout ailleurs, un gérant tapant `12,5` — ce que produit
 * naturellement un clavier français ou malgache — obtenait `null`, aussitôt transformé en `0.0`
 * par le `?: 0.0` qui suivait.
 *
 * Ce n'est pas un plantage, c'est pire : le montant devient **zéro sans rien dire**. Sur le champ
 * « argent reçu », la monnaie rendue est fausse et personne ne s'en aperçoit avant le comptage.
 *
 * Ce qui est accepté : virgule ou point comme séparateur décimal, espaces de milliers (y compris
 * les espaces insécables que produit un copier-coller), devise en suffixe (`Ar`, `Ariary`, `MGA`),
 * signe. Ce qui est refusé reste refusé : une saisie qui n'est pas un nombre renvoie `null`, jamais
 * une valeur approchée.
 */
object NombreUtil {

    /** Espaces ordinaires, insécables (U+00A0) et fins insécables (U+202F, séparateur de milliers). */
    private val ESPACES = Regex("[\\s\\u00A0\\u202F]")

    /** Devise écrite à la main derrière le montant. */
    private val DEVISE = Regex("(ariary|mga|ar)$", RegexOption.IGNORE_CASE)

    private val NOMBRE_VALIDE = Regex("^[+-]?\\d*\\.?\\d+$")

    /**
     * Nombre décimal, ou null si la saisie n'en est pas un.
     *
     * Quand les deux séparateurs sont présents (`1.500,50` ou `1,500.50`), **le dernier est le
     * séparateur décimal** et l'autre marque les milliers : c'est vrai des deux conventions, et ça
     * évite d'avoir à deviner la langue du clavier. Un point seul reste décimal, comme avant — le
     * changer ferait basculer le sens des saisies déjà enregistrées par les versions précédentes.
     */
    fun lireDecimal(saisie: String?): Double? {
        if (saisie.isNullOrBlank()) return null
        var texte = saisie.trim().replace(ESPACES, "")
        texte = DEVISE.replace(texte, "").trim()
        if (texte.isEmpty()) return null

        val dernierPoint = texte.lastIndexOf('.')
        val derniereVirgule = texte.lastIndexOf(',')
        texte = when {
            dernierPoint >= 0 && derniereVirgule >= 0 -> {
                if (derniereVirgule > dernierPoint) {
                    texte.replace(".", "").replace(',', '.')
                } else {
                    texte.replace(",", "")
                }
            }
            derniereVirgule >= 0 -> texte.replace(',', '.')
            else -> texte
        }

        if (!NOMBRE_VALIDE.matches(texte)) return null
        return texte.toDoubleOrNull()
    }

    /**
     * Entier, ou null. Une saisie décimale est refusée comme avant (`toIntOrNull` renvoyait déjà
     * null sur `3.7`) : accepter en tronquant ferait disparaître une partie d'une quantité sans
     * que personne ne le voie.
     */
    fun lireEntier(saisie: String?): Int? {
        val valeur = lireDecimal(saisie) ?: return null
        if (valeur % 1.0 != 0.0) return null
        if (valeur > Int.MAX_VALUE.toDouble() || valeur < Int.MIN_VALUE.toDouble()) return null
        return valeur.toInt()
    }
}

/** Lecture tolérante d'une saisie décimale. Voir [NombreUtil.lireDecimal]. */
fun String?.enDecimal(): Double? = NombreUtil.lireDecimal(this)

/** Lecture tolérante d'une saisie entière. Voir [NombreUtil.lireEntier]. */
fun String?.enEntier(): Int? = NombreUtil.lireEntier(this)
