package com.example.visionwellness.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager as AndroidCameraManager
import timber.log.Timber

class CameraManager(private val context: Context) {

    private val androidCameraManager: AndroidCameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as AndroidCameraManager

    fun getFrontCameraId(): String? {
        return try {
            val cameraIdList = androidCameraManager.cameraIdList
            cameraIdList.firstOrNull { cameraId ->
                val characteristics = androidCameraManager.getCameraCharacteristics(cameraId)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            }
        } catch (e: CameraAccessException) {
            Timber.e(e, "Error accessing camera")
            null
        }
    }

    fun isCameraAvailable(): Boolean {
        return getFrontCameraId() != null
    }

    companion object {
        private const val CAMERA_FRAMES_PER_SECOND = 8  // Optimized for battery
    }
}