package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Journal d'audit inaltérable côté application : chaque geste sensible (suppression, annulation,
 * remise inhabituelle, sortie de caisse, écart de fond de caisse, vente à perte...) y laisse une
 * trace horodatée avec l'appareil et l'employé concernés.
 *
 * Deux usages :
 *  - la traçabilité brute, pour que le gérant puisse répondre à « qui a fait ça, et quand ? » ;
 *  - l'alimentation de [com.example.util.AnomalyDetector], qui transforme ces traces (et les
 *    ventes/retours/mouvements de caisse) en alertes lisibles.
 *
 * Le journal n'est jamais effacé par un employé : seul le gérant peut le purger, et la purge
 * elle-même est journalisée.
 */
@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["type"]),
        Index(value = ["severite"])
    ]
)
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    /** Nom de l'appareil au moment du geste (voir AppPreferences.deviceName). */
    val deviceName: String = "",
    /** Nom du vendeur/gérant connecté, vide si la gestion des comptes n'est pas activée. */
    val utilisateur: String = "",
    /** GERANT | VENDEUR | "" */
    val role: String = "",
    /** Une des constantes [Type]. */
    val type: String,
    /** Ce qui a été touché : "Vente du 12/03 14:22", "Produit Vary", ... */
    val cible: String = "",
    val details: String = "",
    val montant: Double = 0.0,
    /** [SEVERITE_INFO] | [SEVERITE_ATTENTION] | [SEVERITE_ALERTE] */
    val severite: String = SEVERITE_INFO,
    /** true quand l'action a été REFUSÉE (tentative d'un employé sur un geste réservé au gérant). */
    val bloque: Boolean = false
) {
    companion object {
        const val SEVERITE_INFO = "INFO"
        const val SEVERITE_ATTENTION = "ATTENTION"
        const val SEVERITE_ALERTE = "ALERTE"

        object Type {
            const val SUPPRESSION_VENTE = "SUPPRESSION_VENTE"
            const val SUPPRESSION_DETTE = "SUPPRESSION_DETTE"
            const val SUPPRESSION_PRODUIT = "SUPPRESSION_PRODUIT"
            const val SUPPRESSION_MOUVEMENT_CAISSE = "SUPPRESSION_MOUVEMENT_CAISSE"
            const val SUPPRESSION_REAPPRO = "SUPPRESSION_REAPPRO"
            const val SUPPRESSION_RETOUR = "SUPPRESSION_RETOUR"
            const val PURGE_JOURNAL = "PURGE_JOURNAL"
            const val TENTATIVE_REFUSEE = "TENTATIVE_REFUSEE"
            const val VENTE_A_PERTE = "VENTE_A_PERTE"
            const val REMISE_ANORMALE = "REMISE_ANORMALE"
            const val RETOUR = "RETOUR"
            const val SORTIE_CAISSE = "SORTIE_CAISSE"
            const val ECART_CAISSE = "ECART_CAISSE"
            const val AJUSTEMENT_STOCK = "AJUSTEMENT_STOCK"
            const val CONNEXION = "CONNEXION"
            const val CHANGEMENT_PRIX = "CHANGEMENT_PRIX"
        }
    }
}
