package com.example.util

import com.example.data.model.LotProduit

/**
 * Quantité prélevée sur un lot précis pour satisfaire une ligne de vente.
 */
data class AllocationLot(
    val lotId: Long,
    val quantite: Double
)

/**
 * Répartition d'une quantité vendue sur les lots, du plus proche de la péremption au plus lointain
 * (FEFO — *First Expired, First Out*).
 *
 * **Pourquoi FEFO et pas FIFO.** Sur des produits datés, ce n'est pas l'ordre d'ARRIVÉE qui compte
 * mais l'ordre de PÉREMPTION : un lot reçu hier peut expirer avant un lot reçu le mois dernier.
 * Écouler d'abord le plus proche de la date limite est exactement ce qui évite de jeter.
 *
 * **Pourquoi une fonction pure, séparée du dépôt.** C'est la seule partie réellement délicate du
 * lien ventes/lots (quantités partielles, lots insuffisants, arrondis), et elle est ici testable
 * sans base de données ni coroutine — même principe que `EtagereColors.normaliser` et
 * `UpdateManager.parserManifesteMiseAJour`.
 *
 * **Le cas « pas de lots » n'est pas une erreur.** L'immense majorité des épiceries ne saisit aucun
 * lot : la fonction renvoie alors une liste vide, et l'appelant décrémente le stock comme il l'a
 * toujours fait. C'est ce qui permet d'ajouter la traçabilité sans rien changer pour elles.
 */
object FefoAllocator {

    /**
     * Tolérance sur les comparaisons de quantités. Les quantités sont des `Double` et peuvent venir
     * de saisies décimales (0,25 kg, 0,5 L) : sans marge, un reliquat de 1e-15 ferait générer une
     * allocation fantôme d'une quantité invisible.
     */
    private const val EPSILON = 1e-9

    /**
     * @param lots lots disponibles pour le produit vendu, dans n'importe quel ordre.
     * @param quantiteDemandee quantité vendue, dans la même unité que [LotProduit.quantite].
     * @return les prélèvements à effectuer, du lot le plus proche de la péremption au plus lointain.
     *
     * Si les lots ne couvrent pas toute la quantité, on alloue ce qui existe et on s'arrête : le
     * reliquat correspond à du stock saisi sans lot, et il n'est pas question de refuser une vente
     * pour ça — la caisse doit passer, la traçabilité est un plus, pas un verrou.
     */
    fun repartir(lots: List<LotProduit>, quantiteDemandee: Double): List<AllocationLot> {
        if (quantiteDemandee <= EPSILON) return emptyList()

        var restant = quantiteDemandee
        val allocations = mutableListOf<AllocationLot>()

        // `sortedBy` est stable : à date de péremption égale, l'ordre d'origine (donc l'ordre
        // d'insertion renvoyé par la requête) est conservé, ce qui rend le résultat déterministe.
        for (lot in lots.sortedBy { it.datePeremption }) {
            if (restant <= EPSILON) break
            if (lot.quantite <= EPSILON) continue

            val prelevement = minOf(lot.quantite, restant)
            allocations += AllocationLot(lotId = lot.id, quantite = prelevement)
            restant -= prelevement
        }

        return allocations
    }

    /**
     * Quantité qu'aucun lot n'a pu couvrir — utile pour signaler au gérant que son stock de lots
     * est en retard sur son stock réel, sans bloquer la vente.
     */
    fun quantiteNonCouverte(lots: List<LotProduit>, quantiteDemandee: Double): Double {
        val alloue = repartir(lots, quantiteDemandee).sumOf { it.quantite }
        return (quantiteDemandee - alloue).coerceAtLeast(0.0)
    }
}
