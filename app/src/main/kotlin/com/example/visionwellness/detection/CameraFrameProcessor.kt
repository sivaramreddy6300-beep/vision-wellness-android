package com.example.visionwellness.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import timber.log.Timber

/**
 * Handles camera frame processing on a background thread
 * to avoid blocking the camera stream
 */
class CameraFrameProcessor(
    private val eyeDetectionEngine: EyeDetectionEngine
) {

    private val processingThread = HandlerThread("CameraFrameProcessing").apply { start() }
    private val processingHandler = Handler(processingThread.looper)
    private var frameCounter = 0
    private var targetFrameSkip = 0  // For 8fps at 30fps input: skip 3 frames (process 1)

    /**
     * Process a camera frame
     * Throttles processing to 8fps for battery efficiency
     * @param frame The camera frame as a Bitmap
     */
    fun processFrame(frame: Bitmap) {
        frameCounter++
        if (frameCounter > targetFrameSkip) {
            frameCounter = 0
            processingHandler.post {
                try {
                    eyeDetectionEngine.processFrame(frame)
                } catch (e: Exception) {
                    Timber.e(e, "Error in frame processing")
                }
            }
        }
    }

    /**
     * Set the frame skip rate for throttling
     * @param inputFps The input camera FPS
     * @param targetFps The target processing FPS (default 8fps)
     */
    fun setFrameSkipRate(inputFps: Int, targetFps: Int = 8) {
        targetFrameSkip = (inputFps / targetFps) - 1
        Timber.d("Frame skip rate set: $targetFrameSkip (processing at ~${inputFps / (targetFrameSkip + 1)} fps)")
    }

    /**
     * Release resources
     */
    fun release() {
        processingThread.quitSafely()
        Timber.d("CameraFrameProcessor released")
    }
}
