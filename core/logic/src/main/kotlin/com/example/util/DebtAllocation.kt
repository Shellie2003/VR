package com.example.util

import com.example.data.model.Debt

/**
 * Répartition automatique d'un remboursement groupé entre les dettes d'un même débiteur.
 *
 * Cas visé : un client a plusieurs trosa distincts accumulés à des dates différentes (ex. 2 300 Ar
 * et 1 100 Ar, soit 3 400 Ar au total) et rembourse une somme qui ne correspond à aucune section
 * précise (ex. 2 400 Ar). Sans cet outil, le gérant devait ouvrir chaque trosa un par un et deviner
 * comment répartir le montant reçu — source d'erreurs et de perte de temps au comptoir.
 *
 * Règle d'affectation : la dette au solde le plus élevé est soldée en priorité (c'est elle qui,
 * réglée, fait disparaître le plus vite un trosa ouvert) ; s'il reste de l'argent après l'avoir
 * couverte, le reliquat s'applique à la suivante par solde décroissant, et ainsi de suite jusqu'à
 * épuisement du montant reçu ou de la dette totale. À solde égal entre deux dettes, la plus
 * ancienne (date la plus faible) est réglée en premier — ordre déterministe, qui réduit en
 * priorité le trosa qui traîne depuis le plus longtemps.
 *
 * Volontairement une fonction PURE (aucune écriture, aucune dépendance Android) : elle sert à la
 * fois à calculer le plan à appliquer et à en donner un aperçu à l'écran avant confirmation, sans
 * risque que les deux divergent puisque c'est le même calcul.
 */
object DebtAllocation {

    /** Effet du remboursement sur une dette précise. */
    data class Ligne(
        val debt: Debt,
        val soldeAvant: Double,
        val montantApplique: Double,
        val soldeApres: Double
    ) {
        val estSoldee: Boolean get() = soldeApres <= 0.0
    }

    data class Plan(
        val lignes: List<Ligne>,
        /** Part du paiement qui dépasse la dette totale du groupe — jamais transformée en solde négatif. */
        val montantNonAffecte: Double
    ) {
        val montantTotalAffecte: Double get() = lignes.sumOf { it.montantApplique }
    }

    /**
     * Calcule la répartition, sans rien écrire. Les dettes déjà soldées (`balance <= 0`) sont
     * ignorées ; un montant nul ou négatif ne produit aucune ligne.
     */
    fun calculer(dettes: List<Debt>, montant: Double): Plan {
        if (montant <= 0.0) return Plan(emptyList(), 0.0)

        var restant = montant
        val lignes = mutableListOf<Ligne>()
        val ordre = dettes
            .filter { it.balance > 0.0 }
            .sortedWith(compareByDescending<Debt> { it.balance }.thenBy { it.date })

        for (dette in ordre) {
            if (restant <= 0.0) break
            val applique = minOf(restant, dette.balance)
            val soldeApres = (dette.balance - applique).coerceAtLeast(0.0)
            lignes += Ligne(dette, dette.balance, applique, soldeApres)
            restant -= applique
        }

        return Plan(lignes, restant.coerceAtLeast(0.0))
    }
}
