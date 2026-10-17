package com.example

import com.example.data.model.CartItem
import com.example.util.Monnaie
import com.example.util.PriceUtil
import com.example.util.ShopMode
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `PriceUtil.prixActif` remplace la règle du prix de gros qui était réécrite à la main dans
 * `InventoryViewModel.updateShopMode` — la troisième copie de cette règle, après celle du panier
 * corrigée en section 79. Elle décide du montant encaissé : un écart ne s'afficherait nulle part.
 */
class PrixActifPanierTest {

    private fun ligne(
        prix: Double,
        prixDetail: Double = prix,
        gros: Double? = null,
        produitId: Int? = 1
    ) = CartItem(
        id = if (produitId == null) "misc_1" else "product_$produitId",
        name = "Article",
        price = prix,
        quantity = 1.0,
        unit = "Pièce",
        productId = produitId,
        regularPrice = prixDetail,
        wholesalePrice = gros
    )

    @Test
    fun `le mode grossiste applique le prix de gros`() {
        val item = ligne(prix = 3500.0, prixDetail = 3500.0, gros = 3100.0)
        assertEquals(3100.0, PriceUtil.prixActif(item, ShopMode.GROSSISTE), 1e-9)
    }

    @Test
    fun `le mode detail revient au prix de detail et non au prix courant de la ligne`() {
        // La ligne est déjà au prix de gros (le mode vient de changer) : repasser en détail doit
        // restaurer `regularPrice`, pas conserver le prix actuellement porté par la ligne.
        val item = ligne(prix = 3100.0, prixDetail = 3500.0, gros = 3100.0)
        assertEquals(3500.0, PriceUtil.prixActif(item, ShopMode.DETAIL), 1e-9)
    }

    @Test
    fun `un prix de gros absent ou nul ne remplace jamais le prix de detail`() {
        // Même seuil que `isWholesaleActive` (> 0.0) : un tarif de gros à zéro ferait vendre
        // gratuitement en mode grossiste.
        assertEquals(3500.0, PriceUtil.prixActif(ligne(3500.0, 3500.0, null), ShopMode.GROSSISTE), 1e-9)
        assertEquals(3500.0, PriceUtil.prixActif(ligne(3500.0, 3500.0, 0.0), ShopMode.GROSSISTE), 1e-9)
    }

    @Test
    fun `la pharmacie et le bar n'heritent pas du tarif de gros`() {
        val item = ligne(prix = 3500.0, prixDetail = 3500.0, gros = 3100.0)
        assertEquals(3500.0, PriceUtil.prixActif(item, ShopMode.PHARMACIE), 1e-9)
        assertEquals(3500.0, PriceUtil.prixActif(item, ShopMode.BAR), 1e-9)
    }

    @Test
    fun `une ligne divers garde le montant saisi a la main dans tous les modes`() {
        // Pas de produit, donc pas de tarif de gros : lui appliquer `regularPrice` écraserait le
        // montant voulu par le vendeur, qui est la seule information dont on dispose.
        val divers = ligne(prix = 1234.0, prixDetail = 9999.0, gros = 500.0, produitId = null)
        ShopMode.entries.forEach { mode ->
            assertEquals(
                "Mode $mode",
                1234.0,
                PriceUtil.prixActif(divers, mode),
                1e-9
            )
        }
    }

    @Test
    fun `changer de mode change le total encaisse et c'est bien le total qui en depend`() {
        // Le lien entre les deux règles, puisque c'est lui qui compte devant le client.
        val panier = listOf(ligne(prix = 3500.0, prixDetail = 3500.0, gros = 3100.0))
        val enGros = panier.map { it.copy(price = PriceUtil.prixActif(it, ShopMode.GROSSISTE)) }
        assertEquals(3100.0, Monnaie.total(enGros), 1e-9)
    }
}
