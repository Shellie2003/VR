package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QrCodeScannerView
import com.example.ui.viewmodel.InventoryViewModel
import com.example.sync.SyncManager
import com.example.sync.SyncService
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch

// Helper to generate ZXing QR codes safely in-memory as Compose ImageBitmaps
fun generateQrCodeBitmap(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bmp.setPixel(
                    x, y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bmp
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeLang by viewModel.language.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    
    // Core states retrieved from SyncManager
    val connectionStatus by SyncManager.connectionStatus.collectAsState()
    val isServerMode by SyncManager.isServer.collectAsState()
    val isConnected by SyncManager.isConnected.collectAsState()
    val serverIp by SyncManager.serverIp.collectAsState()
    val clientsCount by SyncManager.clientsCount.collectAsState()
    val logs by viewModel.syncLogs.collectAsState()

    // Screen display configurations
    var isScannerOpen by remember { mutableStateOf(false) }
    var selectedRoleTab by remember { mutableStateOf(if (isServerMode) 0 else 1) }

    val isDark = MaterialTheme.colorScheme.background == Color(0xFF002114)
    val cardBg = if (isDark) Color(0xFF1B4332) else Color(0xFFF8FAFC)
    val cardBorderColor = if (isDark) Color(0xFF2C5E43) else Color(0xFFE2E8F0)
    val mainTextColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Localized texts
    val titleText = when (activeLang) {
        "mg" -> "Mampitohy finday maro (Sync)"
        "fr" -> "Synchronisation multi-terminal"
        else -> "Multi-Terminal Sync"
    }

    val subtitleText = when (activeLang) {
        "mg" -> "Fampitahana ny tahiry mivantana tsy mila Internet"
        "fr" -> "Synchronisez vos stocks et ventes sans internet"
        else -> "Synchronize stock & sales in real-time offline"
    }

    val tabServerTitle = when (activeLang) {
        "mg" -> "SERVER (Gérant)"
        "fr" -> "SERVEUR (Gérant)"
        else -> "SERVER (Manager)"
    }

    val tabClientTitle = when (activeLang) {
        "mg" -> "CLIENT (Vendeur)"
        "fr" -> "CLIENT (Vendeur)"
        else -> "CLIENT (Sales)"
    }

    val logsTitle = when (activeLang) {
        "mg" -> "Tantaran'ny Fampifandraisana (Logs)"
        "fr" -> "Historique de synchronisation"
        else -> "Synchronization logs"
    }

    val btnStopSync = when (activeLang) {
        "mg" -> "Hampiato ny Fampifandraisana"
        "fr" -> "Arrêter la synchronisation"
        else -> "Stop Synchronization"
    }

    val btnStartServer = when (activeLang) {
        "mg" -> "Handefa ny Server"
        "fr" -> "Démarrer le Serveur"
        else -> "Start Server"
    }

    val btnStartScanner = when (activeLang) {
        "mg" -> "Hizaha ny QR Code"
        "fr" -> "Scanner le code QR"
        else -> "Scan QR Code"
    }

    // Handle scanner callback
    if (isScannerOpen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            QrCodeScannerView(
                onQrCodeScanned = { qrText ->
                    isScannerOpen = false
                    val ipTrimmed = qrText.trim()
                    if (ipTrimmed.isNotEmpty()) {
                        coroutineScope.launch {
                            viewModel.addSyncLog("Mifandray amin'ny Server $ipTrimmed...")
                            SyncManager.connectToServer(context, ipTrimmed)
                        }
                    }
                },
                onClose = { isScannerOpen = false },
                language = activeLang,
                themeColor = themeColor
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = mainTextColor
                            )
                        )
                        Text(
                            text = subtitleText,
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Hiverina",
                            tint = mainTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Connection Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val statusColor = when {
                        connectionStatus.startsWith("Hosting") -> Color(0xFF10B981)
                        connectionStatus.startsWith("Connected") -> Color(0xFF3B82F6)
                        connectionStatus == "Connecting" -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )

                    Column {
                        Text(
                            text = when (connectionStatus) {
                                "Disconnected" -> if (activeLang == "mg") "Tsy misy fifandraisana" else "Déconnecté"
                                "Hosting (Server)" -> if (activeLang == "mg") "Mandefa Server (Gérant)" else "Serveur actif (Gérant)"
                                "Connected (Client)" -> if (activeLang == "mg") "Mifandray (Client Vendeur)" else "Connecté au serveur (Vendeur)"
                                "Connecting" -> if (activeLang == "mg") "Eo am-pifandraisana..." else "Connexion en cours..."
                                else -> connectionStatus
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = mainTextColor
                        )
                        Text(
                            text = if (isServerMode) {
                                if (activeLang == "mg") "Fitaovana mandray varotra hafa" else "Votre appareil sert de base de données"
                            } else {
                                if (activeLang == "mg") "Mampita varotra amin'ny Server" else "Envoie vos ventes au gérant en temps réel"
                            },
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Tab selector if disconnected
            if (!isConnected) {
                TabRow(
                    selectedTabIndex = selectedRoleTab,
                    containerColor = cardBg,
                    contentColor = themeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, cardBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedRoleTab == 0,
                        onClick = { selectedRoleTab = 0 },
                        text = { Text(tabServerTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedRoleTab == 1,
                        onClick = { selectedRoleTab = 1 },
                        text = { Text(tabClientTitle, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        icon = { Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action panel according to tabs
                if (selectedRoleTab == 0) {
                    // SERVER MODE PREPARATION
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Router,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (activeLang == "mg") {
                                    "Ity finday ity no ho Server (Gérant). Ny fitaovana hafa rehetra dia hifandray eto mba hampitahana ny tahiry mivantana."
                                } else {
                                    "Cet appareil sera le Serveur Principal (Gérant). Les terminaux vendeurs s'y connecteront directement."
                                },
                                fontSize = 13.sp,
                                color = mainTextColor,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    viewModel.addSyncLog("Manomboka Server...")
                                    SyncManager.startServer(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(btnStartServer, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent().apply {
                                            action = "android.settings.TETHER_SETTINGS"
                                        }
                                        context.startActivity(intent)
                                        viewModel.addSyncLog(if (activeLang == "mg") "Manokatra fampifandraisana Hotspot..." else "Ouverture des paramètres du Point d'accès...")
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.WifiTethering, contentDescription = null, tint = themeColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeLang == "mg") "Hampandeha Point d'accès (Hotspot)" else "Activer le Point d'accès (Hotspot)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    // CLIENT MODE PREPARATION
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = if (activeLang == "mg") {
                                    "Ity finday ity no CLIENT (Vendeur). Tsindrio ny bokotra etsy ambany mba hizaha ny kaody QR eo amin'ny findain'ny gérant."
                                } else {
                                    "Cet appareil sera le Client (Vendeur). Cliquez pour scanner le code QR affiché sur le téléphone du gérant."
                                },
                                fontSize = 13.sp,
                                color = mainTextColor,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = { isScannerOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(btnStartScanner, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                                        context.startActivity(intent)
                                        viewModel.addSyncLog(if (activeLang == "mg") "Manokatra fampifandraisana Wi-Fi..." else "Ouverture des paramètres Wi-Fi...")
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                                border = androidx.compose.foundation.BorderStroke(1.dp, themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = themeColor)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeLang == "mg") "Hikaroka réseau Wi-Fi" else "Rechercher un réseau Wi-Fi",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // ACTIVE CONNECTION MODULES
                if (isServerMode) {
                    // SERVER RUNNING DETAILS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val ipAddr = serverIp ?: "127.0.0.1"

                            Text(
                                text = if (activeLang == "mg") "Kaody QR ho an'ny fitaovana hafa" else "Code QR de connexion",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = mainTextColor
                            )

                            // Render ZXing bitmap
                            val qrBitmap = remember(ipAddr) {
                                generateQrCodeBitmap(ipAddr, 350)
                            }

                            if (qrBitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Pairing Code",
                                    modifier = Modifier
                                        .size(160.dp)
                                        .border(2.dp, themeColor, RoundedCornerShape(8.dp))
                                        .padding(4.dp)
                                )
                            }

                            Text(
                                text = "IP: $ipAddr (Port: 8080)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = themeColor
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(themeColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeLang == "mg") {
                                        "Fitaovana mifandray mivantana: $clientsCount"
                                    } else {
                                        "Terminaux connectés : $clientsCount"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = themeColor
                                )
                            }

                            Button(
                                onClick = {
                                    SyncManager.triggerDatabaseSync()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeLang == "mg") "Ampitoviana mivantana ny tahiry" else "Forcer la synchronisation",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(context, SyncService::class.java).apply {
                                        action = SyncService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                    viewModel.addSyncLog("Hajanona ny Server.")
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(btnStopSync, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // CLIENT RUNNING DETAILS
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(48.dp)
                            )

                            Text(
                                text = if (activeLang == "mg") {
                                    "Mifandray tsara amin'ny Server!"
                                } else {
                                    "Mise en réseau réussie !"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF10B981)
                            )

                            Text(
                                text = "IP Server: ${serverIp ?: "—"}",
                                fontSize = 13.sp,
                                color = mainTextColor
                              )

                            Button(
                                onClick = {
                                    SyncManager.triggerDatabaseSync()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (activeLang == "mg") "Ampitoviana mivantana ny tahiry" else "Forcer la synchronisation",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(context, SyncService::class.java).apply {
                                        action = SyncService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                    viewModel.addSyncLog("Nisara-mifandray tamin'ny Server.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(btnStopSync, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Real-Time Logging Console
            Text(
                text = logsTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = mainTextColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                val listState = rememberLazyListState()

                // Scroll to bottom on append automatically
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        listState.animateScrollToItem(logs.size - 1)
                    }
                }

                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (activeLang == "mg") "Tsy misy fandraisana mbola mandeha" else "Aucune activité",
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(logs, key = { index, _ -> index }) { _, logMsg ->
                            Text(
                                text = logMsg,
                                color = if (logMsg.contains("Hadisoana") || logMsg.contains("Erreur") || logMsg.contains("Tsy ampy")) {
                                    Color(0xFFF87171) // Red
                                } else if (logMsg.contains("Nahomby") || logMsg.contains("succès") || logMsg.contains("Tafandray")) {
                                    Color(0xFF34D399) // Green
                                } else {
                                    Color(0xFF94A3B8) // Grey console
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
