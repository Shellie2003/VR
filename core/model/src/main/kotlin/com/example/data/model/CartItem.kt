package com.example.data.model

/**
 * Une ligne du panier en cours d'encaissement.
 *
 * **Sortie de `InventoryViewModel` (module `:app`) pour la même raison qu'`EtagereAvecNiveaux`
 * (section 81) :** ce n'est pas un détail de ViewModel mais un porteur de données composé
 * exclusivement de types du langage, et la caisse de bureau en a besoin. Tant qu'il vivait dans un
 * module Android, aucun autre module ne pouvait le nommer — donc aucune caisse hors d'Android, et
 * aucun test de ses deux règles de calcul en dehors d'un harnais Android.
 *
 * `price` est TOUJOURS un prix TTC : c'est ce que le client paie, et ce montant ne change jamais.
 * `taxable`/`tauxTaxe` ne servent qu'à isoler la part de TVA déjà comprise dedans, pour le reçu et
 * la comptabilité (section 20). Inverser ce principe modifierait le total payé par le client.
 */
data class CartItem(
    val id: String, // "product_<id>" ou "misc_<uuid>"
    val name: String,
    val price: Double, // prix actif (détail ou gros, selon le mode de boutique)
    val quantity: Double,
    val unit: String,
    val productId: Int? = null, // null pour une ligne « divers »
    val maxStock: Double = 99999.0,
    val regularPrice: Double = price,
    val wholesalePrice: Double? = null,
    val taxable: Boolean = false,
    val tauxTaxe: Double = 0.0
) {
    val totalPrice: Double get() = price * quantity

    /** Part de TVA contenue dans `totalPrice` (0 quand la ligne n'est pas taxable). */
    val taxAmount: Double get() =
        if (taxable && tauxTaxe > 0.0) totalPrice * tauxTaxe / (100.0 + tauxTaxe) else 0.0
}
