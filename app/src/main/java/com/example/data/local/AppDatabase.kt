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

@Database(entities = [Product::class, Sale::class, Debt::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun debtDao(): DebtDao

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
                        // Pre-populate the products table with default values including barcodes and wholesale prices
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice) VALUES ('Vary', 3200.0, 'Alimentation', 50.0, '', 10.0, 'Kilogramme', '6111222333444', 3000.0)")
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice) VALUES ('Karoty', 1500.0, 'Légumes', 20.0, '', 5.0, 'Kilogramme', '', 1300.0)")
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice) VALUES ('Menaka', 7500.0, 'Alimentation', 30.0, '', 5.0, 'Litre', '3017620422003', 7000.0)")
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit, barcode, wholesalePrice) VALUES ('Biski', 1000.0, 'Alimentation', 100.0, '', 10.0, 'Pièce', '3250541505351', 850.0)")
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
