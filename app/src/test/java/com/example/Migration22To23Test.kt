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
 * La migration 22 -> 23 ajoute une colonne `couleur` (nullable) aux étagères et aux niveaux, pour
 * le nouveau color picker. Simple `ALTER TABLE ... ADD COLUMN`, mais le risque reste réel : une
 * ligne existante doit lire NULL (pas de couleur) sans erreur, et les lignes déjà présentes ne
 * doivent pas être touchées.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration22To23Test {

    private fun creerSchemaV22Minimal(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE etageres (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                nom TEXT NOT NULL,
                position INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE niveaux_etagere (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                etagereId INTEGER NOT NULL,
                position INTEGER NOT NULL,
                nom TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("INSERT INTO etageres (id, nom, position) VALUES (1, 'Étagère 1', 0)")
        db.execSQL("INSERT INTO niveaux_etagere (id, etagereId, position, nom) VALUES (1, 1, 0, '')")
    }

    @Test
    fun `la migration 22 vers 23 ajoute couleur sans toucher aux lignes existantes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-test-22-23.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(22) {
                    override fun onCreate(db: SupportSQLiteDatabase) = creerSchemaV22Minimal(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        AppDatabase.MIGRATION_22_23.migrate(db)

        // 1. Les lignes déjà présentes lisent couleur = NULL, jamais une valeur inventée.
        db.query("SELECT couleur FROM etageres WHERE id = 1").use { c ->
            c.moveToFirst()
            assertNull(c.getString(0))
        }
        db.query("SELECT couleur FROM niveaux_etagere WHERE id = 1").use { c ->
            c.moveToFirst()
            assertNull(c.getString(0))
        }

        // 2. Une nouvelle ligne peut écrire une couleur.
        db.execSQL("INSERT INTO etageres (id, nom, position, couleur) VALUES (2, 'Étagère 2', 1, '#4CAF50')")
        db.query("SELECT couleur FROM etageres WHERE id = 2").use { c ->
            c.moveToFirst()
            assertEquals("#4CAF50", c.getString(0))
        }

        helper.close()
    }
}
