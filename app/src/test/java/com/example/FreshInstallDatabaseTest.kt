package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Le parcours **installation neuve** : une base créée de zéro, donc le callback `onCreate` de
 * `AppDatabase.getDatabase()` et son catalogue d'amorçage.
 *
 * **Pourquoi ce fichier existe, et c'est le point à retenir.** Les 26 autres tests construisent la
 * base avec `Room.inMemoryDatabaseBuilder(...)`, qui n'enregistre JAMAIS `.addCallback(...)` — ce
 * callback n'existe que dans `getDatabase()`. `onCreate` n'a donc jamais été exécuté par un seul
 * test du projet, et c'est exactement là qu'une épicerie toute neuve a planté en production :
 *
 *     SQLiteConstraintException: NOT NULL constraint failed: products.surOrdonnance
 *
 * `Product.surOrdonnance` (section 71) est un booléen NOT NULL. Une valeur par défaut **Kotlin**
 * n'est pas une valeur par défaut **SQL** : sans `@ColumnInfo(defaultValue = ...)`, Room écrit
 * `surOrdonnance INTEGER NOT NULL` tout court dans son `CREATE TABLE`, et la liste de colonnes des
 * `INSERT` d'amorçage omettait ce champ.
 *
 * L'ironie mérite d'être notée : `Migration24To25Test` vérifie soigneusement le `DEFAULT 0` du
 * chemin de **mise à jour**, et il est juste. Mais une installation neuve ne passe pas par la
 * migration — elle passe par `CREATE TABLE`, que rien ne couvrait. Un téléphone qui avait déjà des
 * données n'a donc jamais pu voir ce défaut ; un téléphone neuf ne pouvait que le voir, à chaque
 * lancement, sans aucun recours.
 *
 * Ces tests valent pour toute la **famille** de défauts, pas pour cette seule colonne : ajouter à
 * l'avenir un champ NOT NULL sans l'ajouter aux `INSERT` fait échouer le premier test ci-dessous.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FreshInstallDatabaseTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    /**
     * `getDatabase` mémorise son instance dans un singleton. Sans cette remise à zéro, seule la
     * première méthode exécutée créerait réellement une base, et les suivantes se contenteraient de
     * relire la sienne — elles passeraient donc sans rien éprouver du parcours visé.
     */
    @Before
    fun avantChaqueTest() = oublierLeSingleton()

    @After
    fun apresChaqueTest() = oublierLeSingleton()

    private fun oublierLeSingleton() {
        val champ = AppDatabase::class.java.getDeclaredField("INSTANCE")
        champ.isAccessible = true
        (champ.get(null) as? AppDatabase)?.close()
        champ.set(null, null)
    }

    /** Ouvre la base comme le fait un téléphone qui vient d'installer l'application. */
    private fun baseNeuve() = AppDatabase.getDatabase(context).also {
        // Forcer l'ouverture réelle : c'est elle qui déclenche `CREATE TABLE` puis `onCreate`.
        // Une requête brute et non un DAO, pour ne pas buter sur le contrôle de fil principal.
        it.openHelper.writableDatabase
    }

    private fun compter(table: String): Int {
        val curseur = baseNeuve().openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table")
        return curseur.use { if (it.moveToFirst()) it.getInt(0) else -1 }
    }

    @Test
    fun `une base creee de zero s'ouvre et contient les produits d'amorcage`() {
        // Si une colonne NOT NULL manque de la liste des INSERT, l'amorçage échoue et ce compte
        // tombe à zéro. C'est la seule chose qui sépare une installation neuve utilisable d'une
        // application qui se ferme à chaque lancement.
        assertEquals("L'amorçage de `products` doit aboutir sur une base neuve", 4, compter("products"))
    }

    @Test
    fun `les produits d'amorcage ne sont pas soumis a ordonnance`() {
        // Ajouter la colonne manquante ne suffit pas : il faut y écrire 0. Avec 1, du riz et des
        // carottes seraient « soumis à ordonnance » dès la première ouverture (section 71).
        val curseur = baseNeuve().openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM products WHERE surOrdonnance != 0")
        val flagues = curseur.use { if (it.moveToFirst()) it.getInt(0) else -1 }
        assertEquals("Aucun produit d'épicerie ne doit être marqué sur ordonnance", 0, flagues)
    }

    @Test
    fun `les tables relationnelles sont amorcees elles aussi`() {
        // `onCreate` est une seule transaction : l'échec sur `products` emportait aussi ces deux
        // tables, qui restaient vides. Les couvrir ici protège la même famille de défaut chez elles.
        assertEquals("`produits` doit être amorcée", 4, compter("produits"))
        assertEquals("`unites_produit` doit être amorcée", 4, compter("unites_produit"))
    }
}
