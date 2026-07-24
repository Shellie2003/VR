package com.example.util

import com.example.data.model.AuditLog
import com.example.data.model.CaisseSession
import com.example.data.model.MouvementCaisse
import com.example.data.model.Product
import com.example.data.model.Retour
import com.example.data.model.Sale
import java.util.Calendar

/**
 * Détection des anomalies « anticipables » dans une épicerie : les gestes qui, pris isolément,
 * peuvent être parfaitement légitimes, mais dont la répétition ou l'ampleur trahit une fuite
 * d'argent ou de marchandise.
 *
 * Volontairement SANS dépendance Android : c'est une fonction pure (données -> alertes), donc
 * testable et réutilisable telle quelle dans un rapport PDF comme à l'écran.
 *
 * Le détecteur ne bloque jamais rien et n'accuse personne : il classe (INFO / ATTENTION / ALERTE)
 * et donne au gérant le contexte exact — quel appareil, quel employé, quel montant, quand.
 */
object AnomalyDetector {

    // --- Seuils. Volontairement conservateurs pour éviter de noyer le gérant sous les alertes. ---

    /** Un manquant de caisse au-delà de ce montant (Ar) est signalé, même isolé. */
    private const val SEUIL_MANQUANT_CAISSE = 2_000.0

    /** Nombre de fermetures avec manquant, sur la période, à partir duquel on parle de récurrence. */
    private const val SEUIL_MANQUANTS_RECURRENTS = 3

    /** Nombre de retours sur la période, pour un même appareil, jugé inhabituel. */
    private const val SEUIL_RETOURS_FREQUENTS = 5

    /** Total des sorties de caisse (Ar) sur la période au-delà duquel on alerte. */
    private const val SEUIL_SORTIES_CAISSE = 100_000.0

    /** Marge (en %) en dessous de laquelle une vente est considérée comme « à perte ». */
    private const val SEUIL_MARGE_NEGATIVE_PCT = 0.0

    /** Plage horaire considérée comme « hors ouverture » pour une épicerie. */
    private const val HEURE_NUIT_DEBUT = 22
    private const val HEURE_NUIT_FIN = 5

    const val SEVERITE_INFO = AuditLog.SEVERITE_INFO
    const val SEVERITE_ATTENTION = AuditLog.SEVERITE_ATTENTION
    const val SEVERITE_ALERTE = AuditLog.SEVERITE_ALERTE

    data class Anomalie(
        val code: String,
        val titre: String,
        val description: String,
        val severite: String,
        /** Appareil / employé concerné, vide quand l'anomalie est globale. */
        val responsable: String = "",
        val montant: Double = 0.0,
        val timestamp: Long = System.currentTimeMillis(),
        /** Nombre d'occurrences regroupées dans cette anomalie. */
        val occurrences: Int = 1
    )

    /**
     * Analyse la période [depuis] -> maintenant et retourne les anomalies, les plus graves
     * d'abord. Toutes les listes reçues peuvent contenir des enregistrements hors période :
     * le filtrage est fait ici.
     */
    fun analyser(
        sales: List<Sale>,
        retours: List<Retour>,
        mouvements: List<MouvementCaisse>,
        sessions: List<CaisseSession>,
        auditLogs: List<AuditLog>,
        products: List<Product>,
        depuis: Long,
        lang: String = "fr"
    ): List<Anomalie> {
        val anomalies = mutableListOf<Anomalie>()
        val salesPeriode = sales.filter { it.timestamp >= depuis }
        val retoursPeriode = retours.filter { it.timestamp >= depuis }
        val mouvementsPeriode = mouvements.filter { it.date >= depuis }
        val sessionsPeriode = sessions.filter { (it.dateFermeture ?: it.dateOuverture) >= depuis }
        val logsPeriode = auditLogs.filter { it.timestamp >= depuis }

        anomalies += detecterSuppressions(logsPeriode, lang)
        anomalies += detecterTentativesRefusees(logsPeriode, lang)
        anomalies += detecterEcartsCaisse(sessionsPeriode, lang)
        anomalies += detecterSortiesCaisse(mouvementsPeriode, lang)
        anomalies += detecterRetoursFrequents(retoursPeriode, lang)
        anomalies += detecterVentesAPerte(salesPeriode, products, lang)
        anomalies += detecterVentesNocturnes(salesPeriode, lang)

        val ordreSeverite = mapOf(SEVERITE_ALERTE to 0, SEVERITE_ATTENTION to 1, SEVERITE_INFO to 2)
        return anomalies.sortedWith(
            compareBy({ ordreSeverite[it.severite] ?: 3 }, { -it.timestamp })
        )
    }

