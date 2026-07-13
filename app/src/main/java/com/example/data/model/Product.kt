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
        Index(value = ["sku"])
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
    val prixAchatUniteBase: Double = 0.0
) {
    val isAvailable: Boolean get() = stock > 0
    val isLowStock: Boolean get() = stock < lowStockThreshold
}
