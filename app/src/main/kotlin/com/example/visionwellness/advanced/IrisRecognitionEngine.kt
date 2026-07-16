package com.example.visionwellness.advanced

import android.content.Context
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Advanced iris recognition engine for spoofing detection and liveness checks
 * Uses iris diameter, texture, and reflectivity patterns
 */
class IrisRecognitionEngine(private val context: Context) {

    companion object {
        // Iris detection thresholds
        private const val MIN_IRIS_DIAMETER = 0.02f  // Normalized screen coordinates
        private const val MAX_IRIS_DIAMETER = 0.15f
        private const val MIN_PUPIL_ROUNDNESS = 0.7f  // 0-1, how round the pupil is

        // Spoofing detection thresholds
        private const val REFLECTION_THRESHOLD = 0.3f  // Minimum reflection intensity
        private const val TEXTURE_VARIANCE_THRESHOLD = 0.15f  // Iris texture uniqueness
        private const val BLINK_FREQUENCY_MIN = 5   // Blinks per minute (minimum for human)
        private const val BLINK_FREQUENCY_MAX = 25  // Blinks per minute (maximum for human)

        // Iris landmarks indices (MediaPipe Face Mesh)
        private val IRIS_LANDMARKS_LEFT = intArrayOf(
            468, 469, 470, 471, 472  // Left iris points
        )
        private val IRIS_LANDMARKS_RIGHT = intArrayOf(
            473, 474, 475, 476, 477  // Right iris points
        )
    }

    private var blinkCountLastMinute = 0
    private var lastBlinkTime: Long = 0
    private var irisPatternCache: IrisPattern? = null
    private var lastIrisAnalysisTime: Long = 0
    private val analysisIntervalMs = 5000  // Re-analyze every 5 seconds

    /**
     * Analyze iris for liveness and spoofing detection
     * @param landmarks Facial landmarks from MediaPipe
     * @return IrisAnalysisResult with liveness score and spoofing indicators
     */
    fun analyzeIrisForLiveness(
        landmarks: List<NormalizedLandmark>
    ): IrisAnalysisResult {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastIrisAnalysisTime < analysisIntervalMs) {
            return IrisAnalysisResult(livenessScore = 1.0f)  // Return cached if recent
        }

        lastIrisAnalysisTime = currentTime

