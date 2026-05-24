package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val category: String,
    val stock: Int,
    val imageUrl: String = ""
) {
    val isAvailable: Boolean get() = stock > 0
}
