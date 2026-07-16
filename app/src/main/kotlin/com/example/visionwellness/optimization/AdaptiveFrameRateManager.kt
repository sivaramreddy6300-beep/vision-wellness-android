package com.example.visionwellness.optimization

import timber.log.Timber

/**
 * Adaptive frame rate manager that adjusts processing FPS based on:
 * - Battery level
 * - Device temperature
 * - Proximity sensor status
 *
 * Tries to maintain battery life while still providing reliable blink detection
 */
class AdaptiveFrameRateManager(
    private val batteryMonitor: BatteryMonitor,
    private val sensorMonitor: SensorMonitor
) {

    private var currentTargetFps: Int = 8  // Default 8 fps
    private var lastAdjustmentTime: Long = 0
    private val adjustmentIntervalMs = 10000  // Re-evaluate every 10 seconds

    companion object {
        // Frame rate configurations for different scenarios
        private const val FPS_CRITICAL = 4    // Battery critical: 4fps
        private const val FPS_LOW = 5         // Battery low: 5fps
        private const val FPS_NORMAL = 8      // Normal: 8fps
        private const val FPS_HIGH = 10       // Good battery: 10fps
        private const val FPS_FACE_AWAY = 3   // Face away from camera: 3fps
    }

    /**
     * Evaluate and adjust frame rate based on current conditions
     * @return Target FPS to process at
     */
    fun evaluateAndAdjustFrameRate(): Int {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdjustmentTime < adjustmentIntervalMs) {
            return currentTargetFps
        }

        lastAdjustmentTime = currentTime
        var newFps = FPS_NORMAL

        // Factor 1: Battery level (highest priority)
        val batteryStatus = batteryMonitor.getBatteryStatusLevel()
        newFps = when (batteryStatus) {
            BatteryMonitor.BatteryStatusLevel.CRITICAL -> FPS_CRITICAL
            BatteryMonitor.BatteryStatusLevel.LOW -> FPS_LOW
            BatteryMonitor.BatteryStatusLevel.NORMAL -> FPS_NORMAL
            BatteryMonitor.BatteryStatusLevel.HEALTHY -> FPS_HIGH
        }

        Timber.d("Battery status: $batteryStatus -> FPS: $newFps")

        // Factor 2: Device temperature (can override to lower)
        val tempStatus = sensorMonitor.getTemperatureStatus()
        if (sensorMonitor.isDeviceTooHot()) {
            newFps = 0  // Pause processing
            Timber.w("Device too hot! Pausing eye tracking")
        } else if (sensorMonitor.isDeviceHot()) {
            newFps = (newFps * 0.75).toInt().coerceAtLeast(3)  // Reduce by 25%
            Timber.d("Device hot: Reducing FPS to $newFps")
        }

        Timber.d("Temperature status: $tempStatus")

        // Factor 3: Proximity sensor (if face is away, reduce FPS)
        if (!sensorMonitor.isFaceNear()) {
            newFps = (newFps * 0.5).toInt().coerceAtLeast(FPS_FACE_AWAY)
            Timber.d("Face away from camera: Reducing FPS to $newFps")
        }

        if (newFps != currentTargetFps) {
            Timber.i("Frame rate adjusted: $currentTargetFps fps -> $newFps fps")
            currentTargetFps = newFps
        }

        return currentTargetFps
    }

    /**
     * Get current target FPS
     */
    fun getCurrentTargetFps(): Int = currentTargetFps

    /**
     * Convert FPS to frame skip count
     * @param inputFps Camera input FPS (usually 30)
     * @return Number of frames to skip
     */
    fun getFrameSkipForTargetFps(inputFps: Int = 30): Int {
        return if (currentTargetFps > 0) {
            (inputFps / currentTargetFps) - 1
        } else {
            inputFps  // Skip all frames if paused
        }
    }

    /**
     * Get a human-readable status message
     */
    fun getStatusMessage(): String {
        val batteryPercent = batteryMonitor.getBatteryPercentage()
        val temperature = sensorMonitor.getCurrentTemperature()
        val faceNear = sensorMonitor.isFaceNear()

        return "Battery: ${batteryPercent}% | Temp: ${temperature}°C | Face: ${if (faceNear) "near" else "away"} | FPS: $currentTargetFps"
    }
}
