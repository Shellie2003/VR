package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {
    private const val BACKUP_FILE_NAME = "database_safety_backup.json"

    /** Dossier public, sous Téléchargements, qui survit à la désinstallation. */
    const val DOSSIER_PUBLIC = "Varotra"
    const val MIME_ARCHIVE = "application/zip"

    fun saveBackup(context: Context, json: String) {
        try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun readBackup(context: Context): String? {
        return try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasBackup(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, BACKUP_FILE_NAME)
            file.exists() && file.length() > 20
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Prépare l'archive **complète** (données + photos) à envoyer par le partage Android —
     * WhatsApp, Bluetooth, Drive, courriel, carte SD — pour les gérants qui n'ont ni projet
     * Firebase ni adresse mail. Renvoie null s'il n'y a pas encore de sauvegarde.
     *
     * L'archive part de la sauvegarde de sécurité locale, qui est réécrite après chaque mutation :
     * elle est donc toujours à jour au moment du partage, sans re-sérialiser la base ici.
     */
    fun creerArchivePartageable(context: Context): File? {
        return try {
            val json = readBackup(context)
            if (json.isNullOrBlank()) return null
            val dossier = context.getExternalFilesDir("Exports") ?: context.filesDir
            if (!dossier.exists()) dossier.mkdirs()
            ArchiveUtil.creerArchive(context, json, File(dossier, nomArchive()))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copie l'archive dans `Téléchargements/Varotra`, **le seul endroit qui survit à la
     * désinstallation de l'application**.
     *
     * Android efface intégralement le stockage privé d'une application désinstallée : ni la base de
     * données, ni les photos, ni la sauvegarde locale n'en réchappent — c'est le système qui
     * l'impose, aucune application ne peut s'y soustraire. Déposer l'archive dans le dossier public
     * Téléchargements est donc la seule façon qu'un gérant retrouve ses données après avoir
     * désinstallé, changé de téléphone ou réinitialisé l'appareil. Le fichier y reste visible
     * depuis n'importe quel gestionnaire de fichiers, et se recharge par « Importer une sauvegarde ».
     *
     * Aucune autorisation de stockage n'est demandée : MediaStore autorise une application à écrire
     * dans Téléchargements depuis Android 10. Sous Android 9 et antérieur, on retombe sur un chemin
     * direct, qui y était encore permis.
     */
    fun deposerDansTelechargements(context: Context, archive: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val valeurs = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, archive.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, MIME_ARCHIVE)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/" + DOSSIER_PUBLIC
                    )
                }
                val resolveur = context.contentResolver
                val uri = resolveur.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valeurs)
                    ?: return false
                resolveur.openOutputStream(uri)?.use { sortie ->
                    archive.inputStream().use { it.copyTo(sortie) }
                } ?: return false
            } else {
                // Android 9 et antérieur : écriture directe, qui exige WRITE_EXTERNAL_STORAGE.
                // On vérifie AVANT d'écrire plutôt que de laisser l'exception décider : sans ce
                // contrôle la fonction renvoyait false sur une exception, l'appelant l'ignorait,
                // et le gérant s'entendait annoncer une sauvegarde qui n'existait pas — sur la
                // seule fonction dont l'intérêt est justement de survivre à la désinstallation.
                if (!autorisationStockageAncienneAccordee(context)) return false
                val dossier = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    DOSSIER_PUBLIC
                )
                if (!dossier.exists() && !dossier.mkdirs()) return false
                archive.copyTo(File(dossier, archive.name), overwrite = true)
            }
            true
        } catch (e: Exception) {
            Log.e("BackupHelper", "Dépôt dans Téléchargements impossible", e)
            false
        }
    }

    /** Chemin affiché au gérant pour qu'il sache où chercher son fichier. */
    fun cheminPublicLisible(): String = "Téléchargements/$DOSSIER_PUBLIC"

    /**
     * Vrai si le dépôt public exige encore une autorisation à demander (Android 9 et antérieur) et
     * qu'elle n'est pas accordée. L'appelant s'en sert pour la réclamer au bon moment — au clic sur
     * « Partager » — plutôt qu'au démarrage, où le gérant ne comprendrait pas pourquoi.
     */
    fun autorisationStockageRequise(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !autorisationStockageAncienneAccordee(context)

    private fun autorisationStockageAncienneAccordee(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    private fun nomArchive(): String {
        val jour = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
        return "varotra-sauvegarde-$jour.zip"
    }
}
