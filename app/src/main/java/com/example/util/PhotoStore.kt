package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Emplacement unique des photos produits, et seul endroit qui sait retrouver le fichier d'une photo
 * à partir de ce qui est stocké en base.
 *
 * **Pourquoi ce fichier existe.** Les photos étaient écrites en vrac à la racine de `filesDir`, et
 * la base gardait leur **chemin absolu** (`file:///data/user/0/<paquet>/files/product_img_123.jpg`).
 * Deux conséquences : après une restauration venue d'un autre téléphone le chemin ne désignait plus
 * rien, et rien ne permettait de rassembler « les photos » pour les joindre à une sauvegarde. Ici,
 * les photos vivent dans un sous-dossier dédié et [resoudre] retombe sur le **nom** du fichier quand
 * le chemin absolu ne répond plus — c'est ce qui rend une archive transportable d'un appareil à
 * l'autre.
 *
 * La référence écrite en base reste `file://<chemin absolu>` : les produits enregistrés par les
 * versions précédentes continuent d'être lus sans migration.
 */
object PhotoStore {

    /** Sous-dossier des photos, à l'intérieur du stockage privé de l'application. */
    const val DOSSIER = "photos"

    private const val PREFIXE_FICHIER = "product_img_"
    private const val PREFIXE_REFERENCE = "file://"

    /**
     * Les photos sont volontairement réduites : elles voyagent dans les archives de sauvegarde, et
     * une épicerie de 200 produits photographiés en pleine résolution produirait une archive de
     * plusieurs centaines de mégaoctets — intransmissible par WhatsApp sur un forfait malgache.
     */
    private const val DIMENSION_MAX = 1024
    private const val QUALITE_JPEG = 82

    fun dossier(context: Context): File {
        val dossier = File(context.filesDir, DOSSIER)
        if (!dossier.exists()) dossier.mkdirs()
        return dossier
    }

    fun nouveauFichier(context: Context): File =
        File(dossier(context), "$PREFIXE_FICHIER${System.currentTimeMillis()}.jpg")

    /** URI à donner à l'appareil photo pour qu'il écrive directement dans notre fichier. */
    fun uriPartageable(context: Context, fichier: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)

    /** Forme stockée dans `Product.imageUrl`. */
    fun reference(fichier: File): String = PREFIXE_REFERENCE + fichier.absolutePath

    fun estLocale(imageUrl: String?): Boolean =
        !imageUrl.isNullOrBlank() && imageUrl.startsWith(PREFIXE_REFERENCE)

    fun nomFichier(imageUrl: String?): String? {
        if (!estLocale(imageUrl)) return null
        return imageUrl!!.removePrefix(PREFIXE_REFERENCE)
            .substringAfterLast('/')
            .takeIf { it.isNotBlank() }
    }

    /**
     * Retrouve le fichier d'une photo, ou null s'il a disparu.
     *
     * Trois tentatives, dans cet ordre : le chemin absolu enregistré (cas courant), le nom du
     * fichier dans le dossier photos (cas d'une archive restaurée depuis un autre téléphone), puis
     * le nom du fichier à la racine de `filesDir` (photos prises par les versions précédentes).
     */
    fun resoudre(context: Context, imageUrl: String?): File? {
        if (!estLocale(imageUrl)) return null
        val chemin = imageUrl!!.removePrefix(PREFIXE_REFERENCE)
        val direct = File(chemin)
        if (direct.exists() && direct.length() > 0) return direct

        val nom = nomFichier(imageUrl) ?: return null
        val dansDossier = File(dossier(context), nom)
        if (dansDossier.exists() && dansDossier.length() > 0) return dansDossier

        val historique = File(context.filesDir, nom)
        return if (historique.exists() && historique.length() > 0) historique else null
    }

    /** Toutes les photos présentes sur l'appareil, dossier dédié + emplacement historique. */
    fun toutes(context: Context): List<File> {
        val dansDossier = dossier(context).listFiles()?.filter { it.isFile } ?: emptyList()
        val historiques = context.filesDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith(PREFIXE_FICHIER) }
            ?: emptyList()
        // Le dossier dédié fait autorité si les deux emplacements portent le même nom.
        val nomsPris = dansDossier.map { it.name }.toSet()
        return dansDossier + historiques.filterNot { it.name in nomsPris }
    }

    /** Écrit un bitmap (réduit) dans un nouveau fichier photo. Renvoie null en cas d'échec. */
    fun enregistrer(context: Context, bitmap: Bitmap): File? = try {
        val fichier = nouveauFichier(context)
        FileOutputStream(fichier).use { sortie ->
            reduire(bitmap).compress(Bitmap.CompressFormat.JPEG, QUALITE_JPEG, sortie)
        }
        fichier
    } catch (e: Exception) {
        Log.e("PhotoStore", "Échec d'enregistrement d'une photo", e)
        null
    }

    /** Copie et réduit une image choisie dans la galerie. */
    fun enregistrerDepuisUri(context: Context, uri: Uri): File? = try {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { entree ->
            BitmapFactory.decodeStream(entree)
        }
        if (bitmap == null) null else enregistrer(context, bitmap)
    } catch (e: Exception) {
        Log.e("PhotoStore", "Échec de lecture de l'image choisie", e)
        null
    }

    /**
     * Réduit sur place la photo que l'appareil photo vient d'écrire en pleine résolution.
     *
     * `inSampleSize` est calculé avant le décodage : décoder d'abord une photo de 12 mégapixels
     * pour la réduire ensuite demanderait ~48 Mo d'un coup, ce qui suffit à faire tomber
     * l'application sur un téléphone d'entrée de gamme.
     */
    fun reduireApresCapture(fichier: File): Boolean = try {
        val mesure = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichier.absolutePath, mesure)
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculerEchantillon(mesure.outWidth, mesure.outHeight)
        }
        val bitmap = BitmapFactory.decodeFile(fichier.absolutePath, options)
        if (bitmap == null) {
            false
        } else {
            FileOutputStream(fichier).use { sortie ->
                reduire(bitmap).compress(Bitmap.CompressFormat.JPEG, QUALITE_JPEG, sortie)
            }
            true
        }
    } catch (e: Exception) {
        Log.e("PhotoStore", "Échec de réduction de la photo capturée", e)
        false
    }

    private fun calculerEchantillon(largeur: Int, hauteur: Int): Int {
        if (largeur <= 0 || hauteur <= 0) return 1
        var echantillon = 1
        while (largeur / (echantillon * 2) >= DIMENSION_MAX || hauteur / (echantillon * 2) >= DIMENSION_MAX) {
            echantillon *= 2
        }
        return echantillon
    }

    private fun reduire(bitmap: Bitmap): Bitmap {
        val largeur = bitmap.width
        val hauteur = bitmap.height
        if (largeur <= DIMENSION_MAX && hauteur <= DIMENSION_MAX) return bitmap
        val ratio = minOf(DIMENSION_MAX.toFloat() / largeur, DIMENSION_MAX.toFloat() / hauteur)
        return Bitmap.createScaledBitmap(
            bitmap,
            (largeur * ratio).toInt().coerceAtLeast(1),
            (hauteur * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
