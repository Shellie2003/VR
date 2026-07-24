package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Import et lecture du logo de l'épicerie, utilisé dans tous les PDF produits par l'app (reçus de
 * caisse, rapports, exports).
 *
 * Le logo choisi par le gérant dans la galerie est recopié dans le stockage privé de l'app : une
 * URI de galerie n'est pas lisible durablement (permission révoquée au redémarrage, fichier
 * déplacé ou supprimé par l'utilisateur), alors qu'un reçu doit pouvoir être réimprimé des mois
 * plus tard. Il est aussi redimensionné, parce qu'une photo de 12 Mpx dans l'en-tête d'un ticket
 * de 80 mm ne sert à rien et alourdirait chaque PDF.
 */
object ShopLogoUtil {

    private const val LOGO_FILE_NAME = "shop_logo.png"

    /** Côté maximal du logo stocké. Large pour un rapport A4, minuscule en poids. */
    private const val MAX_DIMENSION = 512

    /**
     * Copie l'image désignée par [uri] dans le stockage privé et retourne le chemin absolu du
     * fichier créé, ou null si l'image est illisible.
     */
    fun importerLogo(context: Context, uri: Uri): String? {
        return try {
            val source = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null

            val redimensionne = redimensionner(source)
            val destination = File(context.filesDir, LOGO_FILE_NAME)
            FileOutputStream(destination).use { output ->
                redimensionne.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            if (redimensionne !== source) redimensionne.recycle()
            source.recycle()
            destination.absolutePath
        } catch (e: Exception) {
            Log.e("ShopLogoUtil", "Import du logo impossible", e)
            null
        }
    }

    /** Charge le logo enregistré, ou null si aucun logo n'a été importé (cas par défaut). */
    fun chargerLogo(cheminLogo: String?): Bitmap? {
        if (cheminLogo.isNullOrBlank()) return null
        return try {
            val file = File(cheminLogo)
            if (!file.exists()) return null
            BitmapFactory.decodeFile(cheminLogo)
        } catch (e: Exception) {
            Log.e("ShopLogoUtil", "Lecture du logo impossible: $cheminLogo", e)
            null
        }
    }

    fun supprimerLogo(cheminLogo: String?) {
        if (cheminLogo.isNullOrBlank()) return
        try {
            File(cheminLogo).takeIf { it.exists() }?.delete()
        } catch (e: Exception) {
            Log.e("ShopLogoUtil", "Suppression du logo impossible", e)
        }
    }

    private fun redimensionner(source: Bitmap): Bitmap {
        val plusGrandCote = maxOf(source.width, source.height)
        if (plusGrandCote <= MAX_DIMENSION) return source
        val ratio = MAX_DIMENSION.toFloat() / plusGrandCote
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
