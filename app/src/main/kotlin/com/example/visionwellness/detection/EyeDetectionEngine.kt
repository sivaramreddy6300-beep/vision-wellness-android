package com.example.visionwellness.detection

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.facemesh.FaceMesh
import com.google.mediapipe.tasks.vision.facemesh.FaceMeshResult
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Eye Detection Engine using Google MediaPipe Face Mesh
 *
 * This engine detects facial landmarks and calculates Eye Aspect Ratio (EAR)
 * to determine blinks and staring episodes. It processes frames at 8fps
 * for battery efficiency.
 *
 * Eye Aspect Ratio Formula:
 * EAR = (||p2 - p6|| + ||p3 - p5||) / (2 * ||p1 - p4||)
 *
 * Where p1-p6 are the eye landmark coordinates:
 * p1: Eye top
 * p2: Eye top-right
 * p3: Eye bottom-right
 * p4: Eye bottom
 * p5: Eye bottom-left
 * p6: Eye top-left
 */
class EyeDetectionEngine(
    private val context: Context,
    private val listener: BlinkDetectionListener
) {

    private var faceMesh: FaceMesh? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Configuration constants
    companion object {
        // Eye Aspect Ratio thresholds
        private const val EAR_BLINK_THRESHOLD = 0.2f  // Below this = blink
        private const val EAR_STARING_THRESHOLD = 0.3f  // Above this for 5s = staring
        private const val STARING_DURATION_MS = 5000L  // 5 seconds
        private const val DEBOUNCE_DURATION_MS = 100L  // 100ms debounce

        // Blink detection constants
        private const val MIN_FRAMES_FOR_BLINK = 2  // Minimum consecutive low EAR frames
        private const val MAX_FRAMES_FOR_BLINK = 15  // Maximum consecutive low EAR frames (at 8fps ~=2 sec)

        // Eye landmark indices (MediaPipe Face Mesh)
        private const val LEFT_EYE_INDICES = "33,133,160,159,158,157,173,133"
        private const val RIGHT_EYE_INDICES = "362,263,387,386,385,384,398,263"

        // Left eye specific indices
        private val LEFT_EYE_TOP = 33
        private val LEFT_EYE_TOP_RIGHT = 160
        private val LEFT_EYE_BOTTOM_RIGHT = 159
        private val LEFT_EYE_BOTTOM = 158
        private val LEFT_EYE_BOTTOM_LEFT = 157
        private val LEFT_EYE_TOP_LEFT = 173

        // Right eye specific indices
        private val RIGHT_EYE_TOP = 362
        private val RIGHT_EYE_TOP_RIGHT = 387
        private val RIGHT_EYE_BOTTOM_RIGHT = 386
        private val RIGHT_EYE_BOTTOM = 385
        private val RIGHT_EYE_BOTTOM_LEFT = 384
        private val RIGHT_EYE_TOP_LEFT = 398
    }

    // State tracking
    private var consecutiveLowEARFrames = 0
    private var staringStartTime: Long? = null
    private var lastBlinkTime: Long = 0
    private var lastStaringTime: Long = 0
    private var lastEARValues = FloatArray(2)  // [left, right]

    init {
        Timber.d("EyeDetectionEngine initializing")
        initializeMediaPipe()
    }

    /**
     * Initialize MediaPipe Face Mesh model
     */
    private fun initializeMediaPipe() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceMesh.FaceMeshOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(com.google.mediapipe.tasks.vision.core.RunningMode.IMAGE)
                .setNumFaces(1)  // We only need to detect one face (the user's)
                .build()

            faceMesh = FaceMesh.createFromOptions(context, options)
            Timber.d("MediaPipe Face Mesh initialized successfully")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize MediaPipe Face Mesh")
            listener.onDetectionError("Failed to initialize Face Mesh: ${e.message}")
        }
    }

    /**
     * Process a camera frame and detect blinks/staring
     * @param bitmap The camera frame as a bitmap
     */
    fun processFrame(bitmap: Bitmap) {
        try {
            val faceMesh = faceMesh ?: return

            // Convert bitmap to MediaPipe Image
            val mpImage = BitmapImageBuilder(bitmap).build()

            // Detect face landmarks
            val result = faceMesh.detect(mpImage)

            // Process the detection results
            if (result.faceLandmarks().isNotEmpty()) {
                val landmarks = result.faceLandmarks()[0]
                processLandmarks(landmarks.landmarkList)
            } else {
                // No face detected, reset state
                resetDetectionState()
            }

        } catch (e: Exception) {
            Timber.e(e, "Error processing frame")
            listener.onDetectionError("Frame processing error: ${e.message}")
        }
    }

    /**
     * Process facial landmarks to detect blinks and staring
     * @param landmarks List of normalized landmark coordinates
     */
    private fun processLandmarks(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        if (landmarks.size < 468) {  // MediaPipe Face Mesh has 468 landmarks
            Timber.w("Insufficient landmarks detected: ${landmarks.size}")
            return
        }

        // Calculate Eye Aspect Ratio for both eyes
        val leftEAR = calculateEAR(
            landmarks,
            LEFT_EYE_TOP,
            LEFT_EYE_TOP_RIGHT,
            LEFT_EYE_BOTTOM_RIGHT,
            LEFT_EYE_BOTTOM,
            LEFT_EYE_BOTTOM_LEFT,
            LEFT_EYE_TOP_LEFT
        )

        val rightEAR = calculateEAR(
            landmarks,
            RIGHT_EYE_TOP,
            RIGHT_EYE_TOP_RIGHT,
            RIGHT_EYE_BOTTOM_RIGHT,
            RIGHT_EYE_BOTTOM,
            RIGHT_EYE_BOTTOM_LEFT,
            RIGHT_EYE_TOP_LEFT
        )

        // Average the EAR values
        val averageEAR = (leftEAR + rightEAR) / 2f
        lastEARValues[0] = leftEAR
        lastEARValues[1] = rightEAR

        Timber.v("EAR - Left: %.2f, Right: %.2f, Avg: %.2f".format(leftEAR, rightEAR, averageEAR))

        // Detect blinks (low EAR)
        if (averageEAR < EAR_BLINK_THRESHOLD) {
            consecutiveLowEARFrames++
            if (consecutiveLowEARFrames == MIN_FRAMES_FOR_BLINK) {
                Timber.d("Blink detected")
                triggerBlinkDetected()
            }
        } else if (averageEAR >= EAR_BLINK_THRESHOLD) {
            // Reset blink counter when eyes open
            if (consecutiveLowEARFrames > 0) {
                Timber.v("Eyes opened after %d low-EAR frames".format(consecutiveLowEARFrames))
            }
            consecutiveLowEARFrames = 0
        }

        // Detect staring (high EAR for extended duration)
        if (averageEAR > EAR_STARING_THRESHOLD) {
            if (staringStartTime == null) {
                staringStartTime = System.currentTimeMillis()
                Timber.d("Staring detected - starting timer")
            } else {
                val staringDuration = System.currentTimeMillis() - staringStartTime!!
                if (staringDuration >= STARING_DURATION_MS) {
                    triggerStaringDetected(staringDuration)
                    staringStartTime = System.currentTimeMillis()  // Reset timer
                }
            }
        } else {
            // Reset staring timer when eyes blink
            if (staringStartTime != null) {
                Timber.d("Staring interrupted")
                staringStartTime = null
            }
        }
    }

    /**
     * Calculate Eye Aspect Ratio (EAR) for an eye
     *
     * EAR = (||p2 - p6|| + ||p3 - p5||) / (2 * ||p1 - p4||)
     *
     * @param landmarks All 468 facial landmarks
     * @param p1Index Top of eye
     * @param p2Index Top-right
     * @param p3Index Bottom-right
     * @param p4Index Bottom
     * @param p5Index Bottom-left
     * @param p6Index Top-left
     * @return The Eye Aspect Ratio
     */
    private fun calculateEAR(
        landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
        p1Index: Int,
        p2Index: Int,
        p3Index: Int,
        p4Index: Int,
        p5Index: Int,
        p6Index: Int
    ): Float {
        val p1 = landmarks[p1Index]
        val p2 = landmarks[p2Index]
        val p3 = landmarks[p3Index]
        val p4 = landmarks[p4Index]
        val p5 = landmarks[p5Index]
        val p6 = landmarks[p6Index]

        // Calculate Euclidean distances
        val dist1 = euclideanDistance(p2, p6)  // ||p2 - p6||
        val dist2 = euclideanDistance(p3, p5)  // ||p3 - p5||
        val dist3 = euclideanDistance(p1, p4)  // ||p1 - p4||

        // EAR = (dist1 + dist2) / (2 * dist3)
        return if (dist3 != 0f) {
            (dist1 + dist2) / (2f * dist3)
        } else {
            0f
        }
    }

    /**
     * Calculate Euclidean distance between two landmarks
     */
    private fun euclideanDistance(
        p1: com.google.mediapipe.tasks.components.containers.NormalizedLandmark,
        p2: com.google.mediapipe.tasks.components.containers.NormalizedLandmark
    ): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        return sqrt(dx.pow(2) + dy.pow(2))
    }

    /**
     * Trigger blink detected callback with debounce
     */
    private fun triggerBlinkDetected() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBlinkTime >= DEBOUNCE_DURATION_MS) {
            lastBlinkTime = currentTime
            mainHandler.post {
                listener.onBlinkDetected(currentTime)
            }
        }
    }

    /**
     * Trigger staring detected callback with debounce
     */
    private fun triggerStaringDetected(durationMs: Long) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastStaringTime >= DEBOUNCE_DURATION_MS) {
            lastStaringTime = currentTime
            mainHandler.post {
                listener.onStaringDetected(durationMs)
            }
        }
    }

    /**
     * Reset detection state when no face is detected
     */
    private fun resetDetectionState() {
        consecutiveLowEARFrames = 0
        staringStartTime = null
    }

    /**
     * Get current Eye Aspect Ratio values
     * @return FloatArray [leftEAR, rightEAR]
     */
    fun getCurrentEARValues(): FloatArray = lastEARValues

    /**
     * Release resources
     */
    fun release() {
        try {
            faceMesh?.close()
            Timber.d("EyeDetectionEngine released")
        } catch (e: Exception) {
            Timber.e(e, "Error releasing EyeDetectionEngine")
        }
    }
}
