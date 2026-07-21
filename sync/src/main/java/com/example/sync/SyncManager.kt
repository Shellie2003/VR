package com.example.sync

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SyncManager {
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        log("Hadisoana Coroutine: ${exception.localizedMessage ?: exception.message}")
        exception.printStackTrace()
    }
    private val mainScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + exceptionHandler)

    var syncBridge: SyncBridge? = null
    val deviceId: String = UUID.randomUUID().toString().take(6).uppercase()

    private val _connectionStatus = MutableStateFlow("Disconnected") // "Disconnected", "Hosting (Server)", "Connected (Client)", "Connecting"
    val connectionStatus = _connectionStatus.asStateFlow()

    private val _isServer = MutableStateFlow(false)
    val isServer = _isServer.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val _serverIp = MutableStateFlow<String?>(null)
    val serverIp = _serverIp.asStateFlow()

    private val _clientsCount = MutableStateFlow(0)
    val clientsCount = _clientsCount.asStateFlow()

    // Key: saleId, Value: Deferred completion status
    private val pendingSaleDeferreds = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    
    private val activeClientWriters = ConcurrentHashMap<String, PrintWriter>()
    
    private var serverJob: Job? = null
    private var clientJob: Job? = null

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            log("Raharaha IP hadisoana: ${ex.message}")
        }
        return null
    }

    private fun log(message: String) {
        syncBridge?.logMessage(message)
    }

    fun startServer(context: Context) {
        stopAll()
        _connectionStatus.value = "Starting Server..."
        _isServer.value = true

        val ip = getLocalIpAddress() ?: "127.0.0.1"
        _serverIp.value = ip

        // Start Foreground Service to keep server alive
        val intent = Intent(context, SyncService::class.java).apply {
            action = SyncService.ACTION_START_SERVER
        }
        ContextCompat.startForegroundService(context, intent)

        serverJob = mainScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8080)
                _connectionStatus.value = "Hosting (Server)"
                _isConnected.value = true
                log("Server nandeha amin'ny $ip:8080")

                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    handleNewClient(client)
                }
            } catch (ex: Exception) {
                if (ex !is SocketException) {
                    log("Hadisoana Server: ${ex.message}")
                }
            } finally {
                _connectionStatus.value = "Disconnected"
                _isConnected.value = false
                _serverIp.value = null
            }
        }
    }

    private fun handleNewClient(client: Socket) {
        mainScope.launch(Dispatchers.IO) {
            val clientIp = client.inetAddress.hostAddress
            log("Misy mpanjifa mifandray: $clientIp")
            
            var writer: PrintWriter? = null
            var reader: BufferedReader? = null
            var clientId = "UNKNOWN"

            try {
                writer = PrintWriter(client.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(client.getInputStream()))

                // Read hello
                val firstLine = reader.readLine() ?: return@launch
                val firstMsg = SyncSerializer.deserializeMessage(firstLine)
                if (firstMsg.type == "HELLO") {
                    clientId = firstMsg.senderId
                    activeClientWriters[clientId] = writer
                    _clientsCount.value = activeClientWriters.size
                    log("Mpanjifa voamarina: $clientId")

                    // Send initial database sync
                    val dbJson = syncBridge?.getAllProductsJson() ?: "{}"
                    val syncMsg = SyncMessage("INITIAL_SYNC", dbJson, deviceId)
                    writer.println(SyncSerializer.serializeMessage(syncMsg))
                }

                // Listen for transactions
                while (isActive) {
                    val line = reader.readLine() ?: break
                    val msg = SyncSerializer.deserializeMessage(line)
                    handleServerReceivedMessage(clientId, msg, writer)
                }
            } catch (ex: Exception) {
                log("Nisara-mifandray ny mpanjifa $clientId: ${ex.message}")
            } finally {
                if (clientId != "UNKNOWN") {
                    activeClientWriters.remove(clientId)
                    _clientsCount.value = activeClientWriters.size
                }
                try { client.close() } catch (e: Exception) {}
            }
        }
    }

    private fun handleServerReceivedMessage(clientId: String, msg: SyncMessage, writer: PrintWriter) {
        when (msg.type) {
            "HEARTBEAT" -> {
                val response = SyncMessage("HEARTBEAT_ACK", "", deviceId)
                writer.println(SyncSerializer.serializeMessage(response))
            }
            "CLIENT_DATABASE_SYNC" -> {
                mainScope.launch(Dispatchers.IO) {
                    try {
                        log("Nahazo tahiry ho ampitoviana avy amin'ny mpanjifa $clientId")
                        syncBridge?.handleFullDatabaseSync(msg.payload)
                        // Get the newly consolidated database snapshot
                        val consolidatedDb = syncBridge?.getFullDatabaseJson() ?: "{}"
                        // Send consolidated snapshot back to all connected clients
                        broadcastFullDatabase(consolidatedDb)
                    } catch (ex: Exception) {
                        log("Hadisoana nandritra ny fampitoviana avy amin'ny $clientId: ${ex.message}")
                    }
                }
            }
            "REQUEST_SALE" -> {
                mainScope.launch(Dispatchers.IO) {
                    try {
                        val payload = SyncSerializer.deserializeSalePayload(msg.payload)
                        log("Fangatahana varotra avy amin'ny $clientId: ${payload.itemsToDeduct.size} entana")
                        
                        // Strict conflict resolution / Stock check: First Come, First Served
                        var canProceed = true
                        
                        // 1. Check & Reserve Stock under synchronous block lock
                        synchronized(this@SyncManager) {
                            for (item in payload.itemsToDeduct) {
                                val hasStock = syncBridge?.handleReserveStock(item.productId, item.quantity) ?: false
                                if (!hasStock) {
                                    canProceed = false
                                    log("Tsy ampy tahiry ho an'ny ${item.productId}!")
                                    break
                                }
                            }

                            if (canProceed) {
                                // 2. Commit transaction on Server database
                                val committed = syncBridge?.handleCommitSale(payload.saleJson) ?: false
                                if (!committed) {
                                    canProceed = false
                                }
                            }
                        }

                        if (canProceed) {
                            log("Nahomby ny varotra nataon'ny $clientId! Mandefa fankatoavana...")
                            val resp = SyncMessage("RESPONSE_SALE", "SUCCESS", deviceId)
                            writer.println(SyncSerializer.serializeMessage(resp))

                            // 3. Broadcast real-time stock updates to ALL other connected clients
                            payload.itemsToDeduct.forEach {
                                broadcastStockUpdate(it.productId)
                            }
                        } else {
                            log("Tsy nekena ny varotra nataon'ny $clientId: Tsy ampy tahiry")
                            val resp = SyncMessage("RESPONSE_SALE", "FAILURE", deviceId)
                            writer.println(SyncSerializer.serializeMessage(resp))
                        }
                    } catch (ex: Exception) {
                        log("Hadisoana teo am-pikarakarana ny varotra: ${ex.message}")
                        val resp = SyncMessage("RESPONSE_SALE", "ERROR", deviceId)
                        writer.println(SyncSerializer.serializeMessage(resp))
                    }
                }
            }
        }
    }

    private fun broadcastStockUpdate(productId: String) {
        mainScope.launch(Dispatchers.IO) {
            // Retrieve current inventory level
            val productsStr = syncBridge?.getAllProductsJson() ?: "[]"
            // Find specific item quantity (to avoid sending full catalog updates over and over)
            val jsonArr = org.json.JSONArray(productsStr)
            var qty = 0.0
            for (i in 0 until jsonArr.length()) {
                val obj = jsonArr.getJSONObject(i)
                if (obj.getString("barcode") == productId || obj.getString("name") == productId) {
                    // Let's match by barcode or ID
                    // We'll broadcast individual updates
                }
            }
            
            // To be totally bulletproof, let's broadcast the FULL updated products json or individual quantities.
            // Let's send the full updated products json to ensure 100% convergence.
            val fullCatalog = syncBridge?.getAllProductsJson() ?: "[]"
            val broadcastMsg = SyncMessage("STOCK_UPDATE", fullCatalog, deviceId)
            val msgStr = SyncSerializer.serializeMessage(broadcastMsg)
            
            activeClientWriters.values.forEach { writer ->
                try {
                    writer.println(msgStr)
                } catch (e: Exception) {}
            }
        }
    }

    fun connectToServer(context: Context, hostIp: String, port: Int = 8080) {
        stopAll()
        _connectionStatus.value = "Connecting..."
        _isServer.value = false

        // Start client foreground service
        val intent = Intent(context, SyncService::class.java).apply {
            action = SyncService.ACTION_START_CLIENT
            putExtra("EXTRA_HOST", hostIp)
            putExtra("EXTRA_PORT", port)
        }
        ContextCompat.startForegroundService(context, intent)

        clientJob = mainScope.launch(Dispatchers.IO) {
            var socket: Socket? = null
            var writer: PrintWriter? = null
            var reader: BufferedReader? = null

            try {
                log("Mifandray amin'ny server: $hostIp:$port...")
                socket = Socket(hostIp, port)
                clientSocket = socket
                writer = PrintWriter(socket.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                // Send hello identify
                val hello = SyncMessage("HELLO", "", deviceId)
                writer.println(SyncSerializer.serializeMessage(hello))

                _connectionStatus.value = "Connected (Client)"
                _isConnected.value = true
                log("Tafandray tsara tamin'ny Server!")

                // Push our client-side database to the server to start the bidirectional merge
                val fullDbJson = syncBridge?.getFullDatabaseJson() ?: "{}"
                val clientSyncMsg = SyncMessage("CLIENT_DATABASE_SYNC", fullDbJson, deviceId)
                writer.println(SyncSerializer.serializeMessage(clientSyncMsg))
                log("Nandefa ny tahiry rehetra tamin'ny Server mba hampitoviana...")

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val msg = SyncSerializer.deserializeMessage(line)
                    handleClientReceivedMessage(msg)
                }
            } catch (ex: Exception) {
                log("Tapaka ny fifandraisana tamin'ny Server: ${ex.message}")
            } finally {
                _connectionStatus.value = "Disconnected"
                _isConnected.value = false
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }

    private fun handleClientReceivedMessage(msg: SyncMessage) {
        when (msg.type) {
            "INITIAL_SYNC" -> {
                log("Nahazo tahiry voalohany avy amin'ny Server")
                syncBridge?.handleSyncStock(msg.payload)
            }
            "STOCK_UPDATE" -> {
                log("Nahazo fanavaozana tahiry vaovao avy amin'ny Server")
                syncBridge?.handleSyncStock(msg.payload)
            }
            "SERVER_DATABASE_SYNC" -> {
                log("Nahazo tahiry voalamina feno avy amin'ny Server")
                syncBridge?.handleFullDatabaseSync(msg.payload)
            }
            "RESPONSE_SALE" -> {
                log("Valin'ny varotra: ${msg.payload}")
                val success = msg.payload == "SUCCESS"
                // Resolve the first pending deferred in FIFO order
                val firstKey = pendingSaleDeferreds.keys().asSequence().firstOrNull()
                if (firstKey != null) {
                    pendingSaleDeferreds.remove(firstKey)?.complete(success)
                }
            }
        }
    }

    suspend fun requestSaleOnServer(saleJson: String, deductions: List<ProductDeduction>): Boolean {
        if (!_isConnected.value || _isServer.value) {
            // Local fallback if disconnected or is server itself
            return false
        }

        val deferred = CompletableDeferred<Boolean>()
        val requestUuid = UUID.randomUUID().toString()
        pendingSaleDeferreds[requestUuid] = deferred

        return withContext(Dispatchers.IO) {
            try {
                val payload = SalePayload(saleJson, deductions)
                val payloadStr = SyncSerializer.serializeSalePayload(payload)
                val msg = SyncMessage("REQUEST_SALE", payloadStr, deviceId)

                val socket = clientSocket
                if (socket != null && !socket.isClosed) {
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println(SyncSerializer.serializeMessage(msg))
                    log("Nandefa fangatahana varotra tamin'ny Server...")

                    // Wait with timeout of 5 seconds to ensure UI doesn't hang forever
                    withTimeout(5000) {
                        deferred.await()
                    }
                } else {
                    log("Hadisoana: Tsy mifandray amin'ny Server ny fitaovana")
                    false
                }
            } catch (ex: TimeoutCancellationException) {
                log("Lany ny fotoana niandrasana ny fankatoavan'ny Server (Timeout)")
                pendingSaleDeferreds.remove(requestUuid)
                false
            } catch (ex: Exception) {
                log("Hadisoana nandritra ny fangatahana varotra: ${ex.message}")
                pendingSaleDeferreds.remove(requestUuid)
                false
            }
        }
    }

    fun broadcastFullDatabase(dbJson: String) {
        mainScope.launch(Dispatchers.IO) {
            val broadcastMsg = SyncMessage("SERVER_DATABASE_SYNC", dbJson, deviceId)
            val msgStr = SyncSerializer.serializeMessage(broadcastMsg)
            activeClientWriters.values.forEach { writer ->
                try {
                    writer.println(msgStr)
                } catch (e: Exception) {}
            }
        }
    }

    fun triggerDatabaseSync() {
        mainScope.launch(Dispatchers.IO) {
            val dbJson = syncBridge?.getFullDatabaseJson() ?: "{}"
            if (_isServer.value) {
                log("Mandefa fampitoviana tahiry vaovao amin'ny mpanjifa rehetra...")
                broadcastFullDatabase(dbJson)
            } else if (_isConnected.value) {
                log("Mandefa fanavaozana tahiry tamin'ny Server...")
                val socket = clientSocket
                if (socket != null && !socket.isClosed) {
                    try {
                        val writer = PrintWriter(socket.getOutputStream(), true)
                        val syncMsg = SyncMessage("CLIENT_DATABASE_SYNC", dbJson, deviceId)
                        writer.println(SyncSerializer.serializeMessage(syncMsg))
                    } catch (e: Exception) {
                        log("Hadisoana nandefa fampitoviana tamin'ny Server: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopAll() {
        serverJob?.cancel()
        clientJob?.cancel()
        
        try { serverSocket?.close() } catch (e: Exception) {}
        try { clientSocket?.close() } catch (e: Exception) {}
        
        serverSocket = null
        clientSocket = null

        activeClientWriters.clear()
        pendingSaleDeferreds.clear()

        _connectionStatus.value = "Disconnected"
        _isConnected.value = false
        _isServer.value = false
        _serverIp.value = null
        _clientsCount.value = 0
    }
}
