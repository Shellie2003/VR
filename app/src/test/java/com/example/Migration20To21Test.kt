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
 * La migration 20 -> 21 s'exécute chez des épiceries qui ont des mois de ventes et de dettes en
 * base : une table mal orthographiée ou un ALTER invalide ne se verrait qu'en production, sous la
 * forme d'un plantage au premier lancement après mise à jour — ou pire, d'un retour au filet
 * destructif. Ce test rejoue la migration sur une base construite à la main dans l'état v20,
 * peuplée de données, et vérifie que TOUT survit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration20To21Test {

    /** Les cinq tables touchées par la migration, telles qu'elles existaient en version 20. */
    private fun creerSchemaV20(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE sales (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "timestamp INTEGER NOT NULL, totalAmount REAL NOT NULL, items TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE debts (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "debtorName TEXT NOT NULL, amount REAL NOT NULL, balance REAL NOT NULL, " +
                "date INTEGER NOT NULL, note TEXT NOT NULL, isPaid INTEGER NOT NULL, dueDate INTEGER)"
        )
        db.execSQL(
            "CREATE TABLE restocks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "productId INTEGER NOT NULL, productName TEXT NOT NULL, cartonsQuantity REAL NOT NULL, " +
                "itemsPerCarton REAL NOT NULL, totalUnits REAL NOT NULL, totalCostPrice REAL NOT NULL, " +
                "unitSellingPrice REAL NOT NULL, supplierId INTEGER, supplierName TEXT, timestamp INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE mouvements_caisse (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "type TEXT NOT NULL, montant REAL NOT NULL, motif TEXT NOT NULL, " +
                "note TEXT NOT NULL, date INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE retours (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "saleId INTEGER NOT NULL, timestamp INTEGER NOT NULL, items TEXT NOT NULL, " +
                "totalAmount REAL NOT NULL, motif TEXT NOT NULL, modePaiementOrigine TEXT NOT NULL)"
        )
    }

    private fun peupler(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO sales (timestamp, totalAmount, items) VALUES (1700000000000, 12500.0, '[]')")
        db.execSQL("INSERT INTO sales (timestamp, totalAmount, items) VALUES (1700000060000, 3200.0, '[]')")
        db.execSQL(
            "INSERT INTO debts (debtorName, amount, balance, date, note, isPaid, dueDate) " +
                "VALUES ('Koto', 5000.0, 2000.0, 1700000000000, 'Vary (2 x 2 500)', 0, NULL)"
        )
        db.execSQL(
            "INSERT INTO restocks (productId, productName, cartonsQuantity, itemsPerCarton, totalUnits, " +
                "totalCostPrice, unitSellingPrice, supplierId, supplierName, timestamp) " +
                "VALUES (1, 'Vary', 2.0, 24.0, 48.0, 120000.0, 3200.0, NULL, NULL, 1700000000000)"
        )
        db.execSQL("INSERT INTO mouvements_caisse (type, montant, motif, note, date) VALUES ('SORTIE', 10000.0, 'Achat sacs', '', 1700000000000)")
        db.execSQL("INSERT INTO retours (saleId, timestamp, items, totalAmount, motif, modePaiementOrigine) VALUES (1, 1700000000000, '[]', 3200.0, 'Cassé', 'ESPECES')")
    }

    @Test
    fun `la migration 20 vers 21 preserve toutes les donnees existantes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-test-20-21.db")
                .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        creerSchemaV20(db)
                        peupler(db)
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )

        val db = helper.writableDatabase
        AppDatabase.MIGRATION_20_21.migrate(db)

        // 1. Rien n'a été perdu.
        assertEquals(2, compter(db, "sales"))
        assertEquals(1, compter(db, "debts"))
        assertEquals(1, compter(db, "restocks"))
        assertEquals(1, compter(db, "mouvements_caisse"))
        assertEquals(1, compter(db, "retours"))

        // 2. Les nouvelles colonnes existent, avec leur valeur par défaut, sur les données anciennes.
        for (table in listOf("sales", "debts", "restocks", "mouvements_caisse", "retours")) {
            db.query("SELECT deviceName, vendeurNom FROM $table LIMIT 1").use { c ->
                assertTrue("$table: ligne attendue", c.moveToFirst())
                assertEquals("$table.deviceName", "", c.getString(0))
                assertEquals("$table.vendeurNom", "", c.getString(1))
            }
        }

        // 3. Le journal d'audit est créé et accepte une écriture.
        db.execSQL(
            "INSERT INTO audit_logs (timestamp, deviceName, utilisateur, role, type, cible, details, montant, severite, bloque) " +
                "VALUES (1700000000000, 'Tel test', '', '', 'SUPPRESSION_VENTE', 'Vente', '', 1000.0, 'ALERTE', 0)"
        )
        assertEquals(1, compter(db, "audit_logs"))

        // 4. Les données elles-mêmes sont intactes (pas seulement les comptes de lignes).
        db.query("SELECT debtorName, balance, note FROM debts").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Koto", c.getString(0))
            assertEquals(2000.0, c.getDouble(1), 0.001)
            assertEquals("Vary (2 x 2 500)", c.getString(2))
        }

        helper.close()
    }

    private fun compter(db: SupportSQLiteDatabase, table: String): Int =
        db.query("SELECT COUNT(*) FROM $table").use { c ->
            c.moveToFirst()
            c.getInt(0)
        }
}
