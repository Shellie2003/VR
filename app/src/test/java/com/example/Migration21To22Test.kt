package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La migration 21 -> 22 (plan d'étagères) n'ajoute que des tables neuves — rien n'est modifié sur
 * le schéma existant. Le risque est donc plus faible que pour 20 -> 21, mais reste réel : une
 * faute dans un `CREATE TABLE` (nom de colonne, index) ne se verrait qu'au premier lancement chez
 * un client après mise à jour. Ce test rejoue la migration sur une base minimale représentant
 * l'état v21 (une table témoin avec des données, pour vérifier qu'elle n'est pas touchée) et
 * vérifie que les trois tables neuves existent, acceptent des écritures cohérentes avec le DAO,
 * et respectent l'index unique anti-doublon sur (niveauId, produitId).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration21To22Test {

    private fun creerSchemaV21Minimal(db: SupportSQLiteDatabase) {
        // Représentant du reste de la base v21 : sert uniquement à vérifier que la migration ne
        // touche à rien d'existant.
        db.execSQL("CREATE TABLE products (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
        db.execSQL("INSERT INTO products (name) VALUES ('Vary')")
    }

    @Test
    fun `la migration 21 vers 22 cree les tables d etageres sans toucher au reste`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-test-21-22.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                    override fun onCreate(db: SupportSQLiteDatabase) = creerSchemaV21Minimal(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        AppDatabase.MIGRATION_21_22.migrate(db)

        // 1. Le reste de la base est intact.
        assertEquals(1, compter(db, "products"))

        // 2. Les trois tables existent et acceptent des écritures cohérentes entre elles.
        db.execSQL("INSERT INTO etageres (id, nom, position) VALUES (1, 'Étagère 1', 0)")
        db.execSQL("INSERT INTO niveaux_etagere (id, etagereId, position, nom) VALUES (1, 1, 0, '')")
        db.execSQL("INSERT INTO produits_niveau (niveauId, produitId, ordre) VALUES (1, 1, 0)")
        assertEquals(1, compter(db, "etageres"))
        assertEquals(1, compter(db, "niveaux_etagere"))
        assertEquals(1, compter(db, "produits_niveau"))

        // 3. L'index unique (niveauId, produitId) refuse bien un doublon.
        var doublonRefuse = false
        try {
            db.execSQL("INSERT INTO produits_niveau (niveauId, produitId, ordre) VALUES (1, 1, 5)")
        } catch (e: Exception) {
            doublonRefuse = true
        }
        assertTrue("l'index unique niveauId+produitId devrait refuser le doublon", doublonRefuse)
        assertEquals(1, compter(db, "produits_niveau"))

        helper.close()
    }

    private fun compter(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
}