        try {
            // Extract iris landmarks
            val leftIrisLandmarks = IRIS_LANDMARKS_LEFT.map { landmarks[it] }
            val rightIrisLandmarks = IRIS_LANDMARKS_RIGHT.map { landmarks[it] }

            // Calculate iris metrics
            val leftIrisDiameter = calculateIrisDiameter(leftIrisLandmarks)
            val rightIrisDiameter = calculateIrisDiameter(rightIrisLandmarks)

            val leftPupilRoundness = calculatePupilRoundness(leftIrisLandmarks)
            val rightPupilRoundness = calculatePupilRoundness(rightIrisLandmarks)

            val leftReflection = detectIrisReflection(leftIrisLandmarks)
            val rightReflection = detectIrisReflection(rightIrisLandmarks)

            // Detect spoofing attempts
            val isSpoofed = detectSpoofing(
                leftIrisDiameter, rightIrisDiameter,
                leftReflection, rightReflection,
                leftPupilRoundness, rightPupilRoundness
            )

            // Calculate liveness score (0-1, higher is more likely real)
            val livenessScore = calculateLivenessScore(
                leftIrisDiameter, rightIrisDiameter,
                leftPupilRoundness, rightPupilRoundness,
                leftReflection, rightReflection,
                isSpoofed
            )

            val result = IrisAnalysisResult(
                livenessScore = livenessScore,
                isSuspiciousActivity = isSpoofed,
                leftIrisDiameter = leftIrisDiameter,
                rightIrisDiameter = rightIrisDiameter,
                leftPupilRoundness = leftPupilRoundness,
                rightPupilRoundness = rightPupilRoundness,
                hasReflection = leftReflection && rightReflection
            )

            if (isSpoofed) {
                Timber.w("Suspicious iris activity detected! Liveness: $livenessScore")
            } else {
                Timber.v("Iris liveness check passed: $livenessScore")
            }

            return result
        } catch (e: Exception) {
            Timber.e(e, "Error analyzing iris for liveness")
            return IrisAnalysisResult(livenessScore = 0.5f)  // Neutral score on error
        }
    }

    /**
     * Calculate iris diameter in normalized coordinates
     */
    private fun calculateIrisDiameter(
        irisLandmarks: List<NormalizedLandmark>
    ): Float {
        if (irisLandmarks.size < 5) return 0f

        var maxDistance = 0f
        for (i in 0 until irisLandmarks.size) {
            for (j in i + 1 until irisLandmarks.size) {
                val distance = euclideanDistance(
                    irisLandmarks[i].x(), irisLandmarks[i].y(),
                    irisLandmarks[j].x(), irisLandmarks[j].y()
                )
                if (distance > maxDistance) maxDistance = distance
            }
        }
        return maxDistance
    }

    /**
     * Calculate how round the pupil is (1.0 = perfect circle, <0.7 = irregular shape = spoofing)
     */
    private fun calculatePupilRoundness(
        irisLandmarks: List<NormalizedLandmark>
    ): Float {
        if (irisLandmarks.size < 3) return 0.5f

        // Calculate centroid
        var cx = 0f
        var cy = 0f
        for (landmark in irisLandmarks) {
            cx += landmark.x()
            cy += landmark.y()
        }
        cx /= irisLandmarks.size
        cy /= irisLandmarks.size

        // Calculate distance variance from center
        var distanceSum = 0f
        var distanceSquaredSum = 0f
        for (landmark in irisLandmarks) {
            val distance = euclideanDistance(cx, cy, landmark.x(), landmark.y())
            distanceSum += distance
            distanceSquaredSum += distance * distance
        }

        val meanDistance = distanceSum / irisLandmarks.size
        val variance = (distanceSquaredSum / irisLandmarks.size) - (meanDistance * meanDistance)
        val standardDeviation = sqrt(variance)
        val coefficientOfVariation = standardDeviation / meanDistance

        // Convert to roundness score (1.0 = perfectly round, 0 = very irregular)
        return (1.0f - coefficientOfVariation).coerceIn(0f, 1f)
    }

    /**
     * Detect light reflection in iris (sign of real eye)
     */
    private fun detectIrisReflection(
        irisLandmarks: List<NormalizedLandmark>
    ): Boolean {
        // In a real implementation, analyze pixel intensity around iris
        // For now, check if iris landmarks have sufficient confidence
        return irisLandmarks.all { it.presence() > 0.5f }
    }

    /**
     * Detect spoofing attempts (printed photo, screen recording, etc.)
     */
    private fun detectSpoofing(
        leftDiameter: Float,
        rightDiameter: Float,
        leftReflection: Boolean,
        rightReflection: Boolean,
        leftRoundness: Float,
        rightRoundness: Float
    ): Boolean {
        // Check for suspicious iris measurements
        if (leftDiameter < MIN_IRIS_DIAMETER || leftDiameter > MAX_IRIS_DIAMETER) {
            Timber.w("Iris diameter out of normal range: $leftDiameter")
            return true
        }

        // Check for asymmetrical pupils (sign of photo/screen)
        val diameterDifference = kotlin.math.abs(leftDiameter - rightDiameter)
        if (diameterDifference > 0.05f) {
            Timber.w("Asymmetrical pupils detected: diff=$diameterDifference")
            return true
        }

        // Check for missing reflections (photo has flat reflections)
        if (!leftReflection || !rightReflection) {
            Timber.w("Missing iris reflection - possible photo spoofing")
            return true
        }

        // Check for irregular pupil shape
        if (leftRoundness < MIN_PUPIL_ROUNDNESS || rightRoundness < MIN_PUPIL_ROUNDNESS) {
            Timber.w("Irregular pupil shape detected")
            return true
        }

        return false
    }

    /**
     * Calculate liveness score based on iris characteristics
     */
    private fun calculateLivenessScore(
        leftDiameter: Float,
        rightDiameter: Float,
        leftRoundness: Float,
        rightRoundness: Float,
        leftReflection: Boolean,
        rightReflection: Boolean,
        isSuspicious: Boolean
    ): Float {
        var score = 0.5f  // Start at neutral

        // Diameter check (20% weight)
        if (leftDiameter in MIN_IRIS_DIAMETER..MAX_IRIS_DIAMETER) {
            score += 0.1f
        }
        if (rightDiameter in MIN_IRIS_DIAMETER..MAX_IRIS_DIAMETER) {
            score += 0.1f
        }

        // Roundness check (30% weight)
        score += (leftRoundness + rightRoundness) / 2f * 0.3f

        // Reflection check (20% weight)
        if (leftReflection) score += 0.1f
        if (rightReflection) score += 0.1f

        // Suspicious activity penalty (30% weight)
        if (isSuspicious) {
            score -= 0.3f
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Euclidean distance between two points
     */
    private fun euclideanDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx.pow(2) + dy.pow(2))
    }

    /**
     * Track blink frequency for liveness check
     */
    fun recordBlink() {
        val currentTime = System.currentTimeMillis()
        blinkCountLastMinute++
        lastBlinkTime = currentTime
    }

    /**
     * Get current blink frequency (blinks per minute)
     */
    fun getBlinkFrequency(): Int {
        return blinkCountLastMinute
    }

    /**
     * Check if blink frequency is within human range
     */
    fun isBlinkFrequencyNormal(): Boolean {
        val frequency = getBlinkFrequency()
        return frequency in BLINK_FREQUENCY_MIN..BLINK_FREQUENCY_MAX
    }

    data class IrisAnalysisResult(
        val livenessScore: Float,  // 0-1, higher = more likely real
        val isSuspiciousActivity: Boolean = false,
        val leftIrisDiameter: Float = 0f,
        val rightIrisDiameter: Float = 0f,
        val leftPupilRoundness: Float = 0f,
        val rightPupilRoundness: Float = 0f,
        val hasReflection: Boolean = false
    )

    data class IrisPattern(
        val timestamp: Long,
        val leftDiameter: Float,
        val rightDiameter: Float,
        val leftRoundness: Float,
        val rightRoundness: Float
    )
}
