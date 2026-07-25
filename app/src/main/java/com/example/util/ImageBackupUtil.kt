package com.example.util

import android.content.Context
import android.util.Base64
import android.util.Log
import java.io.File

/**
 * Embeds locally-stored product photos (taken via camera or picked from the gallery, saved as
 * "file://..." under the app's private storage) directly as base64 inside the JSON backup blob,
 * so they survive a fresh install / app data wipe / device change exactly like the rest of the
 * data — without needing any paid image hosting. Remote "http(s)://" images (e.g. from Open Food
 * Facts) are already durably hosted elsewhere and are left untouched.
 *
 * Product photos are downsampled to a modest resolution before being saved locally (see
 * [PhotoStore]), which keeps this comfortably small even for a large catalog.
 *
 * Ce chemin base64 ne sert plus qu'à la **synchronisation entre appareils** (Wi-Fi local, gratuit).
 * Les sauvegardes Firebase excluent les photos depuis F1 — voir [ArchiveUtil] pour la répartition
 * entre ce qui part sur Firebase (les références seules) et ce qui embarque les photos (l'archive
 * zip de partage et de conservation).
 */
object ImageBackupUtil {
    private const val LOCAL_FILE_PREFIX = "file://"

    fun isLocalImage(imageUrl: String?): Boolean =
        !imageUrl.isNullOrBlank() && imageUrl.startsWith(LOCAL_FILE_PREFIX)

    /**
     * Returns the base64-encoded bytes of a local product image, or null if remote/missing.
     *
     * La résolution passe par [PhotoStore] : le chemin absolu enregistré en base peut désigner le
     * dossier d'un autre téléphone (produit venu d'une archive restaurée), auquel cas seule la
     * recherche par nom de fichier retrouve la photo. Sans ça, une synchronisation P2P après
     * restauration renverrait des produits sans image.
     */
    fun encodeLocalImage(context: Context, imageUrl: String?): String? {
        if (!isLocalImage(imageUrl)) return null
        return try {
            val file = PhotoStore.resoudre(context, imageUrl) ?: return null
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("ImageBackupUtil", "Failed to encode image for backup: $imageUrl", e)
            null
        }
    }

    /**
     * Recreates a local product image file from its backed-up base64 data, if it's missing.
     *
     * Le fichier est réécrit dans le dossier photos sous son nom d'origine — jamais au chemin
     * absolu reçu, qui peut appartenir à un autre appareil (voire, sur une sauvegarde bricolée,
     * pointer hors du bac à sable de l'application).
     */
    fun restoreLocalImageIfMissing(context: Context, imageUrl: String?, base64Data: String?) {
        if (!isLocalImage(imageUrl) || base64Data.isNullOrBlank()) return
        if (PhotoStore.resoudre(context, imageUrl) != null) return
        try {
            val nom = PhotoStore.nomFichier(imageUrl) ?: return
            val cible = File(PhotoStore.dossier(context), nom)
            cible.writeBytes(Base64.decode(base64Data, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.e("ImageBackupUtil", "Failed to restore image from backup: $imageUrl", e)
        }
    }
}
