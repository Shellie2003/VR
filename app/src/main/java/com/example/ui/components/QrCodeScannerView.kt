@file:kotlin.OptIn(
    com.google.accompanist.permissions.ExperimentalPermissionsApi::class,
    androidx.camera.core.ExperimentalGetImage::class
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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

// Sound and haptic feedback on successful connection scan
private fun triggerQrSuccessFeedback(context: Context) {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                toneGen.release()
            } catch (e: Exception) {
                // Ignore silent release exceptions
            }
        }, 300)
    } catch (e: Exception) {
        Log.e("QrCodeScanner", "Failed to play beep sound", e)
    }

    try {
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(100)
            }
        }
    } catch (e: Exception) {
        Log.e("QrCodeScanner", "Failed to perform haptic feedback", e)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrCodeScannerView(
    onQrCodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    language: String = "fr",
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var manualIpValue by remember { mutableStateOf("") }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val hasCameraPermission = cameraPermissionState.status.isGranted

    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var currentZoom by remember { mutableStateOf(1.0f) }

    // Strictly scan ONLY QR CODES to avoid any conflicts with product barcodes
    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
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

    // Request camera permission dynamically
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Localized texts
    val scanTitle = when (language) {
        "mg" -> "Hizaha ny QR Code"
        "fr" -> "Scanner le code QR de connexion"
        else -> "Scan Connection QR Code"
    }

    val instructions = when (language) {
        "mg" -> "Atomboy eo amin'ny efamira ny QR Code hita amin'ny findain'ny gérant."
        "fr" -> "Placez le code QR affiché sur le téléphone du gérant dans le cadre ci-dessus."
        else -> "Place the QR code shown on the manager's device in the box above."
    }

    val manualLabel = when (language) {
        "mg" -> "Sorao handrindra ny IP (Raha tsy mandeha ny scanner)"
        "fr" -> "Saisir l'adresse IP manuellement (Secours)"
        else -> "Enter IP address manually (Backup)"
    }

    val placeholderIp = "192.168.1."

    val btnConnectText = when (language) {
        "mg" -> "Hampifandray mivantana"
        "fr" -> "Se connecter manuellement"
        else -> "Connect manually"
    }

    val infoPermission = when (language) {
        "mg" -> "Mila fahazoan-dalana hampiasa fakantsary ny scanner."
        "fr" -> "Le scanner requiert l'accès à l'appareil photo."
        else -> "The scanner requires camera access."
    }

    fun handleScannedQr(code: String) {
        val trimmed = code.trim()
        if (trimmed.isNotEmpty()) {
            lastScannedCode = trimmed
            triggerQrSuccessFeedback(context)
            onQrCodeScanned(trimmed)
            onClose()
        }
    }

    fun handleManualConnection() {
        val trimmed = manualIpValue.trim()
        if (trimmed.isNotEmpty()) {
            handleScannedQr(trimmed)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF00110B) // Ultra-dark green/black surface matching App theme
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Header Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = scanTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }

                // Beautiful, centralized QR Square Scanner Box
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.DarkGray.copy(alpha = 0.3f))
                        .border(BorderStroke(2.dp, themeColor.copy(alpha = 0.8f)), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
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
                                                    Log.e("QrCodeScanner", "Focus failed on tap", e)
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
                                                Log.e("QrCodeScanner", "Failed to set initial zoom", e)
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
                                                Log.e("QrCodeScanner", "Failed to focus center", e)
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
                                                                    handleScannedQr(rawValue)
                                                                }
                                                                break
                                                            }
                                                        }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Log.e("QrCodeScanner", "QR Code detection failed", e)
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
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Sliding vertical glowing bar (QR style scanning)
                    val infiniteTransition = rememberInfiniteTransition(label = "laser")
                    val laserYOffset by infiniteTransition.animateFloat(
                        initialValue = 0.1f,
                        targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laserPosition"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * laserYOffset
                        drawLine(
                            color = themeColor,
                            start = androidx.compose.ui.geometry.Offset(size.width * 0.1f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width * 0.9f, y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Transparent square overlay indicator inside
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                    )

                    if (!hasCameraPermission) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📸",
                                fontSize = 36.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = infoPermission,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Floating zoom controls specifically for the QR Code Scanner
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
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
                                                Log.e("QrCodeScanner", "Failed to set zoom ratio to $zoomVal", e)
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

                // Instructions under the preview
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // manual fallback UI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = manualLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.LightGray
                        )

                        OutlinedTextField(
                            value = manualIpValue,
                            onValueChange = { manualIpValue = it },
                            placeholder = { Text(text = placeholderIp, color = Color.Gray) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = themeColor
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = themeColor,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                focusedLabelColor = themeColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { handleManualConnection() },
                            enabled = manualIpValue.trim().isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor,
                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = btnConnectText,
                                fontWeight = FontWeight.Bold,
                                color = if (manualIpValue.trim().isNotEmpty()) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
