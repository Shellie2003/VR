package com.example.util

import com.example.data.model.Debt
import com.example.data.model.DebtorGroup

/**
 * Le regroupement des dettes par débiteur, tel que l'écran Dettes l'applique.
 *
 * **Pourquoi hors de l'écran.** La règle était écrite en ligne dans `DebtsScreen`, et l'écran
 * Dettes de bureau en aurait fait une deuxième copie. Or elle décide de **ce que le gérant lit
 * comme total dû par une personne** : deux variantes qui normaliseraient le nom différemment
 * sépareraient le même client en deux cartes sur un appareil et pas sur l'autre.
 *
 * **La correspondance porte sur le nom COMPLET normalisé, jamais sur un préfixe** (section 31), et
 * c'est une protection délibérée : regrouper « Rakoto » et « Rakotoarisoa » fondrait deux personnes
 * différentes en une seule dette, et le gérant réclamerait de l'argent au mauvais client.
 */
object RegroupementDettes {

    /** La clé de correspondance : nom complet, espaces de bord retirés, casse ignorée. */
    fun cle(nomDebiteur: String): String = nomDebiteur.trim().lowercase()

    /**
     * Regroupe les dettes par débiteur. Les dettes d'un groupe sont triées de la plus récente à la
     * plus ancienne, et les groupes entre eux par dette la plus récente — le gérant retrouve en
     * tête ce qu'il vient d'enregistrer.
     *
     * Le nom affiché est celui de la dette la plus récente du groupe : si l'orthographe a varié
     * d'une saisie à l'autre, c'est la dernière graphie employée qui s'affiche, et non une
     * version mise en minuscules qui ne ressemblerait à aucune saisie.
     */
    fun grouper(dettes: List<Debt>): List<DebtorGroup> =
        dettes.groupBy { cle(it.debtorName) }
            .map { (_, groupe) ->
                val triees = groupe.sortedByDescending { it.date }
                DebtorGroup(displayName = triees.first().debtorName, debts = triees)
            }
            .sortedByDescending { groupe -> groupe.debts.maxOf { it.date } }
}
