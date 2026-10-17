package com.example

import com.example.data.model.Product
import com.example.util.Monnaie
import com.example.util.PanierCaisse
import com.example.util.ShopMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Le panier décide du montant encaissé. Ces règles vivaient dans `InventoryViewModel`, donc dans un
 * module Android, donc sans test possible hors harnais Android — alors que c'est le calcul le plus
 * exposé de l'application.
 */
class PanierCaisseTest {

    private fun produit(
        id: Int = 1,
        prix: Double = 3500.0,
        stock: Double = 10.0,
        gros: Double? = null,
        unite: String = "Pièce",
        taxable: Boolean = false,
        taux: Double = 0.0
    ) = Product(
        id = id,
        name = "Produit $id",
        price = prix,
        category = "Alimentation",
        stock = stock,
        unit = unite,
        wholesalePrice = gros,
        taxable = taxable,
        tauxTaxe = taux
    )

    @Test
    fun `ajouter deux fois le meme produit additionne la ligne au lieu d'en creer une seconde`() {
        var panier = PanierCaisse.ajouterProduit(emptyList(), produit(), 2.0, ShopMode.DETAIL)
        panier = PanierCaisse.ajouterProduit(panier, produit(), 3.0, ShopMode.DETAIL)
        assertEquals(1, panier.size)
        assertEquals(5.0, panier.first().quantity, 1e-9)
    }

    @Test
    fun `la quantite est plafonnee au stock disponible`() {
        // La caisse ne doit pas composer un panier que la boutique ne peut pas servir.
        val panier = PanierCaisse.ajouterProduit(emptyList(), produit(stock = 4.0), 10.0, ShopMode.DETAIL)
        assertEquals(4.0, panier.first().quantity, 1e-9)
    }

    @Test
    fun `un produit en rupture n'entre pas et le panier est rendu inchange`() {
        val vide = emptyList<com.example.data.model.CartItem>()
        // `assertSame` : la MÊME référence, donc aucune recomposition Compose pour un geste sans
        // effet — c'est la raison d'être des listes immuables ici.
        assertSame(vide, PanierCaisse.ajouterProduit(vide, produit(stock = 0.0), 1.0, ShopMode.DETAIL))
        assertSame(vide, PanierCaisse.ajouterProduit(vide, produit(stock = -3.0), 1.0, ShopMode.DETAIL))
    }

    @Test
    fun `le mode grossiste facture le prix de gros des l'ajout`() {
        val panier = PanierCaisse.ajouterProduit(
            emptyList(), produit(prix = 3500.0, gros = 3100.0), 2.0, ShopMode.GROSSISTE
        )
        assertEquals(3100.0, panier.first().price, 1e-9)
        assertEquals(3500.0, panier.first().regularPrice, 1e-9)
        assertEquals(6200.0, Monnaie.total(panier), 1e-9)
    }

    @Test
    fun `la TVA du produit est reportee sur la ligne de panier`() {
        // Sans ce report, la ventilation TVA du reçu serait vide alors que le produit est taxable.
        val panier = PanierCaisse.ajouterProduit(
            emptyList(), produit(prix = 11500.0, taxable = true, taux = 15.0), 1.0, ShopMode.DETAIL
        )
        assertEquals(1500.0, Monnaie.totalTaxe(panier), 1e-6)
    }

    @Test
    fun `deux lignes divers de meme montant restent distinctes`() {
        // Deux « divers » de 500 Ar sont deux encaissements : le vendeur doit pouvoir retirer
        // seulement celui sur lequel il s'est trompé.
        var panier = PanierCaisse.ajouterDivers(emptyList(), "Divers", 500.0, 1.0)
        panier = PanierCaisse.ajouterDivers(panier, "Divers", 500.0, 1.0)
        assertEquals(2, panier.size)
        assertEquals(2, panier.map { it.id }.distinct().size)
        assertEquals(1000.0, Monnaie.total(panier), 1e-9)
    }

    @Test
    fun `le moins retire la ligne quand la quantite atteint zero`() {
        // C'est le geste attendu quand on appuie sur `-` jusqu'au bout : laisser une ligne à zéro
        // serait du bruit que le vendeur devrait nettoyer à la main.
        val panier = PanierCaisse.ajouterProduit(emptyList(), produit(), 1.0, ShopMode.DETAIL)
        val apres = PanierCaisse.deplacerQuantite(panier, panier.first().id, -1.0)
        assertTrue(apres.isEmpty())
    }

    @Test
    fun `une saisie directe a zero ne fait PAS disparaitre la ligne`() {
        // Différence volontaire avec le bouton `-` : une saisie au clavier qui escamoterait la
        // ligne sous le doigt serait déroutante au moment d'annoncer le total.
        val panier = PanierCaisse.ajouterProduit(emptyList(), produit(), 2.0, ShopMode.DETAIL)
        val apres = PanierCaisse.fixerQuantite(panier, panier.first().id, 0.0)
        assertEquals(1, apres.size)
        assertEquals(0.01, apres.first().quantity, 1e-9)
    }

    @Test
    fun `une saisie directe au-dela du stock est plafonnee`() {
        val panier = PanierCaisse.ajouterProduit(emptyList(), produit(stock = 6.0), 1.0, ShopMode.DETAIL)
        val apres = PanierCaisse.fixerQuantite(panier, panier.first().id, 99.0)
        assertEquals(6.0, apres.first().quantity, 1e-9)
    }

    @Test
    fun `le plus ne depasse jamais le stock`() {
        val panier = PanierCaisse.ajouterProduit(emptyList(), produit(stock = 3.0), 3.0, ShopMode.DETAIL)
        val apres = PanierCaisse.deplacerQuantite(panier, panier.first().id, 5.0)
        assertEquals(3.0, apres.first().quantity, 1e-9)
    }

    @Test
    fun `un identifiant inconnu laisse le panier intact sans lever`() {
        // Arrive réellement : la ligne a pu être retirée entre l'affichage et le clic.
        val panier = PanierCaisse.ajouterProduit(emptyList(), produit(), 1.0, ShopMode.DETAIL)
        assertSame(panier, PanierCaisse.deplacerQuantite(panier, "product_999", -1.0))
        assertSame(panier, PanierCaisse.fixerQuantite(panier, "product_999", 2.0))
        assertSame(panier, PanierCaisse.retirer(panier, "product_999"))
    }

    @Test
    fun `annuler le dernier geste sur un panier vide ne leve pas`() {
        val vide = emptyList<com.example.data.model.CartItem>()
        assertSame(vide, PanierCaisse.annulerDernier(vide))
    }

    @Test
    fun `les quantites decimales d'un produit au poids traversent tout le calcul`() {
        // Un quart de kilo ajouté trois fois doit donner 0,75 kg, pas une valeur arrondie.
        var panier = PanierCaisse.ajouterProduit(
            emptyList(), produit(prix = 4000.0, unite = "Kilogramme"), 0.25, ShopMode.DETAIL
        )
        repeat(2) { panier = PanierCaisse.ajouterProduit(panier, produit(prix = 4000.0, unite = "Kilogramme"), 0.25, ShopMode.DETAIL) }
        assertEquals(0.75, panier.first().quantity, 1e-9)
        assertEquals(3000.0, Monnaie.total(panier), 1e-9)
    }
}
