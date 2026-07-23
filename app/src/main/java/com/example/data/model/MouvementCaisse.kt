package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mouvements_caisse")
data class MouvementCaisse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // "ENTREE" | "SORTIE"
    val montant: Double,
    val motif: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)
