package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// C.1: partial or full return/refund of a past sale. Stored separately from Sale (rather than
// mutating it) so the original transaction record stays intact for accounting/exports, while the
// return itself is fully traceable (what was returned, when, why, how it was refunded).
@Entity(tableName = "retours")
data class Retour(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<SoldItem>,
    val totalAmount: Double,
    val motif: String = "",
    // The payment mode of the original sale, reused to decide how the refund affects the cash
    // drawer (see InventoryRepository.processReturn): ESPECES triggers an automatic cash-out
    // MouvementCaisse, other modes don't touch the physical till.
    val modePaiementOrigine: String = "ESPECES"
)
