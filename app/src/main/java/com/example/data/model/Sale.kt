package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class SoldItem(
    val productId: Int,
    val name: String,
    val quantity: Int,
    val price: Double
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalAmount: Double,
    val items: List<SoldItem>
)
