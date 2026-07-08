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

@Database(entities = [Product::class, Sale::class, Debt::class], version = 4, exportSchema = false)
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
                    "varotra_database_v3"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate the products table with default values
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit) VALUES ('Vary', 3200.0, 'Alimentation', 50.0, '', 10.0, 'Kilogramme')")
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit) VALUES ('Karoty', 1500.0, 'Légumes', 20.0, '', 5.0, 'Kilogramme')")
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit) VALUES ('Menaka', 7500.0, 'Alimentation', 30.0, '', 5.0, 'Litre')")
                        db.execSQL("INSERT INTO products (name, price, category, stock, imageUrl, lowStockThreshold, unit) VALUES ('Biski', 1000.0, 'Alimentation', 100.0, '', 10.0, 'Pièce')")
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
