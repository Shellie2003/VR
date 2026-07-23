package com.example.sync

import org.json.JSONArray
import org.json.JSONObject

object SyncSerializer {

    fun serializeMessage(message: SyncMessage): String {
        val obj = JSONObject()
        obj.put("type", message.type)
        obj.put("payload", message.payload)
        obj.put("senderId", message.senderId)
        obj.put("timestamp", message.timestamp)
        return obj.toString()
    }

    fun deserializeMessage(jsonStr: String): SyncMessage {
        val obj = JSONObject(jsonStr)
        return SyncMessage(
            type = obj.getString("type"),
            payload = obj.getString("payload"),
            senderId = obj.getString("senderId"),
            timestamp = obj.getLong("timestamp")
        )
    }

    fun serializeSalePayload(payload: SalePayload): String {
        val obj = JSONObject()
        obj.put("saleJson", payload.saleJson)
        val arr = JSONArray()
        payload.itemsToDeduct.forEach {
            val item = JSONObject()
            item.put("productId", it.productId)
            item.put("quantity", it.quantity)
            arr.put(item)
        }
        obj.put("itemsToDeduct", arr)
        return obj.toString()
    }

    fun deserializeSalePayload(jsonStr: String): SalePayload {
        val obj = JSONObject(jsonStr)
        val saleJson = obj.getString("saleJson")
        val arr = obj.getJSONArray("itemsToDeduct")
        val list = mutableListOf<ProductDeduction>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            list.add(
                ProductDeduction(
                    productId = item.getString("productId"),
                    quantity = item.getDouble("quantity")
                )
            )
        }
        return SalePayload(saleJson, list)
    }

    fun serializeStockUpdate(payload: StockUpdatePayload): String {
        val obj = JSONObject()
        obj.put("productId", payload.productId)
        obj.put("newQuantity", payload.newQuantity)
        return obj.toString()
    }

    fun deserializeStockUpdate(jsonStr: String): StockUpdatePayload {
        val obj = JSONObject(jsonStr)
        return StockUpdatePayload(
            productId = obj.getString("productId"),
            newQuantity = obj.getDouble("newQuantity")
        )
    }

    // Bidirectional Full Database Sync Serialization (Decoupled from App Models)
    fun serializeFullSync(
        productsJson: String,
        salesJson: String,
        debtsJson: String,
        restocksJson: String = "[]",
        mouvementsCaisseJson: String = "[]",
        caisseSessionsJson: String = "[]",
        vendeursJson: String = "[]"
    ): String {
        val root = JSONObject()
        root.put("products", JSONArray(productsJson))
        root.put("sales", JSONArray(salesJson))
        root.put("debts", JSONArray(debtsJson))
        root.put("restocks", JSONArray(restocksJson))
        root.put("mouvementsCaisse", JSONArray(mouvementsCaisseJson))
        root.put("caisseSessions", JSONArray(caisseSessionsJson))
        root.put("vendeurs", JSONArray(vendeursJson))
        return root.toString()
    }

    fun deserializeFullSync(jsonStr: String): Map<String, String> {
        val root = JSONObject(jsonStr)
        val result = mutableMapOf<String, String>()
        result["products"] = root.optJSONArray("products")?.toString() ?: "[]"
        result["sales"] = root.optJSONArray("sales")?.toString() ?: "[]"
        result["debts"] = root.optJSONArray("debts")?.toString() ?: "[]"
        result["restocks"] = root.optJSONArray("restocks")?.toString() ?: "[]"
        result["mouvementsCaisse"] = root.optJSONArray("mouvementsCaisse")?.toString() ?: "[]"
        result["caisseSessions"] = root.optJSONArray("caisseSessions")?.toString() ?: "[]"
        result["vendeurs"] = root.optJSONArray("vendeurs")?.toString() ?: "[]"
        return result
    }
}
