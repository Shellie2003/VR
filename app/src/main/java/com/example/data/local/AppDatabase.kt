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
        MouvementCaisse::class
    ],
    version = 16,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun debtDao(): DebtDao
    abstract fun restockDao(): RestockDao
    abstract fun mouvementCaisseDao(): MouvementCaisseDao

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
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
