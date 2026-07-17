package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restocks")
data class Restock(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val cartonsQuantity: Double,
    val itemsPerCarton: Double,
    val totalUnits: Double,
    val totalCostPrice: Double,
    val unitSellingPrice: Double,
    val supplierId: Long?,
    val supplierName: String?,
    val timestamp: Long = System.currentTimeMillis()
)
