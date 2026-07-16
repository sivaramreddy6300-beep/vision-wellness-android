package com.example.visionwellness.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.IBinder
import android.view.Surface
import androidx.core.app.NotificationCompat
import com.example.visionwellness.R
import com.example.visionwellness.advanced.CloudSyncManager
import com.example.visionwellness.advanced.GazeTrackingEngine
import com.example.visionwellness.advanced.IrisRecognitionEngine
import com.example.visionwellness.advanced.NotificationCustomizer
import com.example.visionwellness.detection.BlinkDetectionListener
import com.example.visionwellness.detection.EyeDetectionEngine
import com.example.visionwellness.detection.OptimizedCameraFrameProcessor
import com.example.visionwellness.database.BlinkDatabase
import com.example.visionwellness.database.BlinkEntity
import com.example.visionwellness.optimization.AdaptiveFrameRateManager
import com.example.visionwellness.optimization.BackgroundTaskScheduler
import com.example.visionwellness.optimization.BatteryMonitor
import com.example.visionwellness.optimization.MemoryProfiler
import com.example.visionwellness.optimization.SensorMonitor
import com.example.visionwellness.ui.AlertOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EyeTrackingService : Service(), BlinkDetectionListener {

    companion object {
        private const val CHANNEL_ID = "vision_wellness_channel"
        private const val NOTIFICATION_ID = 1
        private const val CAMERA_WIDTH = 480
        private const val CAMERA_HEIGHT = 360
    }

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    private lateinit var eyeDetectionEngine: EyeDetectionEngine
    private lateinit var frameProcessor: OptimizedCameraFrameProcessor
    private lateinit var overlayManager: AlertOverlayManager
    private lateinit var database: BlinkDatabase

    // Optimization components
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var sensorMonitor: SensorMonitor
    private lateinit var adaptiveFrameRateManager: AdaptiveFrameRateManager
    private lateinit var memoryProfiler: MemoryProfiler
    private lateinit var backgroundTaskScheduler: BackgroundTaskScheduler

    // Advanced features
    private lateinit var irisRecognitionEngine: IrisRecognitionEngine
    private lateinit var gazeTrackingEngine: GazeTrackingEngine
    private lateinit var notificationCustomizer: NotificationCustomizer
    private lateinit var cloudSyncManager: CloudSyncManager

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private var blinkCountToday = 0
    private var totalStaringTimeToday: Long = 0
    private var lastStaringTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        Timber.d("EyeTrackingService created")
        createNotificationChannel()
        initializeComponents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("EyeTrackingService started")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Schedule background profiling
        backgroundTaskScheduler.scheduleBatteryProfiling()

        // Start sensor monitoring
        sensorMonitor.startMonitoring()

        // Start camera and eye tracking
        startCameraCapture()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("EyeTrackingService destroyed")
        stopCameraCapture()
        cleanup()
    }

    /**
     * Initialize all components including advanced features
     */
    private fun initializeComponents() {
        try {
            cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            database = BlinkDatabase.getInstance(this)
            overlayManager = AlertOverlayManager(this)

            // Initialize optimization components
            batteryMonitor = BatteryMonitor(this)
            memoryProfiler = MemoryProfiler(this)

            sensorMonitor = SensorMonitor(this) { isNear ->
                Timber.d("Proximity changed: face is ${if (isNear) "near" else "away"}")
            }

            adaptiveFrameRateManager = AdaptiveFrameRateManager(
                batteryMonitor,
                sensorMonitor
            )

            backgroundTaskScheduler = BackgroundTaskScheduler(this)

            // Initialize advanced features
            irisRecognitionEngine = IrisRecognitionEngine(this)
            gazeTrackingEngine = GazeTrackingEngine()
            notificationCustomizer = NotificationCustomizer(this)
            cloudSyncManager = CloudSyncManager(this)

            // Initialize detection engine and frame processor
            eyeDetectionEngine = EyeDetectionEngine(this, this)
            frameProcessor = OptimizedCameraFrameProcessor(
                eyeDetectionEngine,
                adaptiveFrameRateManager
            )

            Timber.d("All components initialized successfully")
            Timber.d("Advanced features: Iris, Gaze, Notifications, Cloud Sync")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize components")
        }
    }

    /**
     * Start camera capture and frame processing
     */
    private fun startCameraCapture() {
        try {
            val cameraId = getFrontCameraId() ?: return
            Timber.d("Starting camera capture with camera ID: $cameraId")

            imageReader = ImageReader.newInstance(
                CAMERA_WIDTH,
                CAMERA_HEIGHT,
                ImageFormat.YUV_420_888,
                2
            ).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val bitmap = imageToBitmap(image)
                        frameProcessor.processFrame(bitmap)

                        // Periodic memory optimization
                        if (blinkCountToday % 100 == 0) {
                            memoryProfiler.optimizeMemoryIfNeeded()
                            memoryProfiler.logMemoryUsage()
                        }
                    } finally {
                        image.close()
                    }
                }, null)
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Timber.w("Camera disconnected")
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Timber.e("Camera error: $error")
                    camera.close()
                }
            }, null)

        } catch (e: Exception) {
            Timber.e(e, "Error starting camera capture")
        }
    }

    /**
     * Create camera capture session
     */
    private fun createCaptureSession(camera: CameraDevice) {
        try {
            val surface = imageReader?.surface ?: return
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(surface)

            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                null
                            )
                            Timber.d("Camera capture session started")
                        } catch (e: Exception) {
                            Timber.e(e, "Error starting capture request")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Timber.e("Camera capture session configuration failed")
                    }
                },
                null
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating capture session")
        }
    }

    /**
     * Stop camera capture
     */
    private fun stopCameraCapture() {
        try {
            cameraCaptureSession?.stopRepeating()
            cameraCaptureSession?.close()
            cameraCaptureSession = null

            imageReader?.close()
            imageReader = null

            cameraDevice?.close()
            cameraDevice = null

            sensorMonitor.stopMonitoring()

            Timber.d("Camera capture stopped")
        } catch (e: Exception) {
            Timber.e(e, "Error stopping camera capture")
        }
    }

    /**
     * Get front camera ID
     */
    private fun getFrontCameraId(): String? {
        try {
            val cameraIdList = cameraManager.cameraIdList
            for (cameraId in cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)
                if (facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting front camera ID")
        }
        return null
    }

    /**
     * Convert YUV Image to Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uvSize = planes[1].buffer.remaining() + planes[2].buffer.remaining()
        val nv21 = ByteArray(ySize + uvSize)

        planes[0].buffer.get(nv21, 0, ySize)
        planes[1].buffer.get(nv21, ySize, planes[1].buffer.remaining())
        planes[2].buffer.get(nv21, ySize + planes[1].buffer.remaining(), planes[2].buffer.remaining())

        val bitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        return bitmap
    }

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vision Wellness Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors your blinking habits to prevent eye strain"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground notification with advanced status
     */
    private fun createNotification(): NotificationCompat.Notification {
        val statusMessage = adaptiveFrameRateManager.getStatusMessage()
        val gazePoint = gazeTrackingEngine.getCurrentGazePoint()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vision Wellness")
            .setContentText("Blinks: $blinkCountToday | $statusMessage | Gaze: (%.0f%%, %.0f%%)".format(
                gazePoint.x * 100, gazePoint.y * 100
            ))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    /**
     * Update notification with current stats
     */
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cleanup resources
     */
    private fun cleanup() {
        try {
            frameProcessor.release()
            eyeDetectionEngine.release()
            overlayManager.release()
            backgroundTaskScheduler.cancelBatteryProfiling()
            Timber.d("Resources cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
        }
    }

    // ==================== BlinkDetectionListener Callbacks ====================

    override fun onBlinkDetected(timestamp: Long) {
        blinkCountToday++
        irisRecognitionEngine.recordBlink()
        Timber.d("Blink detected! Total blinks today: $blinkCountToday")
        updateNotification()
        recordBlinkToDatabase()
    }

    override fun onStaringDetected(durationMs: Long) {
        totalStaringTimeToday += durationMs
        lastStaringTime = System.currentTimeMillis()
        Timber.w("Staring detected for ${durationMs / 1000}s! Total staring time today: ${totalStaringTimeToday / 1000}s")
        
        // Check if notification should be shown (respects DND, frequency)
        if (notificationCustomizer.shouldShowStaringAlert()) {
            triggerStaringAlert()
        }
    }

    override fun onEyesClosed(timestamp: Long) {
        Timber.d("Eyes closed at $timestamp")
    }

    override fun onDetectionError(error: String) {
        Timber.e("Detection error: $error")
    }

    /**
     * Record blink data to database
     */
    private fun recordBlinkToDatabase() {
        serviceScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dateString = dateFormat.format(Date())
                val calendar = java.util.Calendar.getInstance()
                val hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY)

                val blinkEntity = BlinkEntity(
                    timestamp = System.currentTimeMillis(),
                    blinkCount = 1,
                    averageEAR = eyeDetectionEngine.getCurrentEARValues().average().toFloat(),
                    staringDuration = 0L,
                    hourOfDay = hourOfDay,
                    dateString = dateString
                )

                database.blinkDao().insertBlinkData(blinkEntity)

                // Sync to cloud periodically
                if (cloudSyncManager.shouldSync()) {
                    cloudSyncManager.syncBlinkData(
                        userId = "user_${Build.SERIAL}",
                        blinkCount = blinkCountToday,
                        averageBlinkRate = blinkCountToday / (System.currentTimeMillis() / 60000f),
                        totalStaringTime = totalStaringTimeToday
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error recording blink data")
            }
        }
    }

    /**
     * Trigger staring alert overlay
     */
    private fun triggerStaringAlert() {
        Timber.w("Triggering staring alert overlay")
        overlayManager.triggerStaringAlert()
    }
}
