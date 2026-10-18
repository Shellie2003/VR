package com.example.data.model

/**
 * Les dettes d'un même débiteur, regroupées pour l'affichage.
 *
 * **Sorti de `DebtsScreen` (module `:app`) pour la même raison que `CartItem` (section 90) et
 * `EtagereAvecNiveaux` (section 81) :** c'est un porteur de données composé d'un seul type de
 * `:core:model`, et l'écran Dettes de bureau en a besoin.
 *
 * **Rien n'est fusionné en base.** Chaque ligne `Debt` garde sa date, son montant et sa note, et
 * reste un enregistrement indépendant — le regroupement n'existe qu'à l'affichage (section 31).
 * C'est ce qui permet de déplier le détail transaction par transaction, et c'est aussi ce qui fait
 * que l'export et la recherche continuent d'opérer à plat sans être affectés.
 */
data class DebtorGroup(
    val displayName: String,
    val debts: List<Debt>
) {
    val totalBalance: Double get() = debts.sumOf { it.balance }
    val totalAmount: Double get() = debts.sumOf { it.amount }
    val isFullyPaid: Boolean get() = debts.all { it.isPaid }
    val hasOverdue: Boolean get() = debts.any { it.isOverdue() }
}
