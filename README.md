# Vision Wellness Android App

A mobile application to address Computer Vision Syndrome by monitoring eye blinking habits and providing gentle reminders to blink during extended screen usage.

## 🎯 Features

- **Real-time Eye Tracking**: Uses Google MediaPipe Face Mesh for lightweight, on-device facial landmark detection
- **Blink Detection**: Calculates Eye Aspect Ratio (EAR) to detect natural blinks
- **Staring Alerts**: Triggers subtle visual feedback when user hasn't blinked for 5+ seconds
- **Background Service**: Runs continuously as a foreground service without disrupting other apps
- **Battery Optimized**: Processes frames at 8 fps to minimize power consumption
- **Local Analytics**: Tracks blink statistics using SQLite database
- **Non-Intrusive UI**: System overlay provides gentle reminders without aggressive notifications

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Computer Vision**: Google MediaPipe Face Mesh
- **Database**: SQLite with Room ORM
- **Concurrency**: Kotlin Coroutines
- **Architecture**: MVVM with Foreground Services

## 📋 Project Structure

```
vision-wellness-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── kotlin/com/example/visionwellness/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── services/
│   │   │   │   │   └── EyeTrackingService.kt
│   │   │   │   ├── detection/
│   │   │   │   │   ├── EyeDetectionEngine.kt
│   │   │   │   │   ├── BlinkDetectionListener.kt
│   │   │   │   │   └── CameraFrameProcessor.kt
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraManager.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── OverlayView.kt
│   │   │   │   │   └── AlertOverlayManager.kt
│   │   │   │   └── database/
│   │   │   │       ├── BlinkDatabase.kt
│   │   │   │       ├── BlinkEntity.kt
│   │   │   │       └── BlinkDao.kt
│   │   │   └── res/
│   │   │       ├── layout/activity_main.xml
│   │   │       ├── values/strings.xml
│   │   │       └── raw/face_landmarker.task
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🚀 Quick Start

### Prerequisites
- Android Studio Giraffe (2022.3.1) or later
- Android SDK 34 or higher
- Kotlin 1.9.22 or later
- **MediaPipe Face Landmarker model** (required)

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/sivaramreddy6300-beep/vision-wellness-android.git
   cd vision-wellness-android
   ```

2. **Download MediaPipe Model**
   ```bash
   # Download the face landmarker model
   wget https://storage.googleapis.com/mediapipe-assets/face_landmarker.task
   
   # Place it in the resources folder
   cp face_landmarker.task app/src/main/res/raw/
   ```

3. **Open in Android Studio**
   - File → Open → Select the project directory
   - Wait for Gradle sync to complete

4. **Build the Project**
   ```bash
   ./gradlew build
   ```

5. **Run on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```

6. **Launch the App**
   - Find "Vision Wellness" in your app drawer
   - Grant camera and overlay permissions when prompted
   - Eye tracking service will start automatically

## 📱 Permissions Required

- `CAMERA` - Front camera access for eye tracking
- `FOREGROUND_SERVICE` - Background operation with persistent notification
- `SYSTEM_ALERT_WINDOW` - Display overlay for staring alerts
- `INTERNET` - Future analytics synchronization (optional)

## 🎨 Phase 3: Alert Overlay System (Complete)

### What's Included

**AlertOverlayManager.kt**
- Manages system-wide overlay window
- Uses `TYPE_APPLICATION_OVERLAY` for Android 8.0+
- `TYPE_PHONE` for older devices
- Non-focusable, non-touchable window (doesn't block user interaction)

**OverlayView.kt**
- Custom view rendering blue border gradient
- Smooth fade-in/hold/fade-out animation
- Animation timeline:
  - 0-200ms: Fade in (opacity 0 → 255)
  - 200-600ms: Hold at full opacity
  - 600-800ms: Fade out (opacity 255 → 0)

**Integration**
- `AlertOverlayManager` instantiated in `EyeTrackingService`
- Triggered by `onStaringDetected()` callback
- Non-intrusive (won't interfere with user's current app)

### How It Works

```
User staring for 5+ seconds
    ↓
EyeDetectionEngine detects staring
    ↓
Calls onStaringDetected()
    ↓
EyeTrackingService.triggerStaringAlert()
    ↓
AlertOverlayManager.showAlert()
    ↓
OverlayView displays blue border with animation
    ↓
