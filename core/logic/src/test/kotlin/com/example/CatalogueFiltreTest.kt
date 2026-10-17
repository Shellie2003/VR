package com.example

import com.example.data.model.Product
import com.example.util.CatalogueFiltre
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ces cas épinglent, critère par critère, ce que fait la requête SQL `InventoryDao.searchProducts`
 * côté Android. C'est le seul garde-fou contre la divergence des deux descriptions de la même règle
 * (voir le KDoc de [CatalogueFiltre]) : si quelqu'un ajoute un champ à la recherche SQL sans
 * l'ajouter ici, le bureau cessera silencieusement de trouver ce que le téléphone trouve.
 */
class CatalogueFiltreTest {

    private fun produit(
        id: Int,
        nom: String,
        categorie: String = "Alimentation",
        stock: Double = 10.0,
        seuil: Double = 5.0,
        codeBarre: String = "",
        dci: String? = null,
        modele: Boolean = false
    ) = Product(
        id = id,
        name = nom,
        price = 1000.0,
        category = categorie,
        stock = stock,
        lowStockThreshold = seuil,
        barcode = codeBarre,
        dci = dci,
        isTemplate = modele
    )

    @Test
    fun `les modeles pre-enregistres n'apparaissent jamais`() {
        // Un modèle n'est pas un produit de la boutique : il sert à pré-remplir une fiche. En
        // laisser passer un, c'est proposer à la vente un article que le gérant n'a pas en stock.
        val resultat = CatalogueFiltre.filtrer(
            listOf(produit(1, "Riz"), produit(2, "Riz importé", modele = true))
        )
        assertEquals(listOf("Riz"), resultat.map { it.name })
    }

    @Test
    fun `la recherche porte sur le nom, la categorie, le code-barres et la DCI`() {
        val catalogue = listOf(
            produit(1, "Doliprane", dci = "paracétamol"),
            produit(2, "Savon", categorie = "Droguerie"),
            produit(3, "Huile", codeBarre = "6001234567890"),
            produit(4, "Sucre")
        )

        // Le client demande le principe actif, pas le nom commercial — c'est tout l'intérêt du
        // champ DCI, et il doit rester cherchable.
        assertEquals(listOf("Doliprane"), CatalogueFiltre.filtrer(catalogue, "paracét").map { it.name })
        assertEquals(listOf("Savon"), CatalogueFiltre.filtrer(catalogue, "Droguerie").map { it.name })
        assertEquals(listOf("Huile"), CatalogueFiltre.filtrer(catalogue, "600123").map { it.name })
        assertEquals(listOf("Sucre"), CatalogueFiltre.filtrer(catalogue, "sucre").map { it.name })
    }

    @Test
    fun `la casse et les espaces autour de la recherche sont ignores`() {
        val catalogue = listOf(produit(1, "Huile de tournesol"))
        assertEquals(1, CatalogueFiltre.filtrer(catalogue, "  TOURNESOL ").size)
    }

    @Test
    fun `une recherche vide rend tout le catalogue`() {
        // Le cas le plus courant de loin : l'écran s'ouvre sans recherche, et doit tout montrer.
        val catalogue = listOf(produit(1, "Riz"), produit(2, "Sel"))
        assertEquals(2, CatalogueFiltre.filtrer(catalogue, "").size)
        assertEquals(2, CatalogueFiltre.filtrer(catalogue, "   ").size)
    }

    @Test
    fun `un produit sans DCI ne correspond jamais par ce champ`() {
        // Si l'absence de principe actif était traitée comme une chaîne vide, elle contiendrait
        // n'importe quel terme pour peu qu'on cherche du vide — et tout remonterait.
        val catalogue = listOf(produit(1, "Riz", dci = null))
        assertTrue(CatalogueFiltre.filtrer(catalogue, "paracétamol").isEmpty())
    }

    @Test
    fun `le filtre par categorie est une egalite exacte, pas une correspondance partielle`() {
        // La puce de catégorie vient d'une liste fermée : « Boisson » ne doit pas ramener
        // « Boissons alcoolisées », sans quoi deux puces afficheraient le même contenu.
        val catalogue = listOf(
            produit(1, "Eau", categorie = "Boisson"),
            produit(2, "Rhum", categorie = "Boissons alcoolisées")
        )
        assertEquals(listOf("Eau"), CatalogueFiltre.filtrer(catalogue, categorie = "Boisson").map { it.name })
    }

    @Test
    fun `All rend toutes les categories`() {
        val catalogue = listOf(produit(1, "Eau", categorie = "Boisson"), produit(2, "Riz"))
        assertEquals(2, CatalogueFiltre.filtrer(catalogue, categorie = CatalogueFiltre.TOUTES_CATEGORIES).size)
    }

    @Test
    fun `le stock faible compare au seuil propre a chaque produit`() {
        // Chaque produit porte son propre seuil : une boîte d'allumettes et un sac de riz ne se
        // réapprovisionnent pas au même niveau. Un seuil global donnerait des alertes fausses.
        val catalogue = listOf(
            produit(1, "Riz", stock = 3.0, seuil = 5.0),
            produit(2, "Sel", stock = 20.0, seuil = 5.0),
            produit(3, "Allumettes", stock = 30.0, seuil = 50.0)
        )
        assertEquals(
            listOf("Allumettes", "Riz"),
            CatalogueFiltre.filtrer(catalogue, stockFaibleSeulement = true).map { it.name }
        )
    }

    @Test
    fun `le tri ignore la casse`() {
        // Un tri binaire placerait « Zébu » avant « ananas » (majuscules d'abord), ce qui n'a
        // aucun sens pour quelqu'un qui parcourt une liste des yeux.
        val catalogue = listOf(produit(1, "Zébu"), produit(2, "ananas"), produit(3, "Beurre"))
        assertEquals(
            listOf("ananas", "Beurre", "Zébu"),
            CatalogueFiltre.filtrer(catalogue).map { it.name }
        )
    }

    @Test
    fun `les categories sont deduites du catalogue, sans doublon ni modele`() {
        val catalogue = listOf(
            produit(1, "Riz", categorie = "Alimentation"),
            produit(2, "Sel", categorie = "Alimentation"),
            produit(3, "Eau", categorie = "Boisson"),
            produit(4, "Vide", categorie = ""),
            produit(5, "Modèle", categorie = "Pièces moto", modele = true)
        )
        // « Pièces moto » ne vient que d'un modèle : la proposer filtrerait sur un ensemble vide.
        assertEquals(listOf("Alimentation", "Boisson"), CatalogueFiltre.categories(catalogue))
    }
}
