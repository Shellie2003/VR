package com.example

import com.example.data.model.Product
import com.example.util.PriceUtil
import com.example.util.ShopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le métier de la boutique est désormais une énumération et non plus une chaîne comparée à des
 * littéraux recopiés dans quatre fichiers.
 *
 * Ce que ces tests protègent avant tout, c'est la **compatibilité des appareils déjà en service** :
 * les clés persistées des deux modes historiques ne doivent JAMAIS changer, sans quoi la mise à
 * jour ferait basculer silencieusement une épicerie de gros en vente au détail — donc lui ferait
 * facturer les mauvais prix.
 */
class ShopModeTest {

    @Test
    fun `les cles persistees des modes historiques ne changent pas`() {
        // Ces deux chaînes sont déjà écrites dans les préférences des appareils en service.
        // Les modifier reviendrait à réinitialiser le réglage de toutes les boutiques existantes.
        assertEquals("retail", ShopMode.DETAIL.cle)
        assertEquals("wholesale", ShopMode.GROSSISTE.cle)
    }

    @Test
    fun `une preference existante est relue sur le bon mode`() {
        assertEquals(ShopMode.DETAIL, ShopMode.depuisCle("retail"))
        assertEquals(ShopMode.GROSSISTE, ShopMode.depuisCle("wholesale"))
    }

    @Test
    fun `les nouveaux metiers ont leur propre cle, distincte des autres`() {
        assertEquals(ShopMode.PHARMACIE, ShopMode.depuisCle("pharmacie"))
        assertEquals(ShopMode.BAR, ShopMode.depuisCle("bar"))

        val cles = ShopMode.entries.map { it.cle }
        assertEquals("Chaque mode doit avoir une clé unique", cles.size, cles.distinct().size)
    }

    @Test
    fun `une valeur inconnue ou absente retombe sur le mode par defaut plutot que d'echouer`() {
        // Préférence corrompue, sauvegarde restaurée depuis une version plus récente, appareil
        // rétrogradé : un réglage illisible ne doit jamais empêcher d'ouvrir la caisse.
        assertEquals(ShopMode.PAR_DEFAUT, ShopMode.depuisCle(null))
        assertEquals(ShopMode.PAR_DEFAUT, ShopMode.depuisCle(""))
        assertEquals(ShopMode.PAR_DEFAUT, ShopMode.depuisCle("mode_venu_du_futur"))
        assertEquals(ShopMode.DETAIL, ShopMode.PAR_DEFAUT)
    }

    @Test
    fun `seul le mode grossiste applique le prix de gros`() {
        val produit = Product(
            name = "Farine 50kg",
            price = 120_000.0,
            category = "Épicerie - Farine & Boulangerie",
            stock = 10.0,
            wholesalePrice = 100_000.0
        )

        assertTrue(PriceUtil.isWholesaleActive(produit, ShopMode.GROSSISTE))
        assertEquals(100_000.0, PriceUtil.displayPrice(produit, ShopMode.GROSSISTE), 1e-9)

        // Les deux nouveaux métiers ne doivent surtout pas hériter du tarif de gros.
        for (mode in listOf(ShopMode.DETAIL, ShopMode.PHARMACIE, ShopMode.BAR)) {
            assertFalse("$mode ne doit pas appliquer le prix de gros", PriceUtil.isWholesaleActive(produit, mode))
            assertEquals(120_000.0, PriceUtil.displayPrice(produit, mode), 1e-9)
        }
    }

    @Test
    fun `sans prix de gros renseigne, le mode grossiste garde le prix de detail`() {
        val produit = Product(
            name = "Bonbon",
            price = 100.0,
            category = "Épicerie - Confiseries & Snacks",
            stock = 500.0,
            wholesalePrice = null
        )

        assertFalse(PriceUtil.isWholesaleActive(produit, ShopMode.GROSSISTE))
        assertEquals(100.0, PriceUtil.displayPrice(produit, ShopMode.GROSSISTE), 1e-9)
    }
}