User blinks → Overlay triggers again on next staring event
```

## 🧠 AI Agent Workflow

This project is designed to be built incrementally using AI agents:

### Phase 1: ✅ Core Architecture (Complete)
- Foreground Service setup
- Permission handling
- Camera initialization framework
- UI boilerplate
- Database schema

### Phase 2: ✅ Eye Detection Engine (Complete)
- MediaPipe Face Mesh integration
- Eye Aspect Ratio calculation
- Blink detection with debounce
- Staring detection (5+ seconds)
- Frame throttling (30fps → 8fps)

### Phase 3: ✅ Alert Overlay System (Complete)
- System-wide overlay window
- Blue border gradient animation
- Fade-in/hold/fade-out sequence
- Non-intrusive UI (doesn't block interaction)
- Works on top of all apps

### Phase 4: ⏳ Battery & Performance Optimization (Ready for Agent)
**Assign to: DevOps & Optimization Agent**

```
Review and optimize the camera framework for battery efficiency:
1. Profile current battery usage with Android Profiler
2. Implement adaptive frame rate based on device load
3. Add proximity sensor detection to pause tracking when screen is off
4. Implement temperature-aware throttling
5. Add WorkManager for background scheduling
6. Optimize YUV to RGB conversion (currently a placeholder)
7. Add battery usage telemetry and reporting
```

### Phase 5: ✅ Local Analytics Database (Schema Complete)
**Optional enhancement: Implement reporting dashboard**

```
Create a statistics screen to display:
1. Daily blink count
2. Average blink rate per minute
3. Total staring time today
4. Trends over the week
5. Export data for health records
```

## 📊 Architecture Overview

```
┌─────────────────────────────────────────────┐
│         MainActivity                         │
│  (Permission Requests & Service Control)    │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│    EyeTrackingService (Foreground)          │
│  (Manages camera stream & detection loop)   │
└────────────────┬────────────────────────────┘
                 │
    ┌────────────┼────────────┬───────────────┐
    ▼            ▼            ▼               ▼
┌─────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
│ Camera  │ │MediaPipe │ │ Overlay  │ │   Database   │
│Manager  │ │ Face Mesh│ │ Manager  │ │ (SQLite/Room)│
└─────────┘ └──────────┘ └──────────┘ └──────────────┘
    │            │            │               │
    └────────────┼────────────┴───────────────┘
                 │
        Frame Processing Loop
           (8fps throttled)
```

## 🔧 Configuration

### Camera Settings
- **Frame Rate**: 8 fps (optimized for battery)
- **Resolution**: 480x360 (adaptive)
- **EAR Threshold (Blink)**: 0.2
- **EAR Threshold (Staring)**: 0.3
- **Staring Duration**: 5 seconds

### Overlay Settings
- **Animation Duration**: 800ms total
- **Fade In**: 200ms
- **Hold**: 400ms
- **Fade Out**: 200ms
- **Border Thickness**: 8dp
- **Border Color**: Blue
- **Window Type**: OVERLAY (top layer)

### Notification Settings
- **Channel**: Low priority background service
- **Auto-dismiss**: No (persistent)
- **Sound/Vibration**: Disabled
- **Foreground Service Type**: Camera

## 📈 Testing Checklist

- [ ] App starts without crashing
- [ ] Camera permissions are requested and granted
- [ ] Overlay permissions are requested and granted
- [ ] Foreground service notification appears
- [ ] Camera feed is accessible
- [ ] Eye landmarks are detected on device
- [ ] Blinks are recognized reliably (5-20 blinks/min normal range)
- [ ] Staring is detected after 5+ seconds without blinking
- [ ] Blue overlay appears when staring detected
- [ ] Overlay animation is smooth (fade in, hold, fade out)
- [ ] Overlay doesn't block user interaction with other apps
- [ ] Blink data is saved to database
- [ ] Battery drain is minimal (< 5% per hour)
- [ ] App doesn't interfere with other apps using camera

## 🐛 Troubleshooting

### Camera Not Accessible
- Verify CAMERA permission is granted in Settings
- Check if another app is using the camera
- Restart the app and device if needed

### Overlay Not Showing
- Ensure SYSTEM_ALERT_WINDOW permission is granted
- Check device is not in Do Not Disturb mode
- Verify overlay window is being created (check logcat)
- Try granting overlay permission in Settings → Advanced

### High Battery Drain
- Reduce frame processing rate further (currently 8fps)
- Check for memory leaks using Android Profiler
- Verify YUV to RGB conversion is not blocking UI thread
- Disable other background services

### Poor Blink Detection
- Ensure adequate lighting on face
- Check that front camera is not obstructed
- Verify MediaPipe model is properly placed in res/raw/
- Adjust EAR thresholds if needed

## 📚 Resources

- [MediaPipe Documentation](https://developers.google.com/mediapipe)
- [MediaPipe Face Landmarker](https://developers.google.com/mediapipe/solutions/vision/face_landmarker)
- [Android Camera2 API](https://developer.android.com/reference/android/hardware/camera2/package-summary)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## 📄 License

MIT License - feel free to use this project for personal or commercial purposes.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

For issues or questions, please open a GitHub issue or contact the development team.

---

**Built with AI-assisted development** 🤖✨

**Current Status**: Phase 3 (Alert Overlay System) ✅ COMPLETE
