package com.example.util

import com.example.data.model.Restock
import com.example.data.model.Sale

/**
 * Le filtrage de l'historique : une journée, puis une recherche.
 *
 * ## Pourquoi hors de l'écran
 *
 * Ces quatre règles étaient écrites dans `SalesHistoryScreen`, et le port de cet écran sur le
 * bureau en aurait fait une seconde copie. Ce n'est pas anodin : c'est ce filtrage qui décide de
 * **ce que le gérant compte comme la recette du jour**. Deux variantes — l'une qui cherche dans le
 * nom des articles, l'autre qui s'arrête au premier article — lui montreraient deux journées
 * différentes selon l'appareil, sans qu'aucune erreur n'apparaisse.
 *
 * Les bornes de la journée viennent de [RapportFinancier.bornesDuJour], qui est déjà la seule
 * définition de « un jour » du projet.
 */
object FiltreHistorique {

    /** Les ventes de la journée civile qui contient [instantDuJour]. */
    fun ventesDuJour(ventes: List<Sale>, instantDuJour: Long): List<Sale> {
        val journee = RapportFinancier.bornesDuJour(instantDuJour)
        return ventes.filter { journee.contient(it.timestamp) }
    }

    /** Les réapprovisionnements de la journée civile qui contient [instantDuJour]. */
    fun reapprosDuJour(reappros: List<Restock>, instantDuJour: Long): List<Restock> {
        val journee = RapportFinancier.bornesDuJour(instantDuJour)
        return reappros.filter { journee.contient(it.timestamp) }
    }

    /**
     * Les ventes dont **au moins un article** correspond à la recherche.
     *
     * `any` et non le premier article : une vente de dix articles dont le neuvième est celui qu'on
     * cherche doit remonter. Ne regarder que le premier ferait disparaître la vente du jour où le
     * gérant veut justement retrouver à qui il a vendu cet article-là.
     */
    fun chercherVentes(ventes: List<Sale>, recherche: String): List<Sale> {
        val terme = recherche.trim()
        if (terme.isEmpty()) return ventes
        return ventes.filter { vente ->
            vente.items.any { it.name.contains(terme, ignoreCase = true) }
        }
    }

    /**
     * Les réapprovisionnements dont le produit **ou le fournisseur** correspond.
     *
     * Le fournisseur compte autant que le produit : « qu'est-ce que j'ai pris chez untel ? » est la
     * question que le gérant pose à cet onglet.
     */
    fun chercherReappros(reappros: List<Restock>, recherche: String): List<Restock> {
        val terme = recherche.trim()
        if (terme.isEmpty()) return reappros
        return reappros.filter { reappro ->
            reappro.productName.contains(terme, ignoreCase = true) ||
                reappro.supplierName?.contains(terme, ignoreCase = true) == true
        }
    }

    /**
     * La variation de recette par rapport à la veille, en pourcentage, ou `null` quand elle n'a
     * pas de sens.
     *
     * **Pourquoi cette fonction existe : l'écran Android affichait « +12% omaly » EN DUR.** Une
     * valeur inventée, la même chaque jour, posée juste sous la recette réelle et donc lue comme
     * une mesure. Un gérant qui décide de ses prix sur « +12 % par rapport à hier » décide sur
     * rien. Le chiffre est désormais calculé, des deux côtés.
     *
     * `null` quand la veille n'a RIEN encaissé : une progression depuis zéro n'est pas « +100 % »,
     * elle n'est pas exprimable en pourcentage. L'écran affiche alors la recette seule, ce qui est
     * exactement ce qu'on sait.
     */
    fun variationVeille(ventes: List<Sale>, instantDuJour: Long): Double? {
        val recetteDuJour = ventesDuJour(ventes, instantDuJour).sumOf { it.totalAmount }
        val veille = RapportFinancier.bornesDuJour(instantDuJour).debut - 1L
        val recetteVeille = ventesDuJour(ventes, veille).sumOf { it.totalAmount }
        if (recetteVeille <= 0.0) return null
        return (recetteDuJour - recetteVeille) / recetteVeille * 100.0
    }

    /**
     * Le coût unitaire d'un réapprovisionnement, ou `null` quand il n'a aucune unité.
     *
     * La carte Android divise `totalCostPrice / totalUnits` sans garde, ce qui affiche « Ar ∞ » sur
     * un réappro saisi à zéro unité. `null` laisse l'écran afficher un tiret, ce qui est la vérité :
     * on ne connaît pas le coût unitaire d'une livraison dont on n'a pas compté les pièces.
     */
    fun coutUnitaireReappro(reappro: Restock): Double? =
        if (reappro.totalUnits > 0.0) reappro.totalCostPrice / reappro.totalUnits else null
}
