package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["barcode"]),
        Index(value = ["sku"]),
        Index(value = ["isTemplate"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val category: String,
    val stock: Double,
    val imageUrl: String = "",
    val lowStockThreshold: Double = 5.0,
    val unit: String = "Pièce",
    val barcode: String = "",
    val wholesalePrice: Double? = null,
    val sku: String = "",
    val stock_quantity: Int = 0,
    val nomCourt: String? = null,
    val sousCategorie: String? = null,
    val marque: String? = null,
    val description: String? = null,
    val stockMax: Double? = null,
    val emplacement: String? = null,
    val fournisseurId: Long? = null,
    val gerePeremption: Boolean = false,
    val taxable: Boolean = false,
    val tauxTaxe: Double = 0.0,
    val prixAchatUniteBase: Double = 0.0,
    val isTemplate: Boolean = false,

    // --- Mode pharmacie (section 71) ---
    // Tous facultatifs et vides par défaut : un produit d'épicerie n'est pas concerné, et les
    // produits déjà en base restent valides sans reprise de données.
    /**
     * Dénomination Commune Internationale : le principe actif (« paracétamol »), par opposition au
     * nom commercial (« Doliprane »). C'est ce que le client demande au comptoir, et souvent le seul
     * mot qu'il connaisse — d'où sa présence dans la recherche.
     */
    val dci: String? = null,
    /** Dosage, tel qu'imprimé sur la boîte : « 500 mg », « 1 g », « 2,5 mg/ml ». */
    val dosage: String? = null,
    /** Forme galénique : comprimé, sirop, gélule, suppositoire, injectable... */
    val formeGalenique: String? = null,
    /** Délivrance soumise à ordonnance : le vendeur doit être alerté avant de servir. */
    val surOrdonnance: Boolean = false
) {
    val isAvailable: Boolean get() = stock > 0
    val isLowStock: Boolean get() = stock < lowStockThreshold
}
