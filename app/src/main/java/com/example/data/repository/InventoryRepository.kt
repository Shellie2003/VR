package com.example.data.repository

import com.example.data.local.ProductDao
import com.example.data.local.SaleDao
import com.example.data.local.DebtDao
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.Debt
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val debtDao: DebtDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allSales: Flow<List<Sale>> = saleDao.getAllSales()
    val allDebts: Flow<List<Debt>> = debtDao.getAllDebts()

    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    suspend fun deleteProductById(id: Int) = productDao.deleteProductById(id)
    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)

    suspend fun insertSale(sale: Sale) = saleDao.insertSale(sale)
    suspend fun deleteSale(sale: Sale) = saleDao.deleteSale(sale)

    suspend fun insertDebt(debt: Debt) = debtDao.insertDebt(debt)
    suspend fun updateDebt(debt: Debt) = debtDao.updateDebt(debt)
    suspend fun deleteDebt(debt: Debt) = debtDao.deleteDebt(debt)
}
