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
                val preview = Preview.Builder()
                    .setResolutionSelector(resolutionSelector)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Create the ImageAnalysis use case with STRATEGY_KEEP_ONLY_LATEST for smooth frame rates
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(resolutionSelector)
                    .build()

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
