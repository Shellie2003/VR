package com.example.util

import com.example.data.model.Product

/**
 * Le filtrage du catalogue, en mémoire et en Kotlin pur.
 *
 * **Pourquoi cette fonction existe.** Côté Android, filtrer le catalogue est une requête SQL
 * (`InventoryDao.searchProducts`) : la base est là, autant la laisser travailler. La version de
 * bureau, elle, n'a pas encore de base — partager le dépôt et les DAO exigerait Room KMP, c'est-à-dire
 * toucher au fichier de base de données de chaque boutique en service, et c'est délibérément remis à
 * plus tard. Il faut donc une description du « ce qui correspond » que l'écran de bureau puisse
 * appliquer à une liste déjà chargée.
 *
 * **Le risque, nommé plutôt que dissimulé :** deux descriptions de la même règle peuvent diverger.
 * La requête SQL reste la référence sur Android ; celle-ci en est l'équivalent en mémoire, et
 * `CatalogueFiltreTest` épingle champ par champ les critères de cette requête (nom, catégorie,
 * code-barres, DCI ; exclusion des modèles ; seuil de stock faible) précisément pour qu'une
 * divergence se voie en CI au lieu de se découvrir à l'écran.
 *
 * Deux écarts assumés avec SQLite, tous deux dans le sens de l'utilisateur :
 *  - `LIKE` de SQLite est insensible à la casse pour l'ASCII seulement ; ici la comparaison passe
 *    par `lowercase()`, donc les caractères accentués en bénéficient aussi.
 *  - `ORDER BY name ASC` trie en binaire (les majuscules d'abord : « Zébu » avant « ananas ») ;
 *    ici le tri ignore la casse, ce qu'un gérant qui parcourt une liste attend.
 */
object CatalogueFiltre {

    /** Valeur sentinelle de la requête SQL pour « toutes les catégories ». Reprise à l'identique. */
    const val TOUTES_CATEGORIES = "All"

    fun filtrer(
        produits: List<Product>,
        recherche: String = "",
        categorie: String = TOUTES_CATEGORIES,
        stockFaibleSeulement: Boolean = false
    ): List<Product> {
        val terme = recherche.trim().lowercase()
        return produits.asSequence()
            // Les modèles pré-enregistrés (`isTemplate`) n'ont jamais à apparaître dans une liste de
            // vente ou de stock : ils servent uniquement à pré-remplir une fiche produit.
            .filter { !it.isTemplate }
            .filter { categorie == TOUTES_CATEGORIES || it.category == categorie }
            .filter { !stockFaibleSeulement || it.stock < it.lowStockThreshold }
            .filter { terme.isEmpty() || correspond(it, terme) }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    private fun correspond(produit: Product, termeEnMinuscules: String): Boolean =
        produit.name.lowercase().contains(termeEnMinuscules) ||
            produit.category.lowercase().contains(termeEnMinuscules) ||
            produit.barcode.lowercase().contains(termeEnMinuscules) ||
            // La DCI est nullable : un produit d'épicerie n'en a pas. `== true` plutôt qu'un
            // `?: false` pour que l'absence de principe actif ne fasse jamais correspondre.
            (produit.dci?.lowercase()?.contains(termeEnMinuscules) == true)

    /**
     * Les catégories réellement présentes dans le catalogue, pour alimenter un filtre.
     *
     * Dérivées des produits et non d'une liste figée : une catégorie personnalisée créée par le
     * gérant doit apparaître ici, c'est exactement le correctif qui avait été nécessaire côté
     * Android pour la liste déroulante de la fiche produit.
     */
    fun categories(produits: List<Product>): List<String> =
        produits.asSequence()
            .filter { !it.isTemplate }
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
}
