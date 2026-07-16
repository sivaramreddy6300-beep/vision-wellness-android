package com.example.visionwellness

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.visionwellness.services.EyeTrackingService
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Manifest.permission.FOREGROUND_SERVICE
            } else {
                null
            }
        ).filterNotNull().toTypedArray()
    }

    private lateinit var statusText: TextView
    private lateinit var stopButton: Button
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("MainActivity created")

        // Initialize UI elements
        statusText = findViewById(R.id.status_text)
        stopButton = findViewById(R.id.stop_service_button)

        stopButton.setOnClickListener {
            stopEyeTrackingService()
        }

        // Request required permissions
        if (!hasAllPermissions()) {
            requestPermissions()
        } else {
            startEyeTrackingService()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        Timber.d("Requesting permissions")
        statusText.text = "Requesting camera and overlay permissions..."
        ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Timber.d("All permissions granted")
                startEyeTrackingService()
            } else {
                Timber.w("Some permissions denied")
                statusText.text = "Permissions denied. Eye tracking cannot start."
            }
        }
    }

    private fun startEyeTrackingService() {
        if (!isServiceRunning) {
            val serviceIntent = Intent(this, EyeTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            isServiceRunning = true
            statusText.text = "Eye tracking service is running..."
            Timber.d("Eye tracking service started")
        }
    }

    private fun stopEyeTrackingService() {
        if (isServiceRunning) {
            val serviceIntent = Intent(this, EyeTrackingService::class.java)
            stopService(serviceIntent)
            isServiceRunning = false
            statusText.text = "Eye tracking service stopped"
            Timber.d("Eye tracking service stopped")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Service continues running even if activity is destroyed
        Timber.d("MainActivity destroyed")
    }
}