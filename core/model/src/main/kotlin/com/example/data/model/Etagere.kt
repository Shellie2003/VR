package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une étagère physique du magasin, positionnée horizontalement parmi les autres (gauche/droite).
 *
 * `position` n'est volontairement PAS contiguë — voir [com.example.util.EtagereLayout] pour le
 * calcul — afin qu'ajouter une étagère ne déplace jamais celles déjà en place.
 */
@Entity(tableName = "etageres", indices = [Index(value = ["position"])])
data class Etagere(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val position: Int,
    /** Hex `#RRGGBB` normalisé par [com.example.util.EtagereColors.normaliser], ou null (pas de couleur). */
    val couleur: String? = null
)

/**
 * Un niveau (rayon) d'une étagère, empilé verticalement (haut/bas) au sein de celle-ci. C'est
 * l'unité qu'on appelle « une case » à l'écran : cliquer dessus montre les produits qui y sont
 * rangés (voir [ProduitNiveau]).
 */
@Entity(
    tableName = "niveaux_etagere",
    indices = [Index(value = ["etagereId"]), Index(value = ["etagereId", "position"])]
)
data class NiveauEtagere(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val etagereId: Long,
    val position: Int,
    val nom: String = "",
    /** Hex `#RRGGBB` normalisé par [com.example.util.EtagereColors.normaliser], ou null (pas de couleur). */
    val couleur: String? = null
)

/**
 * Un produit rangé dans un niveau précis. La table reste techniquement many-to-many (une case
 * contient couramment plusieurs références), mais côté produit un seul lien est maintenu à la
 * fois : ranger un produit ailleurs (recherche depuis une autre case, sélecteur de rayon de la
 * fiche produit, ou déplacement explicite) retire toujours l'ancien lien en même temps qu'il
 * ajoute le nouveau — voir [com.example.util.EtagereLayout.diffEmplacementUnique]. Ainsi
 * l'emplacement affiché dans la fiche produit et celui vu depuis l'écran Étagère ne peuvent
 * jamais diverger.
 */
@Entity(
    tableName = "produits_niveau",
    indices = [
        Index(value = ["niveauId"]),
        Index(value = ["produitId"]),
        Index(value = ["niveauId", "produitId"], unique = true)
    ]
)
data class ProduitNiveau(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val niveauId: Long,
    val produitId: Int,
    val ordre: Int = 0
)
