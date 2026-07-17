package com.example.sync

data class SyncMessage(
    val type: String,       // "HELLO", "INITIAL_SYNC", "REQUEST_SALE", "RESPONSE_SALE", "STOCK_UPDATE", "HEARTBEAT"
    val payload: String,    // Payload content (JSON formatted or simple text)
    val senderId: String,   // Unique identifier of the sending device
    val timestamp: Long = System.currentTimeMillis()
)

data class SalePayload(
    val saleJson: String,
    val itemsToDeduct: List<ProductDeduction>
)

data class ProductDeduction(
    val productId: String,
    val quantity: Double
)

data class StockUpdatePayload(
    val productId: String,
    val newQuantity: Double
)
