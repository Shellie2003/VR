package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.Debt
import com.example.data.model.Produit
import com.example.data.model.UniteProduit
import com.example.data.model.ReglePrix
import com.example.data.model.Fournisseur
import com.example.data.model.MouvementStock
import com.example.data.model.LotProduit
import com.example.data.model.Vente
import com.example.data.model.LigneVente
import com.example.data.model.Restock
import com.example.data.model.MouvementCaisse
import com.example.data.model.CaisseSession
import com.example.data.model.Vendeur
import com.example.data.model.Retour
import com.example.data.model.DeletedRecord
import com.example.data.model.AuditLog
import com.example.data.model.Etagere
import com.example.data.model.NiveauEtagere
import com.example.data.model.ProduitNiveau
import androidx.room.migration.Migration

@Database(
    entities = [
        Product::class,
        Sale::class,
        Debt::class,
        Produit::class,
        UniteProduit::class,
        ReglePrix::class,
        Fournisseur::class,
        MouvementStock::class,
        LotProduit::class,
        Vente::class,
        LigneVente::class,
        Restock::class,
        MouvementCaisse::class,
        CaisseSession::class,
        Vendeur::class,
        Retour::class,
        DeletedRecord::class,
        AuditLog::class,
        Etagere::class,
        NiveauEtagere::class,
        ProduitNiveau::class
    ],
    version = 22,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun debtDao(): DebtDao
    abstract fun restockDao(): RestockDao
    abstract fun mouvementCaisseDao(): MouvementCaisseDao
    abstract fun caisseSessionDao(): CaisseSessionDao
    abstract fun vendeurDao(): VendeurDao
    abstract fun retourDao(): RetourDao
    abstract fun deletedRecordDao(): DeletedRecordDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun etagereDao(): EtagereDao

    abstract fun produitDao(): ProduitDao
    abstract fun uniteProduitDao(): UniteProduitDao
    abstract fun reglePrixDao(): ReglePrixDao
    abstract fun fournisseurDao(): FournisseurDao
    abstract fun mouvementStockDao(): MouvementStockDao
    abstract fun lotProduitDao(): LotProduitDao
    abstract fun venteDao(): VenteDao
    abstract fun lignesVenteDao(): LigneVenteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v20 -> v21 : identification de l'appareil sur chaque transaction + journal d'audit.
         *
         * Écrite explicitement (au lieu de laisser `fallbackToDestructiveMigration` faire son
         * travail) parce qu'une épicerie déjà en production perdrait sinon TOUT son historique de
         * ventes, dettes et caisse à la simple installation de cette mise à jour.
         */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                for (table in listOf("sales", "restocks", "mouvements_caisse", "retours", "debts")) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN deviceName TEXT NOT NULL DEFAULT ''")
                    db.execSQL("ALTER TABLE $table ADD COLUMN vendeurNom TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audit_logs (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        timestamp INTEGER NOT NULL,
                        deviceName TEXT NOT NULL,
                        utilisateur TEXT NOT NULL,
                        role TEXT NOT NULL,
                        type TEXT NOT NULL,
                        cible TEXT NOT NULL,
                        details TEXT NOT NULL,
                        montant REAL NOT NULL,
                        severite TEXT NOT NULL,
                        bloque INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_timestamp ON audit_logs (timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_type ON audit_logs (type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_logs_severite ON audit_logs (severite)")
            }
        }

        /** v21 -> v22 : plan d'étagères interactif (écran Paramètres > Étagère). Tables neuves
         * uniquement, aucune colonne existante touchée — la seule chose à risque serait de rater
         * un nom de table, d'où [EtagereMigrationTest] qui rejoue cette migration sur une base
         * réelle avant de faire confiance à ces `CREATE TABLE`. */
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS etageres (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        nom TEXT NOT NULL,
                        position INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_etageres_position ON etageres (position)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS niveaux_etagere (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        etagereId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        nom TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_niveaux_etagere_etagereId ON niveaux_etagere (etagereId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_niveaux_etagere_etagereId_position ON niveaux_etagere (etagereId, position)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS produits_niveau (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        niveauId INTEGER NOT NULL,
                        produitId INTEGER NOT NULL,
                        ordre INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_produits_niveau_niveauId ON produits_niveau (niveauId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_produits_niveau_produitId ON produits_niveau (produitId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_produits_niveau_niveauId_produitId ON produits_niveau (niveauId, produitId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "varotra_database_v4"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate the products table with default values including barcodes, wholesale prices, sku, and stock_quantity
                        db.execSQL("INSERT INTO products (id, name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice, sku, stock_quantity, gerePeremption, taxable, tauxTaxe, prixAchatUniteBase, isTemplate) VALUES (1, 'Vary', 3200.0, 'Alimentation', 50.0, '', 10.0, 'Kilogramme', '6111222333444', 3000.0, 'SKU-VARY-01', 50, 0, 0, 0.0, 3000.0, 0)")
                        db.execSQL("INSERT INTO products (id, name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice, sku, stock_quantity, gerePeremption, taxable, tauxTaxe, prixAchatUniteBase, isTemplate) VALUES (2, 'Karoty', 1500.0, 'Légumes', 20.0, '', 5.0, 'Kilogramme', '', 1300.0, 'SKU-KAROTY-02', 20, 0, 0, 0.0, 1300.0, 0)")
                        db.execSQL("INSERT INTO products (id, name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice, sku, stock_quantity, gerePeremption, taxable, tauxTaxe, prixAchatUniteBase, isTemplate) VALUES (3, 'Menaka', 7500.0, 'Alimentation', 30.0, '', 5.0, 'Litre', '3017620422003', 7000.0, 'SKU-MENAKA-03', 30, 0, 0, 0.0, 7000.0, 0)")
                        db.execSQL("INSERT INTO products (id, name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice, sku, stock_quantity, gerePeremption, taxable, tauxTaxe, prixAchatUniteBase, isTemplate) VALUES (4, 'Biski', 1000.0, 'Alimentation', 100.0, '', 10.0, 'Pièce', '3250541505351', 850.0, 'SKU-BISKI-04', 100, 0, 0, 0.0, 850.0, 0)")

                        // Pre-populate our robust Produits table
                        val now = System.currentTimeMillis()
                        db.execSQL("INSERT INTO produits (id, nom, nomCourt, categorie, sousCategorie, marque, description, uniteBase, quantiteStock, seuilAlerte, stockMax, emplacement, prixAchatUniteBase, fournisseurId, gerePeremption, imageUrl, codeBarrePrincipal, taxable, tauxTaxe, actif, dateAjout, dateDerniereMaj) VALUES (1, 'Vary', 'Vary', 'Alimentation', NULL, NULL, NULL, 'Kilogramme', 50.0, 10.0, NULL, NULL, 3000.0, NULL, 0, '', '6111222333444', 0, 0.0, 1, $now, $now)")
                        db.execSQL("INSERT INTO produits (id, nom, nomCourt, categorie, sousCategorie, marque, description, uniteBase, quantiteStock, seuilAlerte, stockMax, emplacement, prixAchatUniteBase, fournisseurId, gerePeremption, imageUrl, codeBarrePrincipal, taxable, tauxTaxe, actif, dateAjout, dateDerniereMaj) VALUES (2, 'Karoty', 'Karoty', 'Légumes', NULL, NULL, NULL, 'Kilogramme', 20.0, 5.0, NULL, NULL, 1300.0, NULL, 0, '', '', 0, 0.0, 1, $now, $now)")
                        db.execSQL("INSERT INTO produits (id, nom, nomCourt, categorie, sousCategorie, marque, description, uniteBase, quantiteStock, seuilAlerte, stockMax, emplacement, prixAchatUniteBase, fournisseurId, gerePeremption, imageUrl, codeBarrePrincipal, taxable, tauxTaxe, actif, dateAjout, dateDerniereMaj) VALUES (3, 'Menaka', 'Menaka', 'Alimentation', NULL, NULL, NULL, 'Litre', 30.0, 5.0, NULL, NULL, 7000.0, NULL, 0, '', '3017620422003', 0, 0.0, 1, $now, $now)")
                        db.execSQL("INSERT INTO produits (id, nom, nomCourt, categorie, sousCategorie, marque, description, uniteBase, quantiteStock, seuilAlerte, stockMax, emplacement, prixAchatUniteBase, fournisseurId, gerePeremption, imageUrl, codeBarrePrincipal, taxable, tauxTaxe, actif, dateAjout, dateDerniereMaj) VALUES (4, 'Biski', 'Biski', 'Alimentation', NULL, NULL, NULL, 'Pièce', 100.0, 10.0, NULL, NULL, 850.0, NULL, 0, '', '3250541505351', 0, 0.0, 1, $now, $now)")

                        // Pre-populate units_produit
                        db.execSQL("INSERT INTO unites_produit (id, produitId, nomUnite, facteurVersBase, prixVente, prixAchat, codeBarre, estUniteBase, estUniteVenteDefaut, ordre, actif) VALUES (1, 1, 'Kilogramme', 1.0, 3200.0, 3000.0, '6111222333444', 1, 1, 0, 1)")
                        db.execSQL("INSERT INTO unites_produit (id, produitId, nomUnite, facteurVersBase, prixVente, prixAchat, codeBarre, estUniteBase, estUniteVenteDefaut, ordre, actif) VALUES (2, 2, 'Kilogramme', 1.0, 1500.0, 1300.0, NULL, 1, 1, 0, 1)")
                        db.execSQL("INSERT INTO unites_produit (id, produitId, nomUnite, facteurVersBase, prixVente, prixAchat, codeBarre, estUniteBase, estUniteVenteDefaut, ordre, actif) VALUES (3, 3, 'Litre', 1.0, 7500.0, 7000.0, '3017620422003', 1, 1, 0, 1)")
                        db.execSQL("INSERT INTO unites_produit (id, produitId, nomUnite, facteurVersBase, prixVente, prixAchat, codeBarre, estUniteBase, estUniteVenteDefaut, ordre, actif) VALUES (4, 4, 'Pièce', 1.0, 1000.0, 850.0, '3250541505351', 1, 1, 0, 1)")
                    }
                })
                .addMigrations(MIGRATION_20_21, MIGRATION_21_22)
                // F4 — Filet de sécurité volontairement restreint.
                //
                // Avant : `fallbackToDestructiveMigration(true)`, c'est-à-dire « en cas de doute,
                // efface tout ». En développement c'est commode ; en production, avec des
                // épiceries qui ont des mois de ventes et de dettes en base, la première montée de
                // version livrée sans migration écrite aurait effacé leur historique en silence,
                // sans message, sans récupération possible.
                //
                // Désormais la destruction n'est tolérée que pour les versions 1 à 19, celles
                // d'avant la mise en production — de toute façon déjà effacées par ce même
                // mécanisme dans les versions précédentes. À partir de la 20, toute évolution de
                // schéma DOIT s'accompagner d'une migration : à défaut, l'app plante bruyamment
                // au premier lancement de développement, ce qui se remarque tout de suite,
                // au lieu de détruire silencieusement les données d'un client.
                .fallbackToDestructiveMigrationFrom(
                    true,
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
