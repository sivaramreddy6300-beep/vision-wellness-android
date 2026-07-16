package com.example.visionwellness.advanced

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import timber.log.Timber
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Gaze tracking engine that estimates where on the screen the user is looking
 * Calculates 3D gaze point from eye landmarks
 */
class GazeTrackingEngine {

    companion object {
        // Eye landmark indices for gaze calculation
        private const val LEFT_EYE_CENTER = 33
        private const val RIGHT_EYE_CENTER = 362
        private const val LEFT_IRIS = 468
        private const val RIGHT_IRIS = 473
        private const val LEFT_PUPIL_LEFT = 469
        private const val LEFT_PUPIL_RIGHT = 470
        private const val RIGHT_PUPIL_LEFT = 474
        private const val RIGHT_PUPIL_RIGHT = 475
    }

    private var lastGazePoint: GazePoint = GazePoint(0.5f, 0.5f)  // Center of screen
    private var gazeHistory = mutableListOf<GazePoint>()
    private val maxHistorySize = 30  // Keep last 30 gaze points

    /**
     * Calculate current gaze point on screen from eye landmarks
     * @param landmarks Face landmarks from MediaPipe
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return GazePoint with x,y coordinates (0-1 normalized, 0,0 = top-left)
     */
    fun estimateGazePoint(
        landmarks: List<NormalizedLandmark>,
        screenWidth: Int,
        screenHeight: Int
    ): GazePoint {
        return try {
            if (landmarks.size < 478) {  // Need all iris landmarks
                return lastGazePoint
            }

            // Extract eye landmarks
            val leftEyeCenter = landmarks[LEFT_EYE_CENTER]
            val rightEyeCenter = landmarks[RIGHT_EYE_CENTER]
            val leftIris = landmarks[LEFT_IRIS]
            val rightIris = landmarks[RIGHT_IRIS]
            val leftPupilLeft = landmarks[LEFT_PUPIL_LEFT]
            val leftPupilRight = landmarks[LEFT_PUPIL_RIGHT]
            val rightPupilLeft = landmarks[RIGHT_PUPIL_LEFT]
            val rightPupilRight = landmarks[RIGHT_PUPIL_RIGHT]

            // Calculate gaze vector for left eye
            val leftGazeVector = calculateGazeVector(
                leftEyeCenter, leftIris, leftPupilLeft, leftPupilRight
            )

            // Calculate gaze vector for right eye
            val rightGazeVector = calculateGazeVector(
                rightEyeCenter, rightIris, rightPupilLeft, rightPupilRight
            )

            // Average the gaze vectors
            val averagedGaze = GazeVector(
                x = (leftGazeVector.x + rightGazeVector.x) / 2f,
                y = (leftGazeVector.y + rightGazeVector.y) / 2f,
                z = (leftGazeVector.z + rightGazeVector.z) / 2f
            )

            // Convert gaze vector to screen coordinates
            val gazePoint = convertGazeVectorToScreenCoordinates(
                averagedGaze,
                screenWidth,
                screenHeight
            )

            // Apply smoothing filter
            val smoothedGaze = applyGazeSmoothing(gazePoint)

            // Update history
            gazeHistory.add(smoothedGaze)
            if (gazeHistory.size > maxHistorySize) {
                gazeHistory.removeAt(0)
            }

            lastGazePoint = smoothedGaze
            Timber.v("Gaze point: (%.2f, %.2f)".format(smoothedGaze.x, smoothedGaze.y))
            smoothedGaze
        } catch (e: Exception) {
            Timber.e(e, "Error estimating gaze point")
            lastGazePoint
        }
    }

    /**
     * Calculate gaze vector from eye center and iris position
     */
    private fun calculateGazeVector(
        eyeCenter: NormalizedLandmark,
        iris: NormalizedLandmark,
        pupilLeft: NormalizedLandmark,
        pupilRight: NormalizedLandmark
    ): GazeVector {
        // Vector from eye center to iris
        val irisDx = iris.x() - eyeCenter.x()
        val irisDy = iris.y() - eyeCenter.y()
        val irisDz = iris.z() - eyeCenter.z()

        // Normalize
        val irisMagnitude = sqrt(irisDx.pow(2) + irisDy.pow(2) + irisDz.pow(2))

        return if (irisMagnitude > 0) {
            GazeVector(
                x = irisDx / irisMagnitude,
                y = irisDy / irisMagnitude,
                z = irisDz / irisMagnitude
            )
        } else {
            GazeVector(0f, 0f, 1f)  // Default forward gaze
        }
    }

