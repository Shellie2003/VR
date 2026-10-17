package com.example.util

import com.example.data.model.CartItem

/**
 * Le total à payer, la monnaie à rendre, et le libellé que le vendeur lit avant de rendre
 * l'argent.
 *
 * **Pourquoi c'est ici et non dans l'écran de caisse.** Le bloc « argent reçu / rendu » est écrit
 * **trois fois** dans `CalculatorScreen` (panneau tablette, onglet téléphone, onglet « divers
 * rapide »), et deux de ces copies portaient à l'identique le même calcul de suffisance et les
 * mêmes six chaînes de libellé. C'est la configuration exacte qui a produit les défauts des
 * sections 89 (le pas d'ajustement écrit deux fois) et 79 (le prix de gros recalculé à la main
 * dans le panier) : une copie modifiée, l'autre oubliée, et rien à l'écran pour le signaler.
 *
 * Ici l'enjeu est le pire possible : **c'est l'argent rendu au client**. Un écart ne provoque
 * aucune erreur, aucun plantage — il fait rendre la mauvaise somme, et le manque n'apparaît qu'au
 * comptage du soir, sans qu'on puisse dire quelle vente l'a causé.
 */
object Monnaie {

    /** Le total à encaisser pour un panier. */
    fun total(panier: List<CartItem>): Double = panier.sumOf { it.totalPrice }

    /** La part de TVA comprise dans ce total (section 20 : les prix sont toujours TTC). */
    fun totalTaxe(panier: List<CartItem>): Double = panier.sumOf { it.taxAmount }

    /**
     * Ce qu'il faut rendre. **Négatif quand le client n'a pas assez donné** — la valeur signée est
     * conservée volontairement : c'est elle qui distingue les deux cas, et la remplacer par une
     * valeur absolue ferait afficher « rendu » sur un paiement insuffisant.
     */
    fun rendu(total: Double, recu: Double): Double = recu - total

    /** `true` quand la somme reçue couvre le total. Un paiement exact est suffisant. */
    fun suffisant(total: Double, recu: Double): Boolean = rendu(total, recu) >= 0.0

    /**
     * Le libellé compact affiché à côté du champ « argent reçu », tel qu'il est écrit dans l'écran
     * de caisse Android : « Rendu: Ar 2 500 » ou « Manque Ar 500 ».
     *
     * Le montant est toujours présenté en valeur absolue : afficher « Manque Ar -500 » ferait
     * hésiter le vendeur au moment précis où il doit agir vite.
     */
    fun libelleRendu(total: Double, recu: Double, langue: String): String {
        val rendu = rendu(total, recu)
        return if (rendu >= 0.0) {
            val montant = FormatUtil.formatPrice(rendu)
            when (langue) {
                "mg" -> "Famerenana: Ar $montant"
                "fr" -> "Rendu: Ar $montant"
                else -> "Change: Ar $montant"
            }
        } else {
            val montant = FormatUtil.formatPrice(-rendu)
            when (langue) {
                "mg" -> "Tsy ampy Ar $montant"
                "fr" -> "Manque Ar $montant"
                else -> "Short Ar $montant"
            }
        }
    }

    /**
     * Les coupures proposées en saisie rapide sous le champ « argent reçu ».
     *
     * Ce sont les billets réellement en circulation à Madagascar parmi ceux qu'un client tend pour
     * une course ordinaire. Figés ici plutôt que recopiés dans chaque mise en page de la caisse,
     * où ils l'étaient déjà trois fois.
     */
    val coupuresRapides: List<Int> = listOf(1000, 2000, 5000, 10000, 20000)
}
