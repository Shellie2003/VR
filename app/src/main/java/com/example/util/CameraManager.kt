package com.example.util

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.concurrent.ExecutorService
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import android.hardware.camera2.CaptureRequest

object CameraManager {
    private const val TAG = "CameraManager"

    /**
     * Binds the camera preview and analyzer use cases to the ProcessLifecycleOwner.
     * Configures the ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST.
     */
    fun bindScanner(
        context: Context,
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        cameraExecutor: java.util.concurrent.ExecutorService,
        onCameraConfigured: (Camera) -> Unit = {}
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                
                // Define resolution options consistent with production requirements
                val resolutionSelector = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(
                        androidx.camera.core.resolutionselector.AspectRatioStrategy(
                            AspectRatio.RATIO_16_9,
                            androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
                        )
                    )
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()

                // Create the Preview use case
                val previewBuilder = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)

                // Enable continuous auto-focus using Camera2Interop
                try {
                    val extender = Camera2Interop.Extender(previewBuilder)
                    extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_USE_SCENE_MODE
                    )
                    extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CaptureRequest.CONTROL_SCENE_MODE_BARCODE
                    )
                    Log.d(TAG, "Configured CONTROL_AF_MODE_CONTINUOUS_PICTURE and CONTROL_SCENE_MODE_BARCODE on Preview via Camera2Interop.")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply Camera2Interop autofocus & scene mode option on Preview", e)
                }

                val preview = previewBuilder.build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                // Create the ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST for smooth frame rates
                val imageAnalysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)

                // Force standard format YUV_420_888 for ML Kit, with try-catch fallback
                try {
                    imageAnalysisBuilder.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    Log.d(TAG, "Configured OUTPUT_IMAGE_FORMAT_YUV_420_888 on ImageAnalysis.")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set ImageAnalysis output format to YUV_420_888, falling back to default.", e)
                }

                // Enable continuous auto-focus on ImageAnalysis capture requests as well
                try {
                    val extender = Camera2Interop.Extender(imageAnalysisBuilder)
                    extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_MODE,
                        CaptureRequest.CONTROL_MODE_USE_SCENE_MODE
                    )
                    extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CaptureRequest.CONTROL_SCENE_MODE_BARCODE
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply Camera2Interop autofocus & scene mode option on ImageAnalysis", e)
                }

                val imageAnalysis = imageAnalysisBuilder.build()
                imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                // Unbind previous use cases
                cameraProvider.unbindAll()

                Log.d(TAG, "[LIFECYCLE] Binding Preview and ImageAnalysis to the specified LifecycleOwner.")
                
                // Attach use cases to the passed lifecycleOwner
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )

                onCameraConfigured(camera)
            } catch (e: Exception) {
                Log.e(TAG, "Error binding camera use cases to ProcessLifecycleOwner", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