    /**
     * Toute suppression d'historique est signalée : c'est le geste qui efface la preuve d'un
     * encaissement, donc celui qu'un gérant doit voir en premier. Regroupé par auteur.
     */
    private fun detecterSuppressions(logs: List<AuditLog>, lang: String): List<Anomalie> {
        val suppressions = logs.filter {
            !it.bloque && it.type in setOf(
                AuditLog.Companion.Type.SUPPRESSION_VENTE,
                AuditLog.Companion.Type.SUPPRESSION_DETTE,
                AuditLog.Companion.Type.SUPPRESSION_MOUVEMENT_CAISSE,
                AuditLog.Companion.Type.SUPPRESSION_RETOUR,
                AuditLog.Companion.Type.SUPPRESSION_REAPPRO,
                AuditLog.Companion.Type.PURGE_JOURNAL
            )
        }
        if (suppressions.isEmpty()) return emptyList()

        return suppressions.groupBy { responsableDe(it) }.map { (responsable, group) ->
            val montantTotal = group.sumOf { it.montant }
            Anomalie(
                code = "SUPPRESSIONS_HISTORIQUE",
                titre = when (lang) {
                    "mg" -> "Nisy tantara nofafana"
                    "en" -> "History records deleted"
                    else -> "Historique supprimé"
                },
                description = when (lang) {
                    "mg" -> "$responsable: fafana ${group.size}. Vola voakasika: ${FormatUtil.formatPrice(montantTotal)} Ar."
                    "en" -> "$responsable deleted ${group.size} record(s), totalling ${FormatUtil.formatPrice(montantTotal)} Ar."
                    else -> "$responsable a supprimé ${group.size} enregistrement(s), pour un total de ${FormatUtil.formatPrice(montantTotal)} Ar."
                },
                severite = SEVERITE_ALERTE,
                responsable = responsable,
                montant = montantTotal,
                timestamp = group.maxOf { it.timestamp },
                occurrences = group.size
            )
        }
    }

    /**
     * Une tentative refusée n'a rien coûté à l'épicerie, mais elle révèle une intention : un
     * employé qui essaie d'effacer une vente doit être vu, même si le garde-fou a tenu.
     */
    private fun detecterTentativesRefusees(logs: List<AuditLog>, lang: String): List<Anomalie> {
        val tentatives = logs.filter { it.bloque }
        if (tentatives.isEmpty()) return emptyList()

        return tentatives.groupBy { responsableDe(it) }.map { (responsable, group) ->
            Anomalie(
                code = "TENTATIVES_REFUSEES",
                titre = when (lang) {
                    "mg" -> "Fanandramana nosakanana"
                    "en" -> "Blocked attempts"
                    else -> "Tentatives bloquées"
                },
                description = when (lang) {
                    "mg" -> "$responsable: nanandrana nanao zavatra tsy azony atao in-${group.size}."
                    "en" -> "$responsable attempted ${group.size} manager-only action(s)."
                    else -> "$responsable a tenté ${group.size} action(s) réservée(s) au gérant."
                },
                severite = if (group.size >= 3) SEVERITE_ALERTE else SEVERITE_ATTENTION,
                responsable = responsable,
                timestamp = group.maxOf { it.timestamp },
                occurrences = group.size
            )
        }
    }

