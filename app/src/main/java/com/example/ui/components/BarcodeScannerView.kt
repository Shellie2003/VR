package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.camera.core.FocusMeteringAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

// Helper to trigger audio and haptic feedback on successful scan
private fun triggerSuccessFeedback(context: Context) {
    // 1. Play success beep sound
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        // Release ToneGenerator after beep completes to avoid native memory/audio track leaks
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

    // 2. Perform subtle vibration feedback
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

enum class ScanMode {
    BARCODE,
    OCR
}

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
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val permissionTitleText = when (language) {
        "mg" -> "Mila Fahazoan-dalana hampiasa Fakantsary"
        "fr" -> "Autorisation de l'appareil photo requise"
        else -> "Camera Permission Required"
    }

    val requestPermissionText = when (language) {
        "mg" -> "Ny fampiasana ny fakantsary dia ilaina mba ahafahana mitsapa sy mamantatra ireo kaody bar.\n\nRaha efa nolavinao izany, azonao atao ny manokatra ny fandrindrana mba hamelomana azy."
        "fr" -> "L'accès à l'appareil photo est requis pour scanner les codes-barres.\n\nSi vous l'avez refusé précédemment, vous pouvez l'activer manuellement dans les paramètres de l'application."
        else -> "Camera access is required to scan barcodes.\n\nIf you have previously denied this permission, you can enable it manually in the application settings."
    }

    val requestButtonText = when (language) {
        "mg" -> "Omeo fahazoan-dalana"
        "fr" -> "Autoriser l'appareil photo"
        else -> "Grant Permission"
    }

    val settingsButtonText = when (language) {
        "mg" -> "Sokafy ny Fandrindrana"
        "fr" -> "Ouvrir les Paramètres"
        else -> "Open Settings"
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Accent indicator icon
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(themeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📸",
                            fontSize = 32.sp
                        )
                    }

                    Text(
                        text = permissionTitleText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = requestPermissionText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = requestButtonText, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("BarcodeScanner", "Failed to open settings", e)
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColor),
                            border = BorderStroke(1.dp, themeColor),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = settingsButtonText, fontWeight = FontWeight.SemiBold)
                        }

                        TextButton(
                            onClick = onClose,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = if (language == "mg") "Hanakatona" else if (language == "fr") "Fermer" else "Close",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    } else {
        CameraScannerLayout(
            onBarcodeScanned = onBarcodeScanned,
            onClose = onClose,
            language = language,
            themeColor = themeColor,
            immediateMode = immediateMode,
            continuousMode = continuousMode,
            viewModel = viewModel,
            barcodeFormats = barcodeFormats
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScannerLayout(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    language: String,
    themeColor: Color,
    immediateMode: Boolean,
    barcodeFormats: IntArray,
    continuousMode: Boolean = false,
    viewModel: com.example.ui.viewmodel.InventoryViewModel? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Executors and analyzer should be managed in the Composable lifecycle scope
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var detectedCode by remember { mutableStateOf<String?>(null) }
    var scanMode by remember { mutableStateOf(ScanMode.BARCODE) }
    var sessionScannedProductIds by remember { mutableStateOf(emptySet<Int>()) }

    // Diagnostic/Debug Panel state variables
    var totalFramesScanned by remember { mutableStateOf(0) }
    var lastProcessedTime by remember { mutableStateOf(0L) }
    var lastScannerMessage by remember { mutableStateOf("Initialisation de l'analyseur...") }
    var lastScannerError by remember { mutableStateOf<String?>(null) }
    var showDebugPanel by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var inlineOverlayMessage by remember { mutableStateOf<String?>(null) }
    var inlineOverlayIsSuccess by remember { mutableStateOf(true) }

    val barcodeAnalyzer = remember(barcodeFormats) {
        BarcodeAnalyzer(
            barcodeFormats = barcodeFormats,
            onFrameProcessed = { frameCount, duration, status, error ->
                (context as? android.app.Activity)?.runOnUiThread {
                    totalFramesScanned = frameCount
                    lastProcessedTime = duration
                    lastScannerMessage = status
                    lastScannerError = error
                }
            },
            onBarcodeDetected = { analyzerInstance, barcodeValue ->
                Log.d("BarcodeScannerView", "BarcodeAnalyzer callback triggered with value='$barcodeValue', immediateMode=$immediateMode")
                // Safely report back on the UI/Main Thread
                (context as? android.app.Activity)?.runOnUiThread {
                    triggerSuccessFeedback(context)
                    if (continuousMode) {
                        coroutineScope.launch {
                            val trimmed = barcodeValue.trim()
                            if (viewModel != null) {
                                val match = viewModel.getProductByBarcode(trimmed)
                                if (match != null) {
                                    viewModel.addToCart(match, 1.0)
                                    inlineOverlayMessage = "Ajouté : ${match.name}\n${com.example.util.FormatUtil.formatPrice(match.price)} Ar"
                                    inlineOverlayIsSuccess = true
                                    sessionScannedProductIds = sessionScannedProductIds + match.id
                                } else {
                                    inlineOverlayMessage = "Inconnu : $trimmed"
                                    inlineOverlayIsSuccess = false
                                }
                            } else {
                                onBarcodeScanned(trimmed)
                                inlineOverlayMessage = "Scanné : $trimmed"
                                inlineOverlayIsSuccess = true
                            }
                            delay(1800)
                            inlineOverlayMessage = null
                            analyzerInstance.isScanning = true
                        }
                    } else if (immediateMode) {
                        Log.d("BarcodeScannerView", "immediateMode is true, invoking onBarcodeScanned(value)")
                        onBarcodeScanned(barcodeValue)
                    } else {
                        val successToastMsg = when (language) {
                            "mg" -> "Kaody hita: $barcodeValue"
                            "fr" -> "Code détecté : $barcodeValue"
                            else -> "Detected code: $barcodeValue"
                        }
                        Log.d("BarcodeScannerView", "immediateMode is false, showing preview dialog with scanned value: '$barcodeValue'")
                        android.widget.Toast.makeText(context, successToastMsg, android.widget.Toast.LENGTH_SHORT).show()
                        detectedCode = barcodeValue
                    }
                }
            }
        )
    }

    val textAnalyzer = remember {
        TextAnalyzer(
            onFrameProcessed = { frameCount, duration, status, error ->
                (context as? android.app.Activity)?.runOnUiThread {
                    totalFramesScanned = frameCount
                    lastProcessedTime = duration
                    lastScannerMessage = status
                    lastScannerError = error
                }
            },
            onTextDetected = { analyzerInstance, textValue ->
                Log.d("BarcodeScannerView", "TextAnalyzer callback triggered with value='$textValue', immediateMode=$immediateMode")
                // Safely report back on the UI/Main Thread
                (context as? android.app.Activity)?.runOnUiThread {
                    triggerSuccessFeedback(context)
                    if (continuousMode) {
                        coroutineScope.launch {
                            val trimmed = textValue.trim()
                            if (viewModel != null) {
                                val match = viewModel.getProductByBarcode(trimmed)
                                if (match != null) {
                                    viewModel.addToCart(match, 1.0)
                                    inlineOverlayMessage = "Ajouté : ${match.name}\n${com.example.util.FormatUtil.formatPrice(match.price)} Ar"
                                    inlineOverlayIsSuccess = true
                                    sessionScannedProductIds = sessionScannedProductIds + match.id
                                } else {
                                    inlineOverlayMessage = "Inconnu : $trimmed"
                                    inlineOverlayIsSuccess = false
                                }
                            } else {
                                onBarcodeScanned(trimmed)
                                inlineOverlayMessage = "Scanné : $trimmed"
                                inlineOverlayIsSuccess = true
                            }
                            delay(1800)
                            inlineOverlayMessage = null
                            analyzerInstance.isScanning = true
                        }
                    } else if (immediateMode) {
                        Log.d("BarcodeScannerView", "immediateMode is true, invoking onBarcodeScanned(value)")
                        onBarcodeScanned(textValue)
                    } else {
                        val successToastMsg = when (language) {
                            "mg" -> "Isa hita: $textValue"
                            "fr" -> "Chiffres détectés : $textValue"
                            else -> "Detected digits: $textValue"
                        }
                        Log.d("BarcodeScannerView", "immediateMode is false, showing preview dialog with scanned value: '$textValue'")
                        android.widget.Toast.makeText(context, successToastMsg, android.widget.Toast.LENGTH_SHORT).show()
                        detectedCode = textValue
                    }
                }
            }
        )
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val scannerHint = when (language) {
        "mg" -> "Asio ny kaody eo anelanelan'ny tsipika maitso"
        "fr" -> "Placez le code-barres dans le cadre"
        else -> "Place the barcode inside the frame"
    }

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Laser Animation Y position
    var laserYPercent by remember { mutableStateOf(0.1f) }
    var movingDown by remember { mutableStateOf(true) }
    LaunchedEffect(movingDown) {
        while (true) {
            kotlinx.coroutines.delay(30)
            if (movingDown) {
                laserYPercent += 0.04f
                if (laserYPercent >= 0.9f) {
                    movingDown = false
                }
            } else {
                laserYPercent -= 0.04f
                if (laserYPercent <= 0.1f) {
                    movingDown = true
                }
            }
        }
    }

    // Retrieve ProcessCameraProvider once
    LaunchedEffect(Unit) {
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
            } catch (e: Exception) {
                Log.e("CameraScannerLayout", "Failed to retrieve ProcessCameraProvider", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Re-bind use cases whenever provider, lens, or scan mode changes
    LaunchedEffect(cameraProvider, lensFacing, scanMode) {
        val provider = cameraProvider ?: return@LaunchedEffect
        try {
            // Reset scanner active states
            barcodeAnalyzer.isScanning = (scanMode == ScanMode.BARCODE)
            textAnalyzer.isScanning = (scanMode == ScanMode.OCR)

            com.example.util.CameraManager.bindScanner(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                analyzer = if (scanMode == ScanMode.BARCODE) barcodeAnalyzer else textAnalyzer,
                lensFacing = lensFacing,
                cameraExecutor = cameraExecutor,
                onCameraConfigured = { configuredCamera ->
                    camera = configuredCamera
                    // Re-apply Flash state
                    configuredCamera.cameraControl.enableTorch(isFlashOn)
                }
            )
        } catch (e: Exception) {
            Log.e("CameraScannerLayout", "Failed to bind CameraX use cases via CameraManager", e)
        }
    }

    // 100% leak-proof cleanup of camera binding, executor service, and ML Kit native scanners
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("CameraScannerLayout", "Error unbinding camera during disposal", e)
            }
            barcodeAnalyzer.close()
            textAnalyzer.close()
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render standard preview view with Tap to Focus support
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val factory = previewView.meteringPointFactory
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                            .build()
                        camera?.cameraControl?.startFocusAndMetering(action)
                    }
                },
            factory = { previewView }
        )

        // Semi-transparent Overlay Mask around viewfinder
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Viewfinder Window
        Box(
            modifier = Modifier
                .size(280.dp, 280.dp)
                .align(if (continuousMode) androidx.compose.ui.BiasAlignment(0f, -0.35f) else Alignment.Center)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Transparent)
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
        ) {
            // Corner borders guides
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cornerSize = 32.dp.toPx()
                val strokeW = 5.dp.toPx()

                // Top Left
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))

                // Top Right
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - cornerSize, 0f), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - strokeW, 0f), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))

                // Bottom Left
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, h - strokeW), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(0f, h - cornerSize), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))

                // Bottom Right
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - cornerSize, h - strokeW), size = androidx.compose.ui.geometry.Size(cornerSize, strokeW))
                drawRect(color = themeColor, topLeft = androidx.compose.ui.geometry.Offset(w - strokeW, h - cornerSize), size = androidx.compose.ui.geometry.Size(strokeW, cornerSize))
            }

            // Animated Laser Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (280 * laserYPercent).dp)
                    .background(Color(0xFF22C55E))
            )
        }

        // Overlay Instructions
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (continuousMode) 16.dp else 40.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = when (language) {
                    "mg" -> "Mpanasivana Code"
                    "fr" -> "Scanner de code-barres"
                    else -> "Barcode Scanner"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Beautiful modern tab switch to choose between Barcode and Numbers (OCR)
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    ScanMode.BARCODE to when (language) {
                        "mg" -> "Code-barres"
                        "fr" -> "Code-barres"
                        else -> "Barcode"
                    },
                    ScanMode.OCR to when (language) {
                        "mg" -> "Fisavana Isa / Sary"
                        "fr" -> "Numéros / Texte"
                        else -> "Numbers / Text"
                    }
                )
                modes.forEach { (mode, title) ->
                    val isSelected = scanMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) themeColor else Color.Transparent)
                            .clickable {
                                scanMode = mode
                                detectedCode = null
                                barcodeAnalyzer.isScanning = (mode == ScanMode.BARCODE)
                                textAnalyzer.isScanning = (mode == ScanMode.OCR)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Text(
                text = if (scanMode == ScanMode.BARCODE) scannerHint else when (language) {
                    "mg" -> "Asio ny isa eo anelanelan'ny tsipika maitso"
                    "fr" -> "Placez les chiffres imprimés dans le cadre"
                    else -> "Place the printed numbers inside the frame"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Interactive diagnostics toggle button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showDebugPanel = !showDebugPanel }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (language == "fr") "⚙️ Outils Diagnostic" else if (language == "mg") "⚙️ Fitaovana Diagnostic" else "⚙️ Diagnostics Tools",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Inline scanning status overlay
        if (inlineOverlayMessage != null) {
            Box(
                modifier = Modifier
                    .align(if (continuousMode) androidx.compose.ui.BiasAlignment(0f, -0.75f) else Alignment.TopCenter)
                    .padding(top = if (continuousMode) 0.dp else 140.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (inlineOverlayIsSuccess) Color(0xFF15803D).copy(alpha = 0.92f) else Color(0xFFB91C1C).copy(alpha = 0.92f))
                    .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (inlineOverlayIsSuccess) "✅" else "❌",
                        fontSize = 18.sp
                    )
                    Text(
                        text = inlineOverlayMessage!!,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Non-continuous bottom controls
        if (!continuousMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flashlight button
                IconButton(
                    onClick = {
                        val nextFlash = !isFlashOn
                        isFlashOn = nextFlash
                        camera?.cameraControl?.enableTorch(nextFlash)
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = if (isFlashOn) Color.Yellow else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(50))
                        .background(themeColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Scanner",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Toggle Front/Back camera button
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Continuous bottom panel (Basket summary sheet)
        if (continuousMode) {
            val cart by viewModel?.cart?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
            val cartTotal by viewModel?.cartTotal?.collectAsState(initial = 0.0) ?: remember { mutableStateOf(0.0) }
            val sessionProductsTotal = remember(cart, sessionScannedProductIds) {
                cart.filter { item ->
                    item.productId != null && sessionScannedProductIds.contains(item.productId)
                }.sumOf { it.price * it.quantity }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.42f)
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
                                        "fr" -> "Présentez un code-barres devant l'appareil"
                                        else -> "Show a barcode to the camera"
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

                    // Bottom Row: Action Close/Validate Button and Compact Camera Settings (Flash, Lens)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Flash toggle
                        IconButton(
                            onClick = {
                                val nextFlash = !isFlashOn
                                isFlashOn = nextFlash
                                camera?.cameraControl?.enableTorch(nextFlash)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Toggle Flash",
                                tint = if (isFlashOn) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Lens flip
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Flip Camera",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Primary Action Close Scanner / Checkout
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
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
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Success Dialog Overlay (non-continuous only)
        if (!continuousMode && detectedCode != null) {
            val code = detectedCode!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {
                    detectedCode = null
                    barcodeAnalyzer.isScanning = (scanMode == ScanMode.BARCODE)
                    textAnalyzer.isScanning = (scanMode == ScanMode.OCR)
                }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✅", fontSize = 28.sp)
                        }

                        Text(
                            text = when (language) {
                                "mg" -> "Tafita ny Fisavana"
                                "fr" -> "Numérisation réussie"
                                else -> "Scan Successful"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = when (language) {
                                "mg" -> "Hita soa aman-tsara ny kaody:"
                                "fr" -> "Le code a été détecté avec succès :"
                                else -> "The code was successfully detected:"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = themeColor,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    detectedCode = null
                                    barcodeAnalyzer.isScanning = (scanMode == ScanMode.BARCODE)
                                    textAnalyzer.isScanning = (scanMode == ScanMode.OCR)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = when (language) {
                                        "mg" -> "Averina"
                                        "fr" -> "Réessayer"
                                        else -> "Retry"
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    onBarcodeScanned(code)
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                            ) {
                                Text(
                                    text = when (language) {
                                        "mg" -> "Hamafiso"
                                        "fr" -> "Confirmer"
                                        else -> "Confirm"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diagnostic / Debug overlay panel
        if (showDebugPanel) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showDebugPanel = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("🛠️", fontSize = 20.sp)
                            Text(
                                text = "Diagnostics Caméra & ML Kit",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Yellow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Mode Actif :", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(if (scanMode == ScanMode.BARCODE) "ML Kit Scanner de code-barres" else "ML Kit Reconnaissance de texte (OCR)", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Trames reçues par l'analyseur :", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("$totalFramesScanned", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Temps d'exécution (Dernière trame) :", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${lastProcessedTime} ms", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Statut de l'Analyseur :", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(lastScannerMessage, style = MaterialTheme.typography.bodySmall, color = Color(0xFF22C55E), fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }

                        if (lastScannerError != null) {
                            Text(
                                text = "Erreur Capturée :\n$lastScannerError",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            )
                        } else {
                            Text(
                                text = "Le flux vidéo et le moteur ML Kit fonctionnent correctement. Si le scan ne répond pas, assurez-vous de bien centrer le code.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Reset counters
                                    totalFramesScanned = 0
                                    lastProcessedTime = 0
                                    lastScannerMessage = "Réinitialisé"
                                    lastScannerError = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Réinitialiser", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }

                            Button(
                                onClick = { showDebugPanel = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Fermer", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private class BarcodeAnalyzer(
    private val barcodeFormats: IntArray,
    private val onFrameProcessed: (frameCount: Int, durationMs: Long, status: String, error: String?) -> Unit,
    private val onBarcodeDetected: (BarcodeAnalyzer, String) -> Unit
) : ImageAnalysis.Analyzer {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_QR_CODE
        )
        .build()
    private val scanner = BarcodeScanning.getClient(options)
    var isScanning = true
    private var localFrameCount = 0

    override fun analyze(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()
        localFrameCount++
        val timestamp = imageProxy.imageInfo.timestamp
        Log.d("BarcodeScan", "[LIFECYCLE] New frame received. Timestamp: $timestamp. STRATEGY_KEEP_ONLY_LATEST is active.")
        
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null && isScanning) {
                val rotation = imageProxy.imageInfo.rotationDegrees
                val width = imageProxy.width
                val height = imageProxy.height
                Log.d("BarcodeScan", "[LIFECYCLE] Frame analysis started. Size: ${width}x${height}, Rotation: $rotation deg. Scanning active: $isScanning.")

                val image = InputImage.fromMediaImage(mediaImage, rotation)
                Log.d("BarcodeScan", "[LIFECYCLE] Passing InputImage to ML Kit BarcodeScanner.")
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val duration = System.currentTimeMillis() - startTime
                        Log.d("BarcodeScan", "[LIFECYCLE] ML Kit process SUCCESS. Detected barcodes count: ${barcodes.size}")
                        for ((index, barcode) in barcodes.withIndex()) {
                            Log.d("BarcodeScan", "[DEBUG] Barcode[$index]: Format=${barcode.format}, rawValue='${barcode.rawValue}', valueType=${barcode.valueType}")
                        }
                        
                        val barcode = barcodes.firstOrNull()
                        val value = barcode?.rawValue ?: barcode?.displayValue
                        if (value != null && isScanning) {
                            Log.d("BarcodeScan", "[LIFECYCLE] Valid barcode detected: '$value'. Executing callback.")
                            isScanning = false
                            onFrameProcessed(localFrameCount, duration, "Code détecté: $value", null)
                            onBarcodeDetected(this@BarcodeAnalyzer, value)
                        } else if (value == null && barcodes.isNotEmpty()) {
                            Log.w("BarcodeScan", "[WARNING] Barcodes found but no valid value could be extracted.")
                            onFrameProcessed(localFrameCount, duration, "Code trouvé mais vide", null)
                        } else {
                            onFrameProcessed(localFrameCount, duration, "Aucun code détecté", null)
                        }
                        
                        Log.d("BarcodeScan", "[LIFECYCLE] Closing ImageProxy on success branch. Timestamp: $timestamp")
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        val duration = System.currentTimeMillis() - startTime
                        Log.e("BarcodeScan", "[LIFECYCLE] ML Kit process FAILURE for frame timestamp: $timestamp", e)
                        Log.e("BarcodeScan", "Details: message='${e.message}', cause='${e.cause}'")
                        onFrameProcessed(localFrameCount, duration, "Erreur ML Kit Barcode", e.localizedMessage ?: e.toString())
                        Log.d("BarcodeScan", "[LIFECYCLE] Closing ImageProxy on failure branch. Timestamp: $timestamp")
                        imageProxy.close()
                    }
            } else {
                val duration = System.currentTimeMillis() - startTime
                if (mediaImage == null) {
                    Log.w("BarcodeScan", "[LIFECYCLE] Frame dropped: mediaImage is null. Timestamp: $timestamp")
                    onFrameProcessed(localFrameCount, duration, "Trame ignorée (mediaImage est null)", "La caméra a renvoyé un buffer d'image vide (null).")
                } else if (!isScanning) {
                    Log.v("BarcodeScan", "[LIFECYCLE] Frame dropped: isScanning is false (paused). Timestamp: $timestamp")
                    onFrameProcessed(localFrameCount, duration, "Scanner en pause", null)
                }
                Log.d("BarcodeScan", "[LIFECYCLE] Closing ImageProxy on drop branch. Timestamp: $timestamp")
                imageProxy.close()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("BarcodeScan", "[LIFECYCLE] Exception caught during analyze() for frame timestamp: $timestamp", e)
            onFrameProcessed(localFrameCount, duration, "Exception dans l'analyseur", e.localizedMessage ?: e.toString())
            Log.d("BarcodeScan", "[LIFECYCLE] Closing ImageProxy on exception branch. Timestamp: $timestamp")
            imageProxy.close()
        }
    }

    fun close() {
        Log.d("BarcodeAnalyzer", "Closing BarcodeScanner client")
        try {
            scanner.close()
        } catch (e: Exception) {
            Log.e("BarcodeAnalyzer", "Failed to close ML Kit scanner", e)
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private class TextAnalyzer(
    private val onFrameProcessed: (frameCount: Int, durationMs: Long, status: String, error: String?) -> Unit,
    private val onTextDetected: (TextAnalyzer, String) -> Unit
) : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    var isScanning = true
    private var localFrameCount = 0

    override fun analyze(imageProxy: ImageProxy) {
        val startTime = System.currentTimeMillis()
        localFrameCount++
        
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null && isScanning) {
                val rotation = imageProxy.imageInfo.rotationDegrees
                val width = imageProxy.width
                val height = imageProxy.height
                Log.d("TextAnalyzer", "Analyzing frame for text: size=${width}x${height}, rotation=$rotation, isScanning=$isScanning")

                val image = InputImage.fromMediaImage(mediaImage, rotation)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val duration = System.currentTimeMillis() - startTime
                        Log.d("TextAnalyzer", "ML Kit Text process success: detected block count=${visionText.textBlocks.size}, total character length=${visionText.text.length}")
                        if (visionText.text.isNotEmpty()) {
                            Log.d("TextAnalyzer", "Raw OCR text found:\n'${visionText.text}'")
                        }
                        if (isScanning) {
                            val detectedText = extractBarcodeDigits(visionText.text)
                            Log.d("TextAnalyzer", "Extracted digits from OCR text: '$detectedText'")
                            if (detectedText != null && isScanning) {
                                Log.d("TextAnalyzer", "Valid code found via OCR, invoking callback: value='$detectedText'")
                                isScanning = false
                                onFrameProcessed(localFrameCount, duration, "Code OCR détecté: $detectedText", null)
                                onTextDetected(this@TextAnalyzer, detectedText)
                            } else {
                                onFrameProcessed(localFrameCount, duration, if (visionText.text.isEmpty()) "Aucun texte détecté" else "Texte ignoré (aucun code numérique)", null)
                            }
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        val duration = System.currentTimeMillis() - startTime
                        Log.e("TextAnalyzer", "ML Kit Text recognition failed", e)
                        onFrameProcessed(localFrameCount, duration, "Erreur OCR ML Kit", e.localizedMessage ?: e.toString())
                        imageProxy.close()
                    }
            } else {
                val duration = System.currentTimeMillis() - startTime
                if (mediaImage == null) {
                    Log.w("TextAnalyzer", "Frame dropped: mediaImage is null")
                    onFrameProcessed(localFrameCount, duration, "Trame ignorée (mediaImage est null)", "La caméra a renvoyé un buffer d'image vide (null).")
                } else if (!isScanning) {
                    Log.v("TextAnalyzer", "Frame dropped: analyzer is currently paused or inactive")
                    onFrameProcessed(localFrameCount, duration, "OCR en pause", null)
                }
                imageProxy.close()
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e("TextAnalyzer", "Exception in OCR analyze()", e)
            onFrameProcessed(localFrameCount, duration, "Exception OCR", e.localizedMessage ?: e.toString())
            imageProxy.close()
        }
    }

    private fun extractBarcodeDigits(text: String): String? {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        Log.d("TextAnalyzer", "Parsing lines for digits: total non-empty lines=${lines.size}")
        
        // Find line consisting only of digits (length >= 4)
        for ((index, line) in lines.withIndex()) {
            val cleaned = line.replace("\\s|-|\\.".toRegex(), "")
            Log.d("TextAnalyzer", "Line[$index]: '$line' -> cleaned: '$cleaned'")
            if (cleaned.length >= 4 && cleaned.all { it.isDigit() }) {
                Log.d("TextAnalyzer", "Line[$index] matched digit criteria: '$cleaned'")
                return cleaned
            }
        }
        
        // Fallback to uppercase alphanumeric sequence (length >= 4)
        for ((index, line) in lines.withIndex()) {
            val cleaned = line.replace("\\s|-|\\.".toRegex(), "")
            if (cleaned.length >= 4 && cleaned.all { it.isLetterOrDigit() }) {
                Log.d("TextAnalyzer", "Line[$index] matched alphanumeric fallback: '$cleaned'")
                return cleaned
            }
        }
        
        Log.d("TextAnalyzer", "No matching codes extracted from OCR text")
        return null
    }

    fun close() {
        Log.d("TextAnalyzer", "Closing Text Recognition client")
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e("TextAnalyzer", "Failed to close Text Recognition client", e)
        }
    }
}
