package com.example.visionwellness.detection

/**
 * Callback interface for blink and staring detection events
 */
interface BlinkDetectionListener {
    /**
     * Called when a blink is detected
     * @param timestamp The timestamp when the blink was detected (ms)
     */
    fun onBlinkDetected(timestamp: Long)

    /**
     * Called when the user has been staring (not blinking) for too long
     * @param durationMs The duration of staring in milliseconds
     */
    fun onStaringDetected(durationMs: Long)

    /**
     * Called when eyes are detected as closed
     * @param timestamp The timestamp when eyes were detected as closed
     */
    fun onEyesClosed(timestamp: Long)

    /**
     * Called when detection fails or encounters an error
     * @param error The error message
     */
    fun onDetectionError(error: String)
}
