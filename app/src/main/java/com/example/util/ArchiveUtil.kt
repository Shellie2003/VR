package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Archive de sauvegarde **complète** : les données ET les photos, dans un seul fichier `.zip`.
 *
 * Répartition assumée entre les deux chemins de sauvegarde :
 *
 * - **Firebase** ne reçoit jamais les photos, seulement les références (`imageUrl`). C'est un
 *   choix de coût : une épicerie de 60 produits photographiés produisait ~12 Mo de JSON par envoi,
 *   payé au mégaoctet par l'épicier et multiplié par chaque mutation. Le plan gratuit passait
 *   d'environ 80 boutiques à quelques-unes.
 * - **Tout le reste** — sauvegarde à conserver, partage WhatsApp, dépôt sur Drive, copie sur une
 *   carte SD — passe par ici et contient les photos. Ces envois-là sont manuels et occasionnels :
 *   leur poids est acceptable, et une sauvegarde amputée de ses photos n'est pas une sauvegarde.
 *
 * Structure de l'archive :
 * ```
 * varotra-sauvegarde-2026-07-25.zip
 *   donnees.json          <- exactement le même JSON que la sauvegarde locale et la synchronisation
 *   photos/product_img_1753420000000.jpg
 *   photos/product_img_1753420111111.jpg
 * ```
 *
 * À la relecture, les photos sont réécrites dans [PhotoStore.dossier] sous **le même nom**. C'est
 * ce qui permet à [PhotoStore.resoudre] de les retrouver alors que le chemin absolu enregistré en
 * base pointait vers le dossier d'un autre téléphone.
 */
object ArchiveUtil {

    const val NOM_JSON = "donnees.json"
    private const val DOSSIER_PHOTOS = "photos"
    private const val TAILLE_TAMPON = 8 * 1024

    /** Un `.zip` commence toujours par ces deux octets — « PK », pour Phil Katz. */
    private val SIGNATURE_ZIP = byteArrayOf(0x50, 0x4B)

    /**
     * Fabrique l'archive dans [destination]. Renvoie le fichier écrit, ou null en cas d'échec —
     * jamais une archive à moitié écrite, qui donnerait au gérant l'illusion d'être sauvegardé.
     */
    fun creerArchive(context: Context, json: String, destination: File): File? {
        return try {
            destination.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(destination).buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(NOM_JSON))
                zip.write(json.toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                PhotoStore.toutes(context).forEach { photo ->
                    try {
                        zip.putNextEntry(ZipEntry("$DOSSIER_PHOTOS/${photo.name}"))
                        photo.inputStream().use { it.copyTo(zip, TAILLE_TAMPON) }
                        zip.closeEntry()
                    } catch (e: Exception) {
                        // Une photo illisible ne doit pas emporter toute la sauvegarde avec elle.
                        Log.e("ArchiveUtil", "Photo ignorée dans l'archive : ${photo.name}", e)
                    }
                }
            }
            destination
        } catch (e: Exception) {
            Log.e("ArchiveUtil", "Échec de création de l'archive", e)
            runCatching { destination.delete() }
            null
        }
    }

    /**
     * Lit un fichier choisi par le gérant et renvoie le JSON de données, en extrayant au passage les
     * photos si c'en est une archive.
     *
     * Accepte les **deux** formats : les archives `.zip` de cette version, et les `.json` nus
     * produits par les versions précédentes — un gérant qui restaure une sauvegarde d'il y a six
     * mois ne doit pas se heurter à un refus.
     */
    fun lireSauvegarde(context: Context, uri: Uri): String? {
        val octets = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            Log.e("ArchiveUtil", "Fichier illisible", e)
            null
        } ?: return null

        return if (estZip(octets)) lireArchive(context, octets) else {
            val texte = String(octets, Charsets.UTF_8)
            if (texte.isBlank()) null else texte
        }
    }

    private fun estZip(octets: ByteArray): Boolean =
        octets.size >= 2 && octets[0] == SIGNATURE_ZIP[0] && octets[1] == SIGNATURE_ZIP[1]

    private fun lireArchive(context: Context, octets: ByteArray): String? {
        var json: String? = null
        var photosExtraites = 0
        try {
            ZipInputStream(octets.inputStream()).use { zip ->
                var entree: ZipEntry? = zip.nextEntry
                while (entree != null) {
                    val nom = entree.name
                    when {
                        !entree.isDirectory && nom.substringAfterLast('/') == NOM_JSON -> {
                            json = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        !entree.isDirectory && nom.contains("$DOSSIER_PHOTOS/") -> {
                            if (extrairePhoto(context, nom, zip)) photosExtraites++
                        }
                    }
                    zip.closeEntry()
                    entree = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e("ArchiveUtil", "Archive illisible ou incomplète", e)
            return json  // le JSON déjà lu vaut mieux que rien : les données priment sur les photos
        }
        Log.d("ArchiveUtil", "Archive lue : $photosExtraites photo(s) restaurée(s)")
        return json
    }

    /**
     * Écrit une photo de l'archive dans le dossier photos.
     *
     * Le nom est réduit à son dernier segment et les entrées remontantes sont refusées : une archive
     * fabriquée à la main pourrait sinon contenir `photos/../../databases/varotra_database_v4` et
     * écraser la base de données à la restauration (« Zip Slip »). Une sauvegarde arrive ici par
     * WhatsApp, d'une main qui n'est pas toujours celle du gérant.
     */
    private fun extrairePhoto(context: Context, nomEntree: String, flux: InputStream): Boolean {
        val nom = nomEntree.substringAfterLast('/')
        if (nom.isBlank() || nom == "." || nom == "..") return false
        return try {
            val cible = File(PhotoStore.dossier(context), nom)
            val dossierAutorise = PhotoStore.dossier(context).canonicalPath
            if (!cible.canonicalPath.startsWith(dossierAutorise)) {
                Log.e("ArchiveUtil", "Entrée d'archive refusée : $nomEntree")
                return false
            }
            FileOutputStream(cible).use { sortie -> flux.copyTo(sortie, TAILLE_TAMPON) }
            true
        } catch (e: Exception) {
            Log.e("ArchiveUtil", "Photo non extraite : $nomEntree", e)
            false
        }
    }
}
