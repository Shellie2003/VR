package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class SoldItem(
    val productId: Int,
    val name: String,
    val quantity: Double,
    val price: Double,
    // B.2: price is tax-inclusive; taxable/tauxTaxe record the VAT rate applied at sale time so
    // historical reports (Dashboard, exports) can break out VAT even if a product's rate changes later.
    val taxable: Boolean = false,
    val tauxTaxe: Double = 0.0
) {
    val taxAmount: Double get() = if (taxable && tauxTaxe > 0.0) (price * quantity) * tauxTaxe / (100.0 + tauxTaxe) else 0.0
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val totalAmount: Double,
    val items: List<SoldItem>
)
