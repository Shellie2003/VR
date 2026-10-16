package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// 1. produits — Fiche produit centrale
@Entity(tableName = "produits")
data class Produit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    // Identification
    val nom: String,
    val nomCourt: String? = null,           // pour ticket de caisse
    val categorie: String,
    val sousCategorie: String? = null,
    val marque: String? = null,
    val description: String? = null,

    // Unité de référence pour le STOCK (toujours la plus petite unité)
    val uniteBase: String,                  // "pièce", "kg", "litre"

    // Stock (toujours exprimé en unité de base)
    val quantiteStock: Double = 0.0,
    val seuilAlerte: Double = 5.0,
    val stockMax: Double? = null,
    val emplacement: String? = null,

    // Coût de référence (utile pour calcul marge rapide)
    val prixAchatUniteBase: Double,

    // Fournisseur principal
    val fournisseurId: Long? = null,

    // Traçabilité
    val gerePeremption: Boolean = false,    // active/désactive suivi date
    val imageUrl: String? = null,
    val codeBarrePrincipal: String? = null, // code-barres "par défaut" (souvent l'unité pièce)

    // Fiscalité
    val taxable: Boolean = false,
    val tauxTaxe: Double = 0.0,

    // Statut
    val actif: Boolean = true,
    val dateAjout: Long = System.currentTimeMillis(),
    val dateDerniereMaj: Long = System.currentTimeMillis()
)

// 2. unites_produit — Hiérarchie des unités de vente
@Entity(
    tableName = "unites_produit",
    foreignKeys = [ForeignKey(
        entity = Produit::class,
        parentColumns = ["id"],
        childColumns = ["produitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("produitId"), Index("codeBarre", unique = true)]
)
data class UniteProduit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produitId: Long,

    val nomUnite: String,          // "pièce", "paquet", "cartouche", "carton", "kapoka", "toko", "sac"
    val facteurVersBase: Double,   // ex: 1 carton = 27 pièces

    val prixVente: Double,         // prix de cette unité
    val prixAchat: Double? = null, // coût si acheté directement à ce niveau (ex: grossiste achète en carton)

    val codeBarre: String? = null, // code-barres spécifique à ce niveau (carton ≠ pièce)
    val estUniteBase: Boolean = false,
    val estUniteVenteDefaut: Boolean = false, // unité proposée par défaut au scan
    val ordre: Int = 0,            // pour trier pièce < paquet < carton
    val actif: Boolean = true
)

// 3. regles_prix — Lots fixes et paliers dégressifs
@Entity(
    tableName = "regles_prix",
    foreignKeys = [
        ForeignKey(entity = Produit::class, parentColumns = ["id"], childColumns = ["produitId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UniteProduit::class, parentColumns = ["id"], childColumns = ["uniteId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("produitId"), Index("uniteId")]
)
data class ReglePrix(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produitId: Long,
    val uniteId: Long,             // sur quelle unité s'applique la règle

    val quantiteMin: Double,       // ex: 2 (pour "2 pour 500 Ar")
    val prixTotal: Double,         // ex: 500

    val typeRegle: String,         // "LOT_FIXE" | "PALIER_DEGRESSIF"

    val dateDebut: Long? = null,   // promo temporaire (optionnel)
    val dateFin: Long? = null,

    val actif: Boolean = true
)

// 4. fournisseurs
@Entity(tableName = "fournisseurs")
data class Fournisseur(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val contact: String? = null,       // téléphone
    val adresse: String? = null,
    val delaiReapproJours: Int? = null,
    val actif: Boolean = true
)

// 5. mouvements_stock — Historique entrées/sorties (traçabilité)
@Entity(
    tableName = "mouvements_stock",
    foreignKeys = [ForeignKey(entity = Produit::class, parentColumns = ["id"], childColumns = ["produitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("produitId")]
)
data class MouvementStock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produitId: Long,

    val type: String,              // "ENTREE" | "SORTIE_VENTE" | "CASSE" | "PEREMPTION" | "CORRECTION" | "RETOUR"
    val quantite: Double,          // toujours en unité de base, en valeur absolue
    val quantiteAvant: Double,
    val quantiteApres: Double,

    val referenceId: Long? = null, // ex: id de la vente ou du réappro lié
    val note: String? = null,

    val dateMouvement: Long = System.currentTimeMillis()
)

// 6. lots_produit — Traçabilité péremption (si activée)
@Entity(
    tableName = "lots_produit",
    foreignKeys = [ForeignKey(entity = Produit::class, parentColumns = ["id"], childColumns = ["produitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("produitId")]
)
data class LotProduit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produitId: Long,
    val numeroLot: String? = null,
    val quantite: Double,           // en unité de base
    val datePeremption: Long,
    val dateReception: Long = System.currentTimeMillis()
)

// 7. ventes et lignes_vente — Transaction de caisse
@Entity(tableName = "ventes")
data class Vente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateVente: Long = System.currentTimeMillis(),
    val montantTotal: Double,
    val modePaiement: String,      // "ESPECES" | "MVOLA" | "ORANGE_MONEY" | "CREDIT"
    val clientId: Long? = null,    // pour vente à crédit / client fidèle
    val vendeurId: Long? = null
)

@Entity(
    tableName = "lignes_vente",
    foreignKeys = [
        ForeignKey(entity = Vente::class, parentColumns = ["id"], childColumns = ["venteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Produit::class, parentColumns = ["id"], childColumns = ["produitId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = UniteProduit::class, parentColumns = ["id"], childColumns = ["uniteId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("venteId"), Index("produitId"), Index("uniteId")]
)
data class LigneVente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venteId: Long,
    val produitId: Long,
    val uniteId: Long,             // unité choisie au moment de la vente

    val quantite: Double,          // dans l'unité choisie (ex: 2 paquets)
    val prixUnitaireApplique: Double, // prix réellement appliqué (après règle éventuelle)
    val montantLigne: Double,

    val regleAppliqueeId: Long? = null // traçabilité : quelle promo a joué
)

/**
 * Quelle quantité de quelle ligne de vente est sortie de quel lot.
 *
 * **Pourquoi une table de liaison plutôt qu'un `lotId` sur [LigneVente].** Une même ligne peut
 * couvrir PLUSIEURS lots — vendre 10 boîtes alors que le lot le plus proche de la péremption n'en
 * contient que 6 en prend 6 ici et 4 sur le suivant. Un champ unique sur la ligne ne saurait pas
 * représenter ce cas, pourtant courant dès qu'on suit les dates. Ça évite en prime toute migration
 * sur `lignes_vente`, table déjà écrite à chaque vente.
 *
 * **Reste vide pour qui ne saisit pas de lots**, c'est-à-dire la quasi-totalité des épiceries :
 * aucune ligne n'est créée, et la caisse se comporte exactement comme avant.
 *
 * C'est cette table qui rend possibles les deux gestes impossibles jusqu'ici : répondre à « qui a
 * acheté ce lot rappelé ? » et écouler en priorité ce qui périme le plus tôt.
 */
@Entity(
    tableName = "ligne_vente_lots",
    foreignKeys = [
        ForeignKey(entity = LigneVente::class, parentColumns = ["id"], childColumns = ["ligneVenteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LotProduit::class, parentColumns = ["id"], childColumns = ["lotId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("ligneVenteId"), Index("lotId")]
)
data class LigneVenteLot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ligneVenteId: Long,
    val lotId: Long,
    /** Quantité prélevée sur ce lot, en unité de base. */
    val quantite: Double
)
