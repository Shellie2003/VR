package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La migration 23 -> 24 ajoute la table de liaison `ligne_vente_lots`, qui relie une ligne de vente
 * aux lots dont la marchandise est réellement sortie.
 *
 * Contrairement aux migrations précédentes, aucune table existante n'est modifiée : c'est une
 * création pure. Le risque à couvrir est donc ailleurs — il faut vérifier que les données déjà en
 * base survivent intactes (une base de production contient des ventes), et que la nouvelle table
 * accepte bien plusieurs lots pour UNE SEULE ligne de vente, puisque c'est précisément le cas qu'un
 * simple champ `lotId` sur `lignes_vente` n'aurait pas su représenter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration23To24Test {

    private fun creerSchemaV23Minimal(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE lignes_vente (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                venteId INTEGER NOT NULL,
                produitId INTEGER NOT NULL,
                uniteId INTEGER NOT NULL,
                quantite REAL NOT NULL,
                prixUnitaireApplique REAL NOT NULL,
                montantLigne REAL NOT NULL,
                regleAppliqueeId INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE lots_produit (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                produitId INTEGER NOT NULL,
                numeroLot TEXT,
                quantite REAL NOT NULL,
                datePeremption INTEGER NOT NULL,
                dateReception INTEGER NOT NULL
            )
            """.trimIndent()
        )
        // Une vente et deux lots déjà en base, comme chez un client en production.
        db.execSQL(
            "INSERT INTO lignes_vente (id, venteId, produitId, uniteId, quantite, prixUnitaireApplique, montantLigne) " +
                "VALUES (1, 1, 1, 1, 10.0, 500.0, 5000.0)"
        )
        db.execSQL("INSERT INTO lots_produit (id, produitId, quantite, datePeremption, dateReception) VALUES (1, 1, 6.0, 1000, 0)")
        db.execSQL("INSERT INTO lots_produit (id, produitId, quantite, datePeremption, dateReception) VALUES (2, 1, 10.0, 2000, 0)")
    }

    @Test
    fun `la migration 23 vers 24 cree la table de liaison sans toucher aux donnees existantes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-test-23-24.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                    override fun onCreate(db: SupportSQLiteDatabase) = creerSchemaV23Minimal(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        AppDatabase.MIGRATION_23_24.migrate(db)

        // 1. La vente déjà enregistrée est intacte.
        db.query("SELECT quantite FROM lignes_vente WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(10.0, c.getDouble(0), 1e-9)
        }

        // 2. Une même ligne de vente peut pointer vers PLUSIEURS lots — 6 pris sur le lot qui
        //    périme le plus tôt, 4 sur le suivant. C'est exactement ce qu'un champ unique sur
        //    `lignes_vente` n'aurait pas permis d'enregistrer.
        db.execSQL("INSERT INTO ligne_vente_lots (ligneVenteId, lotId, quantite) VALUES (1, 1, 6.0)")
        db.execSQL("INSERT INTO ligne_vente_lots (ligneVenteId, lotId, quantite) VALUES (1, 2, 4.0)")

        db.query("SELECT COUNT(*), SUM(quantite) FROM ligne_vente_lots WHERE ligneVenteId = 1").use { c ->
            c.moveToFirst()
            assertEquals(2, c.getInt(0))
            assertEquals(10.0, c.getDouble(1), 1e-9)
        }

        // 3. Traçabilité de rappel : retrouver ce qui est sorti d'un lot donné.
        db.query("SELECT ligneVenteId FROM ligne_vente_lots WHERE lotId = 2").use { c ->
            c.moveToFirst()
            assertEquals(1L, c.getLong(0))
        }

        helper.close()
    }
}
