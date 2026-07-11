package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["name"]),
        Index(value = ["category"]),
        Index(value = ["barcode"])
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
    val wholesalePrice: Double? = null
) {
    val isAvailable: Boolean get() = stock > 0
    val isLowStock: Boolean get() = stock < lowStockThreshold
}