    /**
     * Convert 3D gaze vector to 2D screen coordinates
     */
    private fun convertGazeVectorToScreenCoordinates(
        gazeVector: GazeVector,
        screenWidth: Int,
        screenHeight: Int
    ): GazePoint {
        // Convert gaze direction to angle
        val angleX = atan2(gazeVector.y, gazeVector.z)  // Vertical angle
        val angleY = atan2(gazeVector.x, gazeVector.z)  // Horizontal angle

        // Map angles to screen coordinates
        // Assume ~60 degree field of view
        val fov = 60f * (3.14159f / 180f)  // Convert to radians
        val maxAngle = fov / 2f

        // Clamp angles
        val clampedAngleX = angleX.coerceIn(-maxAngle, maxAngle)
        val clampedAngleY = angleY.coerceIn(-maxAngle, maxAngle)

        // Convert to screen space (0-1)
        val screenX = 0.5f + (clampedAngleY / maxAngle) * 0.5f
        val screenY = 0.5f + (clampedAngleX / maxAngle) * 0.5f

        return GazePoint(
            x = screenX.coerceIn(0f, 1f),
            y = screenY.coerceIn(0f, 1f)
        )
    }

    /**
     * Apply temporal smoothing to gaze points (reduce jitter)
     */
    private fun applyGazeSmoothing(currentGaze: GazePoint): GazePoint {
        if (gazeHistory.isEmpty()) return currentGaze

        // Use exponential moving average for smoothing
        val alpha = 0.3f  // Smoothing factor (0-1, higher = more responsive)
        val smoothedX = alpha * currentGaze.x + (1 - alpha) * lastGazePoint.x
        val smoothedY = alpha * currentGaze.y + (1 - alpha) * lastGazePoint.y

        return GazePoint(smoothedX, smoothedY)
    }

    /**
     * Get current gaze point
     */
    fun getCurrentGazePoint(): GazePoint = lastGazePoint

    /**
     * Get gaze point history (for trend analysis)
     */
    fun getGazeHistory(): List<GazePoint> = gazeHistory.toList()

    /**
     * Calculate gaze stability (how steady the gaze is)
     * Returns 0-1 where 1 = perfectly steady
     */
    fun getGazeStability(): Float {
        if (gazeHistory.size < 5) return 0.5f

        // Calculate standard deviation of gaze points
        val recentGaze = gazeHistory.takeLast(10)
        val meanX = recentGaze.map { it.x }.average().toFloat()
        val meanY = recentGaze.map { it.y }.average().toFloat()

        val varianceX = recentGaze.map { (it.x - meanX).pow(2) }.average()
        val varianceY = recentGaze.map { (it.y - meanY).pow(2) }.average()
        val stdDev = sqrt(varianceX + varianceY)

        // Convert std dev to stability score (lower std dev = higher stability)
        return (1f / (1f + stdDev)).coerceIn(0f, 1f)
    }

    /**
     * Check if gaze is fixated on a point (not moving)
     */
    fun isGazeFixated(stabilityThreshold: Float = 0.8f): Boolean {
        return getGazeStability() > stabilityThreshold
    }

    /**
     * Get gaze velocity (how fast eyes are moving)
     */
    fun getGazeVelocity(): Float {
        if (gazeHistory.size < 2) return 0f

        val previous = gazeHistory[gazeHistory.size - 2]
        val current = gazeHistory[gazeHistory.size - 1]

        val dx = current.x - previous.x
        val dy = current.y - previous.y

        return sqrt(dx.pow(2) + dy.pow(2))
    }

    data class GazePoint(
        val x: Float,  // 0-1, 0 = left edge
        val y: Float   // 0-1, 0 = top edge
    ) {
        fun toPixels(screenWidth: Int, screenHeight: Int): GazePointPixels {
            return GazePointPixels(
                x = (x * screenWidth).toInt(),
                y = (y * screenHeight).toInt()
            )
        }
    }

    data class GazePointPixels(
        val x: Int,
        val y: Int
    )

    private data class GazeVector(
        val x: Float,
        val y: Float,
        val z: Float
    )
}
