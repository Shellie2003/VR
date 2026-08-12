package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La migration 24 -> 25 ajoute les quatre colonnes du mode pharmacie (`dci`, `dosage`,
 * `formeGalenique`, `surOrdonnance`).
 *
 * Le risque à couvrir tient en deux points. D'abord, les produits DÉJÀ en base — une épicerie en
 * service a des centaines de références — doivent survivre intacts et lire `NULL` sur les nouvelles
 * colonnes. Ensuite et surtout, `surOrdonnance` est un booléen NOT NULL : sans valeur par défaut à
 * 0, la migration échouerait sur une table non vide, ou pire, rendrait des produits d'épicerie
 * subitement « soumis à ordonnance ».
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration24To25Test {

    private fun creerSchemaV24Minimal(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE products (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                category TEXT NOT NULL,
                stock REAL NOT NULL,
                barcode TEXT NOT NULL,
                isTemplate INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "INSERT INTO products (id, name, price, category, stock, barcode, isTemplate) " +
                "VALUES (1, 'Vary gasy', 3200.0, 'Alimentation', 50.0, '6111222333444', 0)"
        )
    }

    @Test
    fun `la migration 24 vers 25 ajoute les champs pharmacie sans abimer les produits existants`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-test-24-25.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(24) {
                    override fun onCreate(db: SupportSQLiteDatabase) = creerSchemaV24Minimal(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        AppDatabase.MIGRATION_24_25.migrate(db)

        // 1. Le produit d'épicerie déjà en base est intact et n'a aucune donnée pharmacie.
        db.query("SELECT name, dci, dosage, formeGalenique, surOrdonnance FROM products WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals("Vary gasy", c.getString(0))
            assertNull(c.getString(1))
            assertNull(c.getString(2))
            assertNull(c.getString(3))
            // Le point critique : un produit d'épicerie ne doit JAMAIS devenir soumis à ordonnance.
            assertEquals(0, c.getInt(4))
        }

        // 2. Un médicament peut désormais être enregistré avec sa molécule.
        db.execSQL(
            "INSERT INTO products (id, name, price, category, stock, barcode, isTemplate, dci, dosage, formeGalenique, surOrdonnance) " +
                "VALUES (2, 'Doliprane 500', 1500.0, 'Médicaments', 30.0, '', 0, 'paracétamol', '500 mg', 'comprimé', 0)"
        )

        // 3. La recherche par principe actif retrouve le nom commercial : le client demande
        //    « paracétamol », pas « Doliprane ».
        db.query("SELECT name FROM products WHERE dci LIKE '%paracétamol%'").use { c ->
            c.moveToFirst()
            assertEquals(1, c.count)
            assertEquals("Doliprane 500", c.getString(0))
        }

        helper.close()
    }
}
