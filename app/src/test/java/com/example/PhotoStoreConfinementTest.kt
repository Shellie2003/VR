package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.PhotoStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * `Product.imageUrl` vient de la BASE, donc d'une charge de synchronisation reçue sur le réseau
 * local ou d'une archive de sauvegarde restaurée — deux sources qui ne sont pas les nôtres. Tant
 * que `PhotoStore.resoudre` honorait n'importe quel chemin absolu existant, ce champ était une
 * primitive de lecture de fichier arbitraire dans le bac à sable de l'application : un produit
 * portant `file:///data/.../shared_prefs/pos_app_prefs.xml` faisait encoder le fichier de
 * préférences en base64 dans la sauvegarde/synchronisation suivante, donc diffusait le code de
 * récupération — et avec lui l'accès à toute la sauvegarde cloud de la boutique.
 *
 * Ces tests vérifient les deux moitiés du correctif : le chemin absolu n'est honoré que s'il
 * désigne réellement une de nos photos, ET le rattrapage par nom de fichier (section 46), qui est
 * ce qui rend une archive transportable d'un téléphone à l'autre, reste intact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhotoStoreConfinementTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun ecrire(fichier: File, contenu: String): File {
        fichier.parentFile?.mkdirs()
        fichier.writeText(contenu)
        return fichier
    }

    @Test
    fun `une photo legitime du dossier dedie est resolue par son chemin absolu`() {
        val photo = ecrire(File(PhotoStore.dossier(context), "product_img_1.jpg"), "jpeg")

        assertEquals(photo.canonicalPath, PhotoStore.resoudre(context, PhotoStore.reference(photo))?.canonicalPath)
    }

    @Test
    fun `une photo historique a la racine de filesDir reste resolue`() {
        // Les versions antérieures écrivaient les photos en vrac dans `filesDir`, sous ce préfixe.
        val photo = ecrire(File(context.filesDir, "product_img_ancienne.jpg"), "jpeg")

        assertEquals(photo.canonicalPath, PhotoStore.resoudre(context, PhotoStore.reference(photo))?.canonicalPath)
    }

    @Test
    fun `le fichier de preferences ne peut plus etre lu via imageUrl`() {
        // Reproduit la cible exacte : le fichier de préférences porte `firebaseBackupToken`, donc
        // le code de récupération de la boutique. Écrit à la main plutôt que via AppPreferences —
        // c'est la règle de confinement qu'on teste, pas la date d'écriture des préférences.
        val prefs = ecrire(
            File(context.filesDir.parentFile, "shared_prefs/pos_app_prefs.xml"),
            "<map><string name=\"key_firebase_backup_token\">SECRET</string></map>"
        )

        assertNull(
            "Un chemin hors du stockage des photos ne doit jamais être résolu",
            PhotoStore.resoudre(context, PhotoStore.reference(prefs))
        )
    }

    @Test
    fun `un chemin de traversee par point-point est rejete`() {
        val prefs = ecrire(
            File(context.filesDir.parentFile, "shared_prefs/pos_app_prefs.xml"),
            "<map/>"
        )
        // Le chemin PART du dossier des photos : seule la comparaison sur le chemin canonique,
        // qui résout `..` avant le test, le rejette.
        val traversee = File(PhotoStore.dossier(context), "../../shared_prefs/pos_app_prefs.xml")
        assertEquals(
            "Le chemin de traversée doit bien désigner le fichier visé, sinon le test ne prouve rien",
            prefs.canonicalPath,
            traversee.canonicalPath
        )

        assertNull(PhotoStore.resoudre(context, "file://" + traversee.path))
    }

    @Test
    fun `un fichier hors filesDir est rejete meme s'il existe`() {
        val horsPerimetre = ecrire(File(context.cacheDir, "product_img_1.jpg"), "jpeg")

        // Le préfixe de nom ne suffit pas : c'est l'EMPLACEMENT qui décide.
        assertNull(PhotoStore.resoudre(context, PhotoStore.reference(horsPerimetre)))
    }

    @Test
    fun `un fichier de filesDir sans le prefixe des photos est rejete`() {
        // `filesDir` contient aussi la sauvegarde locale (`database_safety_backup.json`), qui porte
        // toute la base de la boutique. Seul le préfixe des photos y est honoré.
        val sauvegarde = ecrire(File(context.filesDir, "database_safety_backup.json"), "{\"products\":[]}")

        assertNull(PhotoStore.resoudre(context, PhotoStore.reference(sauvegarde)))
    }

    @Test
    fun `une photo venue d'un autre telephone est retrouvee par son nom de fichier`() {
        // C'est le rattrapage de la section 46, et la raison pour laquelle le confinement ne peut
        // pas simplement faire échouer la résolution : le chemin absolu enregistré désigne le
        // dossier du téléphone d'origine, mais le fichier est bien là, sous le même nom.
        val photo = ecrire(File(PhotoStore.dossier(context), "product_img_42.jpg"), "jpeg")
        val cheminEtranger = "file:///data/user/0/com.autre.paquet/files/photos/product_img_42.jpg"

        assertEquals(photo.canonicalPath, PhotoStore.resoudre(context, cheminEtranger)?.canonicalPath)
    }

    @Test
    fun `le rattrapage par nom de fichier ne rouvre pas la lecture arbitraire`() {
        // `nomFichier` ne garde que le dernier segment, donc le repli ne peut chercher que dans nos
        // deux emplacements : un fichier de préférences n'y est jamais trouvé, même de nom.
        ecrire(File(context.filesDir.parentFile, "shared_prefs/pos_app_prefs.xml"), "<map/>")

        assertNull(PhotoStore.resoudre(context, "file:///data/data/com.example/shared_prefs/pos_app_prefs.xml"))
    }

    @Test
    fun `la sauvegarde locale ne peut pas etre lue par un chemin etranger de meme nom`() {
        // Le cas réellement exploitable, et celui que le premier jet du correctif laissait ouvert :
        // le chemin absolu est rejeté, mais le repli par NOM allait chercher le fichier à la racine
        // de `filesDir` sans exiger le préfixe des photos. `database_safety_backup.json` y porte
        // toute la base de la boutique — ventes, dettes, caisse — et partait donc encodée dans la
        // charge de synchronisation suivante.
        ecrire(File(context.filesDir, "database_safety_backup.json"), "{\"sales\":[]}")

        assertNull(
            PhotoStore.resoudre(context, "file:///data/user/0/com.autre.paquet/files/database_safety_backup.json")
        )
    }

    @Test
    fun `un repertoire n'est jamais rendu comme photo`() {
        // `nomFichier` d'une référence terminée par `..` rend `".."`, et `File(dossier, "..")` est
        // un répertoire existant dont `length()` n'est pas nulle : `exists()` l'aurait accepté.
        PhotoStore.dossier(context)

        assertNull(PhotoStore.resoudre(context, "file://" + File(PhotoStore.dossier(context), "..").path))
    }

    @Test
    fun `un fichier photo vide est traite comme absent`() {
        val vide = ecrire(File(PhotoStore.dossier(context), "product_img_vide.jpg"), "")

        assertNull(PhotoStore.resoudre(context, PhotoStore.reference(vide)))
    }

    @Test
    fun `une reference qui n'est pas une photo locale est ignoree`() {
        assertNull(PhotoStore.resoudre(context, "https://images.openfoodfacts.org/x.jpg"))
        assertNull(PhotoStore.resoudre(context, null))
        assertNull(PhotoStore.resoudre(context, ""))
    }

    @Test
    fun `toutes ne remonte que nos photos`() {
        ecrire(File(PhotoStore.dossier(context), "product_img_1.jpg"), "jpeg")
        ecrire(File(context.filesDir, "product_img_2.jpg"), "jpeg")
        ecrire(File(context.filesDir, "database_safety_backup.json"), "{}")

        val noms = PhotoStore.toutes(context).map { it.name }.toSet()

        // Contenance et exclusion plutôt qu'égalité exacte : Robolectric ne garantit pas que
        // `filesDir` soit remis à neuf entre deux méthodes de test, et une égalité exacte
        // dépendrait alors de l'ordre d'exécution.
        assertTrue("La photo du dossier dédié doit être listée", "product_img_1.jpg" in noms)
        assertTrue("La photo historique doit être listée", "product_img_2.jpg" in noms)
        assertFalse(
            "La sauvegarde locale n'est pas une photo et ne doit jamais être listée",
            "database_safety_backup.json" in noms
        )
        assertNotNull(PhotoStore.resoudre(context, PhotoStore.reference(File(PhotoStore.dossier(context), "product_img_1.jpg"))))
    }
}
