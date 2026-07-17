package com.example.sync

interface SyncBridge {
    fun handleReserveStock(productId: String, quantity: Double): Boolean
    fun handleCommitSale(saleJson: String): Boolean
    fun handleUpdateStockGlobal(productId: String, newQuantity: Double)
    fun getAllProductsJson(): String
    fun handleSyncStock(stockJson: String)
    fun logMessage(text: String)
    fun getFullDatabaseJson(): String
    fun handleFullDatabaseSync(syncJson: String)
}
