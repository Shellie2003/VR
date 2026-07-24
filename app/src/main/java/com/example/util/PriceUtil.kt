package com.example.util

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
    fun isWholesaleActive(product: Product, shopMode: String): Boolean =
        shopMode == "wholesale" && (product.wholesalePrice ?: 0.0) > 0.0

    /** The selling price a cashier would actually charge right now for one unit. */
    fun displayPrice(product: Product, shopMode: String): Double =
        if (isWholesaleActive(product, shopMode)) product.wholesalePrice!! else product.price
}
