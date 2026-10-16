package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Tombstone: records that a piece of data was intentionally deleted, so that restoring an older
// local safety backup, downloading an older Firebase backup, or syncing with a device that never
// saw the deletion can never silently resurrect it. Without this, syncFullDatabaseSync (which is
// purely additive by design, so a restore never loses data) would re-insert a deleted product/sale
// the next time it encountered it in any backup or peer payload.
//
// entityType is one of: "product", "sale", "debt", "restock", "mouvementCaisse", "caisseSession",
// "vendeur", "retour", "lot". naturalKey is a stable, cross-device identifier for that record (see
// the *NaturalKey() helpers in InventoryViewModel) — never the local autoIncrement id, since that
// can differ between devices for the same real-world record.
@Entity(
    tableName = "deleted_records",
    indices = [Index(value = ["entityType", "naturalKey"], unique = true)]
)
data class DeletedRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val naturalKey: String,
    val deletedAt: Long = System.currentTimeMillis()
)
