package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// B.3/E.2: opt-in employee accounts. When this table is empty (the default for every existing
// shop), the app behaves exactly as before — no login, no PIN, no restriction anywhere. The
// feature only activates once the owner creates a first account from Paramètres.
@Entity(tableName = "vendeurs")
data class Vendeur(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    // SHA-256 hex digest of the PIN, never the PIN itself (see Vendeur.hashPin).
    val pinHash: String,
    val role: String = ROLE_VENDEUR, // ROLE_GERANT | ROLE_VENDEUR
    val actif: Boolean = true,
    val dateCreation: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_GERANT = "GERANT"
        const val ROLE_VENDEUR = "VENDEUR"

        fun hashPin(pin: String): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(pin.trim().toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