    /**
     * Manquant de caisse : la différence entre l'argent compté et l'argent théorique. Un manquant
     * isolé peut être une erreur de rendu de monnaie ; des manquants répétés sur le même poste,
     * beaucoup moins.
     */
    private fun detecterEcartsCaisse(sessions: List<CaisseSession>, lang: String): List<Anomalie> {
        val anomalies = mutableListOf<Anomalie>()
        val manquants = sessions.filter { (it.ecart ?: 0.0) <= -SEUIL_MANQUANT_CAISSE }

        manquants.forEach { session ->
            val manque = -(session.ecart ?: 0.0)
            anomalies += Anomalie(
                code = "MANQUANT_CAISSE",
                titre = when (lang) {
                    "mg" -> "Tsy ampy ny vola am-pandraisana"
                    "en" -> "Cash shortfall at closing"
                    else -> "Manquant en caisse"
                },
                description = when (lang) {
                    "mg" -> "Tsy ampy ${FormatUtil.formatPrice(manque)} Ar tamin'ny fanakatonana."
                    "en" -> "${FormatUtil.formatPrice(manque)} Ar missing at session closing."
                    else -> "Il manque ${FormatUtil.formatPrice(manque)} Ar à la fermeture de caisse."
                },
                severite = SEVERITE_ALERTE,
                montant = manque,
                timestamp = session.dateFermeture ?: session.dateOuverture
            )
        }

        if (manquants.size >= SEUIL_MANQUANTS_RECURRENTS) {
            val total = manquants.sumOf { -(it.ecart ?: 0.0) }
            anomalies += Anomalie(
                code = "MANQUANTS_RECURRENTS",
                titre = when (lang) {
                    "mg" -> "Tsy fahampiam-bola miverimberina"
                    "en" -> "Recurring cash shortfalls"
                    else -> "Manquants de caisse récurrents"
                },
                description = when (lang) {
                    "mg" -> "In-${manquants.size} no tsy ampy ny vola, mitotaly ${FormatUtil.formatPrice(total)} Ar."
                    "en" -> "${manquants.size} closings ended short, totalling ${FormatUtil.formatPrice(total)} Ar."
                    else -> "${manquants.size} fermetures en manque, pour un total de ${FormatUtil.formatPrice(total)} Ar."
                },
                severite = SEVERITE_ALERTE,
                montant = total,
                timestamp = manquants.maxOf { it.dateFermeture ?: it.dateOuverture },
                occurrences = manquants.size
            )
        }
        return anomalies
    }

    /** Les sorties de caisse sont le canal le plus simple pour faire sortir de l'argent « pour un motif ». */
    private fun detecterSortiesCaisse(mouvements: List<MouvementCaisse>, lang: String): List<Anomalie> {
        val sorties = mouvements.filter { it.type == "SORTIE" }
        if (sorties.isEmpty()) return emptyList()

        return sorties.groupBy { it.deviceName.ifBlank { "?" } }
            .mapNotNull { (device, group) ->
                val total = group.sumOf { it.montant }
                if (total < SEUIL_SORTIES_CAISSE) return@mapNotNull null
                Anomalie(
                    code = "SORTIES_CAISSE_ELEVEES",
                    titre = when (lang) {
                        "mg" -> "Vola navoaka be loatra"
                        "en" -> "High cash withdrawals"
                        else -> "Sorties de caisse élevées"
                    },
                    description = when (lang) {
                        "mg" -> "$device: navoaka ${FormatUtil.formatPrice(total)} Ar (in-${group.size})."
                        "en" -> "$device withdrew ${FormatUtil.formatPrice(total)} Ar in ${group.size} movement(s)."
                        else -> "$device a sorti ${FormatUtil.formatPrice(total)} Ar en ${group.size} mouvement(s)."
                    },
                    severite = SEVERITE_ATTENTION,
                    responsable = device,
                    montant = total,
                    timestamp = group.maxOf { it.date },
                    occurrences = group.size
                )
            }
    }

    /**
     * Le faux retour est la fraude classique en caisse : encaisser normalement, puis enregistrer
     * un « retour » et garder l'argent. Un poste qui en enregistre beaucoup plus que les autres
     * mérite un contrôle.
     */
    private fun detecterRetoursFrequents(retours: List<Retour>, lang: String): List<Anomalie> {
        if (retours.isEmpty()) return emptyList()

        return retours.groupBy { it.deviceName.ifBlank { "?" } }
            .mapNotNull { (device, group) ->
                if (group.size < SEUIL_RETOURS_FREQUENTS) return@mapNotNull null
                val total = group.sumOf { it.totalAmount }
                Anomalie(
                    code = "RETOURS_FREQUENTS",
                    titre = when (lang) {
                        "mg" -> "Famerenam-bola matetika"
                        "en" -> "Frequent refunds"
                        else -> "Retours fréquents"
                    },
                    description = when (lang) {
                        "mg" -> "$device: famerenana in-${group.size}, ${FormatUtil.formatPrice(total)} Ar."
                        "en" -> "$device recorded ${group.size} refunds totalling ${FormatUtil.formatPrice(total)} Ar."
                        else -> "$device a enregistré ${group.size} retours pour ${FormatUtil.formatPrice(total)} Ar."
                    },
                    severite = SEVERITE_ATTENTION,
                    responsable = device,
                    montant = total,
                    timestamp = group.maxOf { it.timestamp },
                    occurrences = group.size
                )
            }
    }

