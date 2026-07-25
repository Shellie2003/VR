package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Filet global : recueille toute exception qui n'a été rattrapée nulle part, avant qu'Android ne
 * ferme l'application.
 *
 * **Pourquoi.** Jusqu'ici, un plantage ne laissait rien : l'application se fermait sèchement, le
 * gérant appelait pour dire « ça s'est arrêté », et il n'y avait aucun moyen de savoir où. Le
 * plantage à la prise de photo a coûté une enquête complète dans le code pour une cause qu'une
 * seule ligne de pile aurait désignée immédiatement. Une épicerie à Madagascar ne sait pas
 * consulter un logcat, et personne n'ira brancher son téléphone en USB.
 *
 * **Ce que ça fait.** Trois choses, dans cet ordre de priorité :
 *
 *  1. **Sauver les données.** Un travail de sauvegarde peut être en attente au moment du plantage
 *     (l'écriture disque est regroupée, avec un délai de quelques secondes). [actionUrgence] est le
 *     crochet que le ViewModel remplit pour forcer cette écriture — c'est la seule chose qui, ici,
 *     vaut plus que le diagnostic.
 *  2. **Écrire le rapport** dans un fichier : date, version, appareil, pile d'appel complète.
 *  3. **Rendre la main au gestionnaire d'origine**, pour qu'Android termine le processus
 *     normalement. Sans ça, l'application resterait figée sur un écran mort — pire que le plantage.
 *
 * Tout est enveloppé : **le filet ne doit jamais devenir lui-même la cause d'un plantage**, ni
 * retarder la fermeture au point de déclencher un « l'application ne répond pas ».
 */
object CrashReporter {

    private const val DOSSIER = "crashs"
    private const val EXTENSION = ".txt"

    /** Au-delà, les plus anciens sont effacés : ce sont des traces de dépannage, pas des archives. */
    private const val MAX_RAPPORTS = 5

    /**
     * Sauvegarde d'urgence, renseignée par le ViewModel au démarrage. Volontairement optionnelle :
     * le filet doit fonctionner même si personne ne l'a branchée.
     */
    @Volatile
    var actionUrgence: (() -> Unit)? = null

    private var installe = false

    fun installer(context: Context) {
        if (installe) return
        installe = true
        val applicatif = context.applicationContext
        val precedent = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, erreur ->
            // 1. Les données d'abord : un rapport de plantage se remplace, une journée de ventes non.
            try {
                actionUrgence?.invoke()
            } catch (e: Throwable) {
                Log.e("CrashReporter", "Sauvegarde d'urgence impossible", e)
            }

            // 2. Le diagnostic ensuite.
            try {
                ecrireRapport(applicatif, thread, erreur)
            } catch (e: Throwable) {
                Log.e("CrashReporter", "Écriture du rapport impossible", e)
            }

            // 3. La main au système, toujours : c'est lui qui doit terminer le processus.
            if (precedent != null) {
                precedent.uncaughtException(thread, erreur)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    private fun dossier(context: Context): File {
        val dossier = File(context.filesDir, DOSSIER)
        if (!dossier.exists()) dossier.mkdirs()
        return dossier
    }

    private fun ecrireRapport(context: Context, thread: Thread, erreur: Throwable) {
        val horodatage = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val pile = StringWriter().also { erreur.printStackTrace(PrintWriter(it)) }.toString()

        val versionApp = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        } catch (e: Exception) {
            "inconnue"
        }

        val contenu = buildString {
            appendLine("Varotra — rapport de plantage")
            appendLine("=================================")
            appendLine("Date        : $horodatage")
            appendLine("Version app : $versionApp")
            appendLine("Android     : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Appareil    : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Fil         : ${thread.name}")
            appendLine()
            appendLine("Cause : ${erreur.javaClass.name}: ${erreur.message}")
            appendLine()
            appendLine(pile)
        }

        File(dossier(context), "crash-$horodatage$EXTENSION").writeText(contenu)
        purger(context)
    }

    private fun purger(context: Context) {
        val fichiers = dossier(context).listFiles()?.sortedBy { it.name } ?: return
        if (fichiers.size <= MAX_RAPPORTS) return
        fichiers.take(fichiers.size - MAX_RAPPORTS).forEach { runCatching { it.delete() } }
    }

    /** Rapport le plus récent, s'il en existe un non encore proposé au gérant. */
    fun rapportEnAttente(context: Context): File? =
        try {
            dossier(context).listFiles()
                ?.filter { it.isFile && it.name.endsWith(EXTENSION) }
                ?.maxByOrNull { it.name }
        } catch (e: Exception) {
            null
        }

    /**
     * Efface les rapports. Appelé quand le gérant a fermé l'avertissement — qu'il ait partagé ou
     * non : lui reproposer le même rapport à chaque démarrage le pousserait à ignorer le message,
     * et donc à ignorer aussi le suivant, qui pourrait être important.
     */
    fun effacerRapports(context: Context) {
        try {
            dossier(context).listFiles()?.forEach { runCatching { it.delete() } }
        } catch (e: Exception) {
            Log.e("CrashReporter", "Purge impossible", e)
        }
    }

    /**
     * Copie le rapport là où le partage Android peut le lire, puis ouvre le sélecteur. Le gérant
     * l'envoie au fournisseur par WhatsApp — c'est le seul canal réaliste ici.
     */
    fun partager(context: Context, rapport: File) {
        try {
            val dossierPartage = context.getExternalFilesDir("Exports") ?: context.filesDir
            if (!dossierPartage.exists()) dossierPartage.mkdirs()
            val copie = File(dossierPartage, rapport.name)
            rapport.copyTo(copie, overwrite = true)
            ExportUtil.shareFile(context, copie, "text/plain")
        } catch (e: Exception) {
            Log.e("CrashReporter", "Partage du rapport impossible", e)
        }
    }
}
