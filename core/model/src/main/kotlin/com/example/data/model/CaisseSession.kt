package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// C.2: a work session (ouverture/fermeture de caisse) bracketing a shift. Reconciliation counts
// only ESPECES sales as physical cash movements — Mvola/Orange Money/Crédit sales never touch the
// drawer, so they must never be included in the theoretical amount.
@Entity(tableName = "caisse_sessions")
data class CaisseSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateOuverture: Long = System.currentTimeMillis(),
    val montantOuverture: Double,
    val dateFermeture: Long? = null,
    val montantCompteFermeture: Double? = null,
    val montantTheoriqueFermeture: Double? = null,
    val ecart: Double? = null,
    val note: String = ""
) {
    val isOpen: Boolean get() = dateFermeture == null
}
