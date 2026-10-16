package com.example.data.model

/**
 * Plan d'étagères résolu : l'arbre étagères → niveaux → produits, tel que l'écran Étagère et la
 * recherche d'emplacement le consomment.
 *
 * **Ces deux classes étaient imbriquées dans `InventoryViewModel`**, ce qui les rendait
 * inaccessibles hors de `:app` — et bloquait `RechercheEtagere`, le dernier calcul pur encore
 * prisonnier du module Android (sections 78 à 80). Elles n'ont pourtant rien d'un détail de
 * ViewModel : ce sont de simples porteurs de données composés exclusivement de types de ce module
 * (`Etagere`, `NiveauEtagere`, `ProduitNiveau`, `Product`), donc leur place est ici.
 *
 * Contrairement aux déplacements précédents, celui-ci **ne pouvait pas** se faire sans toucher aux
 * sites d'appel : Kotlin n'autorise pas de `typealias` imbriqué dans une classe, il était donc
 * impossible de laisser `InventoryViewModel.EtagereAvecNiveaux` en place comme alias de
 * compatibilité. Les 15 références ont été réécrites, et c'est précisément pour cela que cette
 * étape a été faite seule.
 */

/** Un niveau (la case cliquable du plan) avec les produits qui y sont rangés, déjà résolus. */
data class NiveauAvecProduits(
    val niveau: NiveauEtagere,
    val liens: List<ProduitNiveau>,
    val produits: List<Product>
)

/** Une étagère avec ses niveaux, du plus haut au plus bas selon leur position. */
data class EtagereAvecNiveaux(
    val etagere: Etagere,
    val niveaux: List<NiveauAvecProduits>
)
