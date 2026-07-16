package com.example.visionwellness.detection

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import com.example.visionwellness.optimization.AdaptiveFrameRateManager
import timber.log.Timber

/**
 * Enhanced frame processor with adaptive frame rate and memory optimization
 */
class OptimizedCameraFrameProcessor(
    private val eyeDetectionEngine: EyeDetectionEngine,
    private val adaptiveFrameRateManager: AdaptiveFrameRateManager
) {

    private val processingThread = HandlerThread("CameraFrameProcessing").apply { start() }
    private val processingHandler = Handler(processingThread.looper)
    private var frameCounter = 0
    private var currentFrameSkip = 0
    private var bitmapPool: Bitmap? = null

    init {
        updateFrameSkipRate()
    }

    /**
     * Process a camera frame with adaptive frame rate
     */
    fun processFrame(frame: Bitmap) {
        // Evaluate and adjust frame rate
        val targetFps = adaptiveFrameRateManager.evaluateAndAdjustFrameRate()

        if (targetFps <= 0) {
            // Pause processing if FPS is 0
            Timber.v("Processing paused due to system conditions")
            return
        }

        frameCounter++
        currentFrameSkip = adaptiveFrameRateManager.getFrameSkipForTargetFps()

        if (frameCounter > currentFrameSkip) {
            frameCounter = 0
            processingHandler.post {
                try {
                    eyeDetectionEngine.processFrame(frame)
                } catch (e: Exception) {
                    Timber.e(e, "Error in optimized frame processing")
                }
            }
        }
    }

    /**
     * Update frame skip rate based on target FPS
     */
    fun updateFrameSkipRate() {
        val newFrameSkip = adaptiveFrameRateManager.getFrameSkipForTargetFps()
        if (newFrameSkip != currentFrameSkip) {
            Timber.d("Frame skip updated: $currentFrameSkip -> $newFrameSkip")
            currentFrameSkip = newFrameSkip
        }
    }

    /**
     * Get current processing status
     */
    fun getStatusMessage(): String {
        return adaptiveFrameRateManager.getStatusMessage()
    }

    /**
     * Release resources
     */
    fun release() {
        processingThread.quitSafely()
        bitmapPool?.recycle()
        Timber.d("OptimizedCameraFrameProcessor released")
    }
}
