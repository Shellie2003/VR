package com.example.util

import com.example.data.model.CartItem
import com.example.data.model.Product
import java.util.UUID

/**
 * Les opérations du panier de caisse : ajouter un produit, ajouter une ligne « divers », changer
 * une quantité, retirer une ligne.
 *
 * **Pourquoi hors du ViewModel.** Ces règles vivaient dans `InventoryViewModel`, donc dans un
 * module Android, et la caisse de bureau en avait besoin. La seule autre issue aurait été de les
 * réécrire là-bas — c'est-à-dire de recommencer exactement ce que les sections 89, 90 et 91 ont
 * passé leur temps à défaire, et sur le panier, là où un écart ne s'affiche pas mais change le
 * montant encaissé.
 *
 * **Listes immuables, et ce n'est pas un choix de style.** Compose ne recompose que sur changement
 * de référence : muter une liste en place laisserait l'écran afficher l'ancien panier. Chaque
 * fonction rend donc une nouvelle liste — et rend **la même référence** quand rien ne change, pour
 * qu'un geste sans effet ne provoque aucune recomposition.
 *
 * Les deux règles de quantité sont **volontairement différentes**, telles qu'elles l'étaient dans
 * le ViewModel : [fixerQuantite] borne à `0.01` au minimum (une saisie directe ne doit pas faire
 * disparaître la ligne sous le doigt), tandis que [deplacerQuantite] **retire** la ligne dès que le
 * total tombe à zéro (c'est le geste attendu quand on appuie sur `-` jusqu'au bout).
 */
object PanierCaisse {

    /** L'identifiant d'une ligne portant un produit du catalogue. */
    fun idLigneProduit(produitId: Int): String = "product_$produitId"

    /**
     * Ajoute `quantite` du produit, ou l'additionne à la ligne existante.
     *
     * La quantité est **plafonnée au stock** : la caisse ne doit pas composer un panier qu'elle ne
     * peut pas servir. Un produit en rupture (stock nul ou négatif) n'entre donc pas du tout, et le
     * panier est rendu inchangé — sans erreur, car refuser l'ajout est la réponse correcte.
     */
    fun ajouterProduit(
        panier: List<CartItem>,
        produit: Product,
        quantite: Double,
        modeBoutique: ShopMode
    ): List<CartItem> {
        val idLigne = idLigneProduit(produit.id)
        val prixActif = PriceUtil.displayPrice(produit, modeBoutique)
        val index = panier.indexOfFirst { it.id == idLigne }

        if (index != -1) {
            val nouvelleQuantite = (panier[index].quantity + quantite).coerceAtMost(produit.stock)
            if (nouvelleQuantite <= 0.0) return panier
            return panier.toMutableList().also {
                it[index] = it[index].copy(
                    quantity = nouvelleQuantite,
                    price = prixActif,
                    taxable = produit.taxable,
                    tauxTaxe = produit.tauxTaxe
                )
            }
        }

        val quantitePlafonnee = quantite.coerceAtMost(produit.stock)
        if (quantitePlafonnee <= 0.0) return panier
        return panier + CartItem(
            id = idLigne,
            name = produit.name,
            price = prixActif,
            quantity = quantitePlafonnee,
            unit = produit.unit,
            productId = produit.id,
            maxStock = produit.stock,
            regularPrice = produit.price,
            wholesalePrice = produit.wholesalePrice,
            taxable = produit.taxable,
            tauxTaxe = produit.tauxTaxe
        )
    }

    /**
     * Ajoute une ligne « divers » : une somme sans article identifiable au catalogue.
     *
     * Chaque appel crée une **nouvelle** ligne et n'en additionne jamais deux, volontairement :
     * deux « divers » de 500 Ar sont deux encaissements distincts, que le vendeur doit pouvoir
     * retirer séparément s'il s'est trompé sur l'un des deux.
     */
    fun ajouterDivers(
        panier: List<CartItem>,
        nom: String,
        prix: Double,
        quantite: Double
    ): List<CartItem> = panier + CartItem(
        id = "misc_${UUID.randomUUID()}",
        name = nom,
        price = prix,
        quantity = quantite,
        unit = "Pièce",
        productId = null
    )

    /**
     * Fixe la quantité d'une ligne (saisie directe). Bornée entre `0.01` et le stock disponible :
     * une saisie à zéro ne doit pas escamoter la ligne, et une saisie au-delà du stock composerait
     * un panier que la boutique ne peut pas servir.
     */
    fun fixerQuantite(panier: List<CartItem>, idLigne: String, quantite: Double): List<CartItem> {
        val index = panier.indexOfFirst { it.id == idLigne }
        if (index == -1) return panier
        val ligne = panier[index]
        return panier.toMutableList().also {
            it[index] = ligne.copy(quantity = quantite.coerceIn(0.01, ligne.maxStock))
        }
    }

    /**
     * Déplace la quantité d'une ligne de `delta`. **Retire la ligne** quand le total atteint zéro
     * ou passe en dessous : c'est le geste attendu quand on appuie sur `-` jusqu'au bout, et
     * laisser une ligne à zéro dans le panier serait du bruit que le vendeur devrait nettoyer.
     */
    fun deplacerQuantite(panier: List<CartItem>, idLigne: String, delta: Double): List<CartItem> {
        val index = panier.indexOfFirst { it.id == idLigne }
        if (index == -1) return panier
        val ligne = panier[index]
        val nouvelleQuantite = ligne.quantity + delta
        if (nouvelleQuantite <= 0.0) return panier.filterNot { it.id == idLigne }
        return panier.toMutableList().also {
            it[index] = ligne.copy(quantity = nouvelleQuantite.coerceAtMost(ligne.maxStock))
        }
    }

    /** Retire une ligne. Le panier est rendu tel quel si l'identifiant n'y figure pas. */
    fun retirer(panier: List<CartItem>, idLigne: String): List<CartItem> =
        if (panier.none { it.id == idLigne }) panier else panier.filterNot { it.id == idLigne }

    /** Retire la dernière ligne ajoutée (annulation du geste précédent). */
    fun annulerDernier(panier: List<CartItem>): List<CartItem> =
        if (panier.isEmpty()) panier else panier.dropLast(1)
}
