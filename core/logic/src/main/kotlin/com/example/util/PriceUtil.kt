package com.example.util

import com.example.data.model.CartItem
import com.example.data.model.Product

/**
 * Single source of truth for the price displayed on a product card, wherever that card is drawn
 * (écran d'accueil, écran stock, sélecteurs de produits...).
 *
 * Before this helper existed each screen decided on its own: HomeScreen honoured the "wholesale"
 * shop mode while InventoryListScreen always printed `product.price`, so the very same product
 * could show two different prices on two screens — especially right after a réapprovisionnement,
 * which rewrites the product's prices. Every card must go through [displayPrice] so the two
 * screens can never drift apart again.
 */
object PriceUtil {

    /** True when the wholesale (ambongadiny) price is the one actually charged for this product. */
    fun isWholesaleActive(product: Product, shopMode: ShopMode): Boolean =
        shopMode == ShopMode.GROSSISTE && (product.wholesalePrice ?: 0.0) > 0.0

    /** The selling price a cashier would actually charge right now for one unit. */
    fun displayPrice(product: Product, shopMode: ShopMode): Double =
        if (isWholesaleActive(product, shopMode)) product.wholesalePrice!! else product.price

    /**
     * Le prix actif d'une ligne de panier déjà constituée, quand le mode de boutique change en
     * cours de vente.
     *
     * Même règle que [displayPrice], sur l'autre type qui porte un prix de gros. Elle était
     * réécrite à la main dans `InventoryViewModel.updateShopMode` — une troisième copie de la règle
     * après celle du panier corrigée en section 79, et au même endroit sensible : une divergence
     * ne s'afficherait nulle part, elle changerait le montant encaissé.
     *
     * La ligne « divers » (pas de `productId`) garde son prix saisi à la main : elle n'a pas de
     * tarif de gros, et lui appliquer `regularPrice` écraserait le montant voulu par le vendeur.
     */
    fun prixActif(item: CartItem, shopMode: ShopMode): Double {
        if (item.productId == null) return item.price
        val gros = item.wholesalePrice ?: 0.0
        return if (shopMode == ShopMode.GROSSISTE && gros > 0.0) gros else item.regularPrice
    }
}
