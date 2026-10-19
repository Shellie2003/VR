package com.example.desktop.donnees

import com.example.util.FirebaseBackupManager
import com.example.util.RecoveryCode
import java.util.prefs.Preferences

/** Le résultat d'une tentative de chargement des données de la boutique. */
sealed interface ResultatChargement {
    data class Succes(val donnees: DonneesBoutique, val codeNormalise: String) : ResultatChargement

    /** Le code saisi n'a pas la forme d'un code de récupération : rien n'a été tenté. */
    object CodeInvalide : ResultatChargement

    /**
     * Le code est valide mais aucune sauvegarde n'existe sous ce chemin. Distingué du réseau et du
     * code invalide pour la raison exacte de la section 56 : un seul message pour trois situations
     * fait corriger un code qui était correct.
     */
    object AucuneSauvegarde : ResultatChargement

    /** La requête n'a pas abouti — réseau absent, coupure, serveur indisponible. */
    object EchecReseau : ResultatChargement

    /** La sauvegarde a bien été téléchargée mais n'a pas pu être interprétée. */
    object SauvegardeIllisible : ResultatChargement
}

/**
 * Charge les données réelles d'une boutique sur le poste de bureau, à partir du code de
 * récupération que le gérant possède déjà.
 *
 * ## Ce que ce service change
 *
 * Le bureau affichait un jeu de démonstration, donc il n'était qu'une maquette. Il lit désormais la
 * sauvegarde consolidée que le téléphone envoie à Firebase après chaque mutation (sections 24
 * et 40) : catalogue, ventes, trosa et retours réels, dans tous les écrans, **sans partager la base
 * de données** et donc sans toucher au fichier Room d'aucune boutique.
 *
 * ## Le code est demandé une fois, puis mémorisé
 *
 * `java.util.prefs.Preferences` plutôt qu'un fichier écrit à la main : c'est fourni par le JDK, ça
 * survit à une mise à jour du logiciel (les préférences vivent hors du dossier d'installation, donc
 * le remplacement de dossier de `MiseAJourBureau` ne les emporte pas), et il n'y a ni chemin ni
 * encodage à choisir. Le code n'est enregistré **qu'après un chargement réussi** — mémoriser une
 * saisie fautive obligerait le gérant à la corriger à chaque ouverture.
 *
 * ## Lecture seule, et dit comme tel
 *
 * Voir [SauvegardeBoutique] : ce service ne renvoie jamais rien vers Firebase. Un bureau qui
 * publierait sa propre version de la sauvegarde écraserait celle du téléphone, dont la fusion
 * suppose une base Room de part et d'autre.
 */
class ServiceDonneesBoutique(
    private val preferences: Preferences = Preferences.userRoot().node("com/example/varotra/bureau"),
    private val urlBase: String = FirebaseBackupManager.resolveDatabaseUrl("")
) {

    /** Le code mémorisé après un chargement réussi, ou `null` si le poste n'est pas encore relié. */
    fun codeEnregistre(): String? =
        runCatching { preferences.get(CLE_CODE, null)?.takeIf { it.isNotBlank() } }.getOrNull()

    /** Oublie le code : le poste repasse sur les données de démonstration. */
    fun oublierCode() {
        runCatching {
            preferences.remove(CLE_CODE)
            preferences.flush()
        }
    }

    /**
     * Télécharge et interprète la sauvegarde désignée par [codeSaisi].
     *
     * Les **deux générations** de code sont acceptées, comme côté Android (`applyRecoveryCode`,
     * section 45) : le code lisible `VRT-…` et l'UUID des boutiques activées avant son
     * introduction. Refuser l'ancien format laisserait précisément les plus anciens clients — ceux
     * qui ont le plus de données — incapables de relier leur PC.
     */
    suspend fun charger(codeSaisi: String): ResultatChargement {
        val normalise = RecoveryCode.normalize(codeSaisi)
            ?: RecoveryCode.normalizeLegacyUuid(codeSaisi)
            ?: return ResultatChargement.CodeInvalide

        val telechargement = FirebaseBackupManager.downloadBackup(urlBase, normalise)
        val corps = telechargement.getOrElse { erreur ->
            // `downloadBackup` rend le même échec pour « chemin vide » (HTTP 200 + corps « null »)
            // et pour une panne réseau. Le code HTTP permet de les séparer : un 200 signifie qu'on
            // a bien joint le serveur, donc c'est la sauvegarde qui manque — pas la connexion.
            return if (erreur.message?.contains("HTTP 200") == true) {
                ResultatChargement.AucuneSauvegarde
            } else {
                ResultatChargement.EchecReseau
            }
        }

        val donnees = SauvegardeBoutique.lire(corps)
            ?: return ResultatChargement.SauvegardeIllisible

        runCatching {
            preferences.put(CLE_CODE, normalise)
            preferences.flush()
        }
        return ResultatChargement.Succes(donnees, normalise)
    }

    private companion object {
        const val CLE_CODE = "code_recuperation"
    }
}
