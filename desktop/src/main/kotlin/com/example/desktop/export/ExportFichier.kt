package com.example.desktop.export

import com.example.util.Tableau
import com.example.util.TableauExport
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Le résultat d'un export, pour que l'écran puisse dire ce qui s'est passé. */
sealed interface ResultatExport {
    /** Le fichier est écrit. [chemin] est ce qu'on montre au gérant, tel quel. */
    data class Reussi(val fichier: File, val chemin: String) : ResultatExport

    /** Il n'y avait rien à exporter : on n'écrit pas un fichier vide sans le dire. */
    object ListeVide : ResultatExport

    /** L'écriture a échoué, avec son motif technique pour le journal. */
    data class Echec(val motif: String) : ResultatExport
}

/**
 * Écrit un tableau d'export dans un fichier, sur le disque du PC.
 *
 * ## Pourquoi l'export compte plus sur un PC que sur le téléphone
 *
 * Sur un téléphone, un CSV part par WhatsApp et finit rarement ouvert. Sur un PC, il s'ouvre d'un
 * double-clic dans Excel, se range dans un dossier, s'envoie au comptable en pièce jointe. C'est
 * l'une des rares choses que la version de bureau fait **mieux** que le téléphone, et non
 * simplement « aussi ».
 *
 * ## Pas de sélecteur de fichier, et c'est un choix
 *
 * Le fichier va toujours dans `Documents/Varotra` (ou le dossier personnel si `Documents`
 * n'existe pas). Un sélecteur de fichier obligerait le gérant à choisir un dossier et un nom à
 * chaque export, donc à se souvenir de l'endroit — et la plainte la plus courante sur les exports
 * est « je ne retrouve pas le fichier ». Un dossier unique, toujours le même, annoncé après chaque
 * export, se retient une fois.
 *
 * Le nom du fichier porte la **date et l'heure** plutôt qu'un horodatage en millisecondes comme sur
 * Android : `ventes_2026-01-15_14h30.csv` se trie et se reconnaît, `historique_ventes_1737...csv`
 * non.
 *
 * ## Le contenu vient de `TableauExport`
 *
 * Colonnes, ordre et formatage sont ceux d'Android, décrits une seule fois dans `:core:logic` : un
 * gérant qui exporte depuis son téléphone et depuis son PC obtient le même fichier.
 */
object ExportFichier {

    private val formatHorodatage = SimpleDateFormat("yyyy-MM-dd_HH'h'mm", Locale.FRANCE)

    /** Le dossier d'export, créé au besoin. */
    fun dossier(): File {
        val maison = File(System.getProperty("user.home") ?: ".")
        val documents = File(maison, "Documents")
        val racine = if (documents.isDirectory) documents else maison
        return File(racine, "Varotra")
    }

    /**
     * Écrit [tableau] en CSV et renvoie ce qui s'est passé.
     *
     * **Une liste vide ne produit pas de fichier.** Écrire un CSV qui ne contient que ses en-têtes
     * laisserait le gérant chercher ses données dans un fichier qui n'en a pas, et conclure que
     * l'export est cassé plutôt que de comprendre que son filtre ne retenait rien.
     */
    fun ecrireCsv(tableau: Tableau): ResultatExport {
        if (tableau.lignes.isEmpty()) return ResultatExport.ListeVide
        return try {
            val dossier = dossier()
            if (!dossier.isDirectory && !dossier.mkdirs()) {
                return ResultatExport.Echec("Dossier non créé : ${dossier.path}")
            }
            val horodatage = formatHorodatage.format(Date())
            val fichier = File(dossier, "${tableau.nomFichier}_$horodatage.csv")
            // `writeText` en UTF-8 (le défaut de Kotlin) : le BOM que `versCsv` place en tête n'a
            // de sens que si le fichier est bien écrit en UTF-8.
            fichier.writeText(TableauExport.versCsv(tableau))
            ResultatExport.Reussi(fichier, fichier.path)
        } catch (e: Exception) {
            ResultatExport.Echec(e.message ?: e.javaClass.simpleName)
        }
    }

    /**
     * Ouvre le dossier d'export dans l'explorateur de fichiers du système.
     *
     * Enveloppé et silencieux en cas d'échec : `Desktop` n'est pas disponible partout (session sans
     * interface, machine sans explorateur), et le gérant a de toute façon le chemin écrit à
     * l'écran. Un bouton de confort ne doit jamais pouvoir fermer le logiciel.
     */
    fun ouvrirDossier(): Boolean = runCatching {
        if (!Desktop.isDesktopSupported()) return false
        val bureau = Desktop.getDesktop()
        if (!bureau.isSupported(Desktop.Action.OPEN)) return false
        val dossier = dossier()
        if (!dossier.isDirectory) return false
        bureau.open(dossier)
        true
    }.getOrDefault(false)
}