    /**
     * Vente en dessous du prix d'achat : soit une erreur de saisie, soit un article « bradé » à un
     * proche. Dans les deux cas le gérant doit le savoir — c'est de la marge qui disparaît.
     */
    private fun detecterVentesAPerte(sales: List<Sale>, products: List<Product>, lang: String): List<Anomalie> {
        if (sales.isEmpty() || products.isEmpty()) return emptyList()
        val coutParProduit = products.associate { it.id to it.prixAchatUniteBase }

        data class Perte(val nom: String, val perte: Double, val timestamp: Long, val device: String)

        val pertes = mutableListOf<Perte>()
        sales.forEach { sale ->
            sale.items.forEach { item ->
                val cout = coutParProduit[item.productId] ?: 0.0
                if (cout > 0.0 && item.price < cout) {
                    val margePct = ((item.price - cout) / cout) * 100.0
                    if (margePct < SEUIL_MARGE_NEGATIVE_PCT) {
                        pertes += Perte(
                            nom = item.name,
                            perte = (cout - item.price) * item.quantity,
                            timestamp = sale.timestamp,
                            device = sale.deviceName.ifBlank { "?" }
                        )
                    }
                }
            }
        }
        if (pertes.isEmpty()) return emptyList()

        return pertes.groupBy { it.device }.map { (device, group) ->
            val totalPerte = group.sumOf { it.perte }
            val articles = group.map { it.nom }.distinct().take(3).joinToString(", ")
            Anomalie(
                code = "VENTES_A_PERTE",
                titre = when (lang) {
                    "mg" -> "Varotra latsaky ny vidiny"
                    "en" -> "Items sold below cost"
                    else -> "Ventes en dessous du prix d'achat"
                },
                description = when (lang) {
                    "mg" -> "$device: $articles… fatiantoka ${FormatUtil.formatPrice(totalPerte)} Ar (in-${group.size})."
                    "en" -> "$device sold $articles… below cost, losing ${FormatUtil.formatPrice(totalPerte)} Ar over ${group.size} line(s)."
                    else -> "$device a vendu $articles… sous le prix d'achat : ${FormatUtil.formatPrice(totalPerte)} Ar de perte sur ${group.size} ligne(s)."
                },
                severite = SEVERITE_ATTENTION,
                responsable = device,
                montant = totalPerte,
                timestamp = group.maxOf { it.timestamp },
                occurrences = group.size
            )
        }
    }

    /** Encaissements enregistrés en pleine nuit, alors que la boutique est fermée. */
    private fun detecterVentesNocturnes(sales: List<Sale>, lang: String): List<Anomalie> {
        val nocturnes = sales.filter { estNocturne(it.timestamp) }
        if (nocturnes.isEmpty()) return emptyList()

        return nocturnes.groupBy { it.deviceName.ifBlank { "?" } }.map { (device, group) ->
            val total = group.sumOf { it.totalAmount }
            Anomalie(
                code = "VENTES_NOCTURNES",
                titre = when (lang) {
                    "mg" -> "Varotra amin'ny alina"
                    "en" -> "Sales outside opening hours"
                    else -> "Ventes hors horaires"
                },
                description = when (lang) {
                    "mg" -> "$device: varotra in-${group.size} teo anelanelan'ny ${HEURE_NUIT_DEBUT}h sy ny ${HEURE_NUIT_FIN}h (${FormatUtil.formatPrice(total)} Ar)."
                    "en" -> "$device recorded ${group.size} sale(s) between ${HEURE_NUIT_DEBUT}:00 and ${HEURE_NUIT_FIN}:00 (${FormatUtil.formatPrice(total)} Ar)."
                    else -> "$device a enregistré ${group.size} vente(s) entre ${HEURE_NUIT_DEBUT}h et ${HEURE_NUIT_FIN}h (${FormatUtil.formatPrice(total)} Ar)."
                },
                severite = SEVERITE_INFO,
                responsable = device,
                montant = total,
                timestamp = group.maxOf { it.timestamp },
                occurrences = group.size
            )
        }
    }

    private fun estNocturne(timestamp: Long): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val heure = cal.get(Calendar.HOUR_OF_DAY)
        return heure >= HEURE_NUIT_DEBUT || heure < HEURE_NUIT_FIN
    }

    /** Libellé lisible de l'auteur d'une trace : l'employé s'il est connu, sinon l'appareil. */
    private fun responsableDe(log: AuditLog): String = when {
        log.utilisateur.isNotBlank() && log.deviceName.isNotBlank() -> "${log.utilisateur} (${log.deviceName})"
        log.utilisateur.isNotBlank() -> log.utilisateur
        log.deviceName.isNotBlank() -> log.deviceName
        else -> "?"
    }
}
