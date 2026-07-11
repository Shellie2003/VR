package com.example.data.local

import androidx.room.*
import com.example.data.model.Product
import com.example.data.model.Sale
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC LIMIT :limit")
    fun getLimitedProducts(limit: Int): Flow<List<Product>>

    @Query("SELECT DISTINCT category FROM products WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM products LIMIT 1)")
    fun hasProducts(): Flow<Boolean>

    @Query("""
        SELECT * FROM products 
        WHERE (:category = 'All' OR category = :category)
        AND (:showLowStockOnly = 0 OR stock < lowStockThreshold)
        AND (:query = '' OR name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%')
        ORDER BY name ASC 
        LIMIT 100
    """)
    fun searchProducts(query: String, category: String, showLowStockOnly: Boolean): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE barcode != '' ORDER BY name ASC LIMIT 50")
    fun getProductsWithBarcodes(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale)

    @Delete
    suspend fun deleteSale(sale: Sale)
}
