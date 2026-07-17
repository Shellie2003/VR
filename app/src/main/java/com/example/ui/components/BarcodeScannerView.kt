@file:kotlin.OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class
)

package com.example.ui.components

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// Helper to trigger audio and haptic feedback on successful scan
private fun triggerSuccessFeedback(context: Context) {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                toneGen.release()
            } catch (e: Exception) {
                // Ignore silent release exceptions
            }
        }, 300)
    } catch (e: Exception) {
        Log.e("BarcodeScanner", "Failed to play beep sound", e)
    }

    try {
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(80)
            }
        }
    } catch (e: Exception) {
        Log.e("BarcodeScanner", "Failed to perform haptic feedback", e)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    language: String = "fr",
    themeColor: Color = MaterialTheme.colorScheme.primary,
    immediateMode: Boolean = false,
    continuousMode: Boolean = false,
    viewModel: com.example.ui.viewmodel.InventoryViewModel? = null,
    barcodeFormats: IntArray = intArrayOf(
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_CODE_93,
        Barcode.FORMAT_CODABAR,
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_ITF,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E
    )
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var manualBarcodeValue by remember { mutableStateOf("") }
    var sessionScannedProductIds by remember { mutableStateOf(setOf<Int>()) }
    var editingCartItem by remember { mutableStateOf<com.example.ui.viewmodel.CartItem?>(null) }
    var editingQuantityStr by remember { mutableStateOf("") }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val hasCameraPermission = cameraPermissionState.status.isGranted

    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var currentZoom by remember { mutableStateOf(1.0f) }

    val options = remember(barcodeFormats) {
        val builder = BarcodeScannerOptions.Builder()
        if (barcodeFormats.isNotEmpty()) {
            val first = barcodeFormats[0]
            val rest = barcodeFormats.drop(1).toIntArray()
            builder.setBarcodeFormats(first, *rest)
        } else {
            builder.setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        }
        builder.build()
    }
    val scanner = remember(options) {
        BarcodeScanning.getClient(options)
    }
    DisposableEffect(scanner) {
        onDispose {
            scanner.close()
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(cameraExecutor) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var lastScanTime by remember { mutableStateOf(0L) }

    // Request camera permission in UI background if missing
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Interactive titles and localized texts
    val scanTitle = when (language) {
        "mg" -> "Sary Scanner"
        "fr" -> "Scanner de Code-barres"
        else -> "Barcode Scanner"
    }

    val placeholderDesc = when (language) {
        "mg" -> "Ampiasao ity simulation ity mba hampidirana kaody bar sy hanamarinana ny fandehan'ny fampiharana."
        "fr" -> "Utilisez cette simulation pour saisir un code-barres et valider le comportement de l'application."
        else -> "Use this simulation to enter a barcode and validate the application behavior."
    }

    val labelBarcode = when (language) {
        "mg" -> "Kaody Bar"
        "fr" -> "Code-barres à scanner"
        else -> "Barcode to scan"
    }

    val btnSimulate = when (language) {
        "mg" -> "Alefaso Scanner"
        "fr" -> "Simuler le Scan"
        else -> "Simulate Scan"
    }

    val infoPermission = when (language) {
        "mg" -> "Mila fahazoan-dalana hampiasa fakantsary ny scanner."
        "fr" -> "Le scanner requiert l'accès à l'appareil photo."
        else -> "The scanner requires camera access."
    }

    fun handleScannedBarcode(scannedCode: String) {
        val trimmed = scannedCode.trim()
        if (trimmed.isNotEmpty()) {
            lastScannedCode = trimmed
            triggerSuccessFeedback(context)
            if (viewModel != null) {
                coroutineScope.launch {
                    val match = viewModel.getProductByBarcode(trimmed)
                    if (match != null) {
                        viewModel.addToCart(match, 1.0)
                        sessionScannedProductIds = sessionScannedProductIds + match.id
                    }
                }
            }
            onBarcodeScanned(trimmed)
            if (!continuousMode) {
                onClose()
            }
        }
    }

    fun handleSimulatedScan() {
        val trimmed = manualBarcodeValue.trim()
        if (trimmed.isNotEmpty()) {
            handleScannedBarcode(trimmed)
            if (continuousMode) {
                manualBarcodeValue = "" // Reset for next scan in continuous mode
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black // Dark background for scanner aesthetic
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Top Content (Header + Simulated Camera Window + Manual Input Box)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (continuousMode) 0.58f else 1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = scanTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // CameraX Preview Scanner Frame Container with breathing scanner line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.DarkGray.copy(alpha = 0.4f))
                        .border(BorderStroke(2.dp, themeColor.copy(alpha = 0.6f)), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    // Add tap-to-focus gesture
                                    setOnTouchListener { view, event ->
                                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                                            val currentCameraControl = cameraControl
                                            if (currentCameraControl != null) {
                                                try {
                                                    val factory = meteringPointFactory
                                                    val point = factory.createPoint(event.x, event.y)
                                                    val action = androidx.camera.core.FocusMeteringAction.Builder(
                                                        point,
                                                        androidx.camera.core.FocusMeteringAction.FLAG_AF or androidx.camera.core.FocusMeteringAction.FLAG_AE
                                                    ).build()
                                                    currentCameraControl.startFocusAndMetering(action)
                                                    view.performClick()
                                                } catch (e: Exception) {
                                                    Log.e("BarcodeScanner", "Failed to focus on touch", e)
                                                }
                                            }
                                        }
                                        true
                                    }
                                }.also { previewView ->
                                    com.example.util.CameraManager.bindScanner(
                                        context = ctx,
                                        lifecycleOwner = lifecycleOwner,
                                        previewView = previewView,
                                        onCameraConfigured = { camera ->
                                             cameraControl = camera.cameraControl
                                             try {
                                                 camera.cameraControl.setZoomRatio(currentZoom)
                                             } catch (e: Exception) {
                                                 Log.e("BarcodeScanner", "Failed to set initial zoom", e)
                                             }
                                             // Focus on the center once configured
                                            try {
                                                previewView.post {
                                                    val factory = previewView.meteringPointFactory
                                                    val centerPoint = factory.createPoint(
                                                        previewView.width / 2f,
                                                        previewView.height / 2f
                                                    )
                                                    val action = androidx.camera.core.FocusMeteringAction.Builder(
                                                        centerPoint,
                                                        androidx.camera.core.FocusMeteringAction.FLAG_AF or androidx.camera.core.FocusMeteringAction.FLAG_AE
                                                    ).setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                                     .build()
                                                    camera.cameraControl.startFocusAndMetering(action)
                                                }
                                            } catch (e: Exception) {
                                                Log.e("BarcodeScanner", "Failed to focus center", e)
                                            }
                                        },
                                        analyzer = { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                val image = InputImage.fromMediaImage(
                                                    mediaImage,
                                                    imageProxy.imageInfo.rotationDegrees
                                                )
                                                scanner.process(image)
                                                    .addOnSuccessListener { barcodes ->
                                                        for (barcode in barcodes) {
                                                            val rawValue = barcode.rawValue
                                                            if (!rawValue.isNullOrEmpty()) {
                                                                val currentTime = System.currentTimeMillis()
                                                                if (rawValue != lastScannedCode || currentTime - lastScanTime > 1500L) {
                                                                    lastScannedCode = rawValue
                                                                    lastScanTime = currentTime
                                                                    handleScannedBarcode(rawValue)
                                                                }
                                                                break
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("BarcodeScanner", "Barcode detection failed", e)
                                                    }
                                                    .addOnCompleteListener {
                                                        imageProxy.close()
                                                    }
                                            } else {
                                                imageProxy.close()
                                            }
                                        },
                                        cameraExecutor = cameraExecutor
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { /* Camera initialized and bound once in factory */ }
                        )
                    }

                    // Moving scan line simulation
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserYOffset by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laserPosition"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * laserYOffset
                        drawLine(
                            color = themeColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Inside Frame Text Indicator
                    if (!hasCameraPermission) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "📸",
                                fontSize = 32.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "CAMERA EN ATTENTE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = infoPermission,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Soft dark overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.15f))
                        )
                        // Floating zoom control overlay (Google Camera Style)
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1.0f, 2.0f, 3.0f).forEach { zoomVal ->
                                val isSelected = currentZoom == zoomVal
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isSelected) themeColor else Color.Transparent)
                                        .clickable {
                                            currentZoom = zoomVal
                                            try {
                                                cameraControl?.setZoomRatio(zoomVal)
                                            } catch (e: Exception) {
                                                Log.e("BarcodeScanner", "Failed to set zoom ratio to $zoomVal", e)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${zoomVal.toInt()}x",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Last Scanned Code Banner
                lastScannedCode?.let { code ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .testTag("last_scanned_code_banner"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Scanned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (language) {
                                        "mg" -> "Kaody voascan farany:"
                                        "fr" -> "Dernier code scanné :"
                                        else -> "Last scanned code:"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(
                                onClick = { lastScannedCode = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }


            }

            // Continuous Bottom Basket summary sheet
            if (continuousMode) {
                val cartState = viewModel?.cart?.collectAsState(initial = emptyList<com.example.ui.viewmodel.CartItem>())
                val cart = cartState?.value ?: remember { emptyList<com.example.ui.viewmodel.CartItem>() }

                val cartTotalState = viewModel?.cartTotal?.collectAsState(initial = 0.0)
                val cartTotal = cartTotalState?.value ?: 0.0

                val sessionProductsTotal = remember(cart, sessionScannedProductIds) {
                    cart.filter { item ->
                        item.productId != null && sessionScannedProductIds.contains(item.productId)
                    }.sumOf { it.price * it.quantity }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.55f)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Header row: Title & Running Total
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when (language) {
                                        "mg" -> "Entana scanné"
                                        "fr" -> "Produits scannés"
                                        else -> "Scanned Items"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val sessionCount = cart.count { it.productId != null && sessionScannedProductIds.contains(it.productId) }
                                Text(
                                    text = when (language) {
                                        "mg" -> "Nisafidy entana $sessionCount tamin'ity session ity"
                                        "fr" -> "$sessionCount produit(s) scanné(s) cette session"
                                        else -> "$sessionCount product(s) scanned this session"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = when (language) {
                                        "mg" -> "Total an'ity fivoriana ity"
                                        "fr" -> "Total de la session"
                                        else -> "Session Total"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = themeColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = "${com.example.util.FormatUtil.formatPrice(sessionProductsTotal)} Ar",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = themeColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // List of items in the cart
                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "📸",
                                        fontSize = 28.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Text(
                                        text = when (language) {
                                            "mg" -> "Tsy misy entana scanné mbola hita"
                                            "fr" -> "Aucun produit scanné"
                                            else -> "No scanned products yet"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = when (language) {
                                            "mg" -> "Asio kaody bar eo anoloan'ny fakantsary"
                                            "fr" -> "Saisissez un code-barres ci-dessus pour simuler"
                                            else -> "Enter a barcode above to simulate"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        } else {
                            // Scrollable List of Cart items
                            androidx.compose.foundation.lazy.LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    count = cart.size,
                                    key = { index -> cart[index].id }
                                ) { index ->
                                    val item = cart[index]
                                    val isScannedInSession = item.productId != null && sessionScannedProductIds.contains(item.productId)
                                    val backgroundColor = if (isScannedInSession) {
                                        themeColor.copy(alpha = 0.08f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                    val itemModifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgroundColor)
                                        .let {
                                            if (isScannedInSession) {
                                                it.border(BorderStroke(1.dp, themeColor.copy(alpha = 0.35f)), RoundedCornerShape(12.dp))
                                            } else {
                                                it
                                            }
                                        }
                                        .padding(8.dp)

                                    Row(
                                        modifier = itemModifier,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            if (isScannedInSession) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = themeColor,
                                                        modifier = Modifier.padding(vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = if (language == "fr") "Scanné" else if (language == "mg") "Scanné" else "Scanned",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${com.example.util.FormatUtil.formatPrice(item.price)} Ar / ${item.unit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Quantity Selector controls inside bottom panel
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Minus Button
                                            IconButton(
                                                onClick = {
                                                    viewModel?.changeCartQuantityByDelta(item.id, -1.0)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }

                                            Text(
                                                text = com.example.util.FormatUtil.formatQty(item.quantity, "").trim(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )

                                            // Plus Button
                                            IconButton(
                                                onClick = {
                                                    viewModel?.changeCartQuantityByDelta(item.id, 1.0)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Edit Button
                                            IconButton(
                                                onClick = {
                                                    editingCartItem = item
                                                    editingQuantityStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit",
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Trash/Delete Button
                                            IconButton(
                                                onClick = {
                                                    viewModel?.removeFromCart(item.id)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Text("🗑️", fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 8.dp))

                        // Bottom Row: Action Close/Validate Button
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Text(
                                text = when (language) {
                                    "mg" -> "Hameno Vantana (${com.example.util.FormatUtil.formatPrice(cartTotal)} Ar)"
                                    "fr" -> "Valider (${com.example.util.FormatUtil.formatPrice(cartTotal)} Ar)"
                                    else -> "Confirm (${com.example.util.FormatUtil.formatPrice(cartTotal)} Ar)"
                                },
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingCartItem != null) {
        val item = editingCartItem!!
        val titleText = when (language) {
            "mg" -> "Hanova isan'ny entana"
            "fr" -> "Modifier la quantité"
            else -> "Modify quantity"
        }
        val labelText = when (language) {
            "mg" -> "Isany (farany ambony: ${com.example.util.FormatUtil.formatQty(item.maxStock, item.unit)})"
            "fr" -> "Quantité (max: ${com.example.util.FormatUtil.formatQty(item.maxStock, item.unit)})"
            else -> "Quantity (max: ${com.example.util.FormatUtil.formatQty(item.maxStock, item.unit)})"
        }
        val cancelText = when (language) {
            "mg" -> "Hanafoana"
            "fr" -> "Annuler"
            else -> "Cancel"
        }
        val confirmText = when (language) {
            "mg" -> "Hamarina"
            "fr" -> "Valider"
            else -> "Confirm"
        }
        AlertDialog(
            onDismissRequest = { editingCartItem = null },
            title = { Text(titleText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    OutlinedTextField(
                        value = editingQuantityStr,
                        onValueChange = { editingQuantityStr = it },
                        label = { Text(labelText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = editingQuantityStr.replace(',', '.').toDoubleOrNull()
                        if (newQty != null && newQty > 0) {
                            if (newQty > item.maxStock) {
                                val warnText = when (language) {
                                    "mg" -> "Tahiry ambony indrindra: ${com.example.util.FormatUtil.formatQty(item.maxStock, item.unit)}"
                                    "fr" -> "Stock maximum dépassé : ${com.example.util.FormatUtil.formatQty(item.maxStock, item.unit)}"
                                    else -> "Maximum stock exceeded: ${com.example.util.FormatUtil.formatQty(item.maxStock, item.unit)}"
                                }
                                android.widget.Toast.makeText(context, warnText, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            viewModel?.updateCartQuantity(item.id, newQty)
                            editingCartItem = null
                        } else {
                            val errorText = when (language) {
                                "mg" -> "Isana tsy mety"
                                "fr" -> "Quantité invalide"
                                else -> "Invalid quantity"
                            }
                            android.widget.Toast.makeText(context, errorText, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text(confirmText, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCartItem = null }) {
                    Text(cancelText)
                }
            }
        )
    }
}
