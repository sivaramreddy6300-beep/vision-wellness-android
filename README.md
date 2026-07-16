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
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraManager.kt
│   │   │   │   ├── ui/
│   │   │   │   │   └── OverlayView.kt
│   │   │   │   └── database/
│   │   │   │       ├── BlinkDatabase.kt
│   │   │   │       ├── BlinkEntity.kt
│   │   │   │       └── BlinkDao.kt
│   │   │   └── res/
│   │   │       ├── layout/activity_main.xml
│   │   │       ├── values/strings.xml
│   │   │       └── drawable/ic_eye_tracking.xml
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

### Setup Instructions

1. **Clone the Repository**
   ```bash
   git clone https://github.com/sivaramreddy6300-beep/vision-wellness-android.git
   cd vision-wellness-android
   ```

2. **Open in Android Studio**
   - File → Open → Select the project directory
   - Wait for Gradle sync to complete

3. **Build the Project**
   ```bash
   ./gradlew build
   ```

4. **Run on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```

5. **Launch the App**
   - Find "Vision Wellness" in your app drawer
   - Grant camera and overlay permissions when prompted
   - Eye tracking service will start automatically

## 📱 Permissions Required

- `CAMERA` - Front camera access for eye tracking
- `FOREGROUND_SERVICE` - Background operation with persistent notification
- `SYSTEM_ALERT_WINDOW` - Display overlay for staring alerts
- `INTERNET` - Future analytics synchronization (optional)

## 🧠 AI Agent Workflow

This project is designed to be built incrementally using AI agents. Each phase has ready-to-use prompts:

### Phase 1: ✅ Core Architecture (Complete)
- Foreground Service setup
- Permission handling
- Camera initialization framework
- UI boilerplate
- Database schema

### Phase 2: Eye Detection Engine (Ready for Agent)
**Assign to: Computer Vision Developer Agent**

```
Act as a Computer Vision Expert. I have a working Android Foreground Service 
that captures frames from the front camera at 8 fps using Camera2 API. 

Now integrate Google MediaPipe Face Mesh to:
1. Detect facial landmarks in real-time
2. Calculate the Eye Aspect Ratio (EAR) for both eyes using the formula:
   EAR = (||p2 - p6|| + ||p3 - p5||) / (2 * ||p1 - p4||)
3. Trigger a callback when EAR drops below 0.2 (blink detected)
4. Track staring duration when EAR stays above 0.3 for >5 seconds
5. Provide a debounce mechanism (100ms) to avoid false positives
6. Return blink events via a callback interface

The processing should happen on a background thread to avoid blocking the camera stream.
```

### Phase 3: UI/UX Overlay System (Ready for Agent)
**Assign to: UI/UX Developer Agent**

```
Build a System Alert Window overlay in Kotlin/Android that:
1. Displays a subtle blue border gradient when user is staring
2. Fades in over 200ms, holds for 400ms, then fades out
3. Doesn't interfere with user interactions on other apps
4. Respects DO_NOT_DISTURB settings
5. Can be toggled on/off from the main activity
6. Uses WindowManager.LayoutParams with TYPE_APPLICATION_OVERLAY
```

### Phase 4: Battery & Performance Optimization (Ready for Agent)
**Assign to: DevOps & Optimization Agent**

```
Review and optimize the camera framework for battery efficiency:
1. Implement throttling to reduce frame processing from 30fps to 8fps
2. Add proximity sensor detection to pause tracking when screen is off
3. Implement adaptive bitrate based on device temperature
4. Profile memory usage and identify leaks
5. Add battery usage telemetry
6. Provide background task scheduling using WorkManager
```

### Phase 5: Local Analytics Database (Ready for Agent)
**Assign to: Software Architect Agent**

```
Design and implement a local SQLite database schema using Room ORM to:
1. Store hourly blink count statistics
2. Track average staring duration intervals
3. Calculate daily eye health compliance scores
4. Generate weekly/monthly reports
5. Export data for user review
6. Implement automatic data cleanup (retain 90 days of data)
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
    ┌────────────┼────────────┐
    ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌──────────┐
│ Camera  │ │MediaPipe │ │ Overlay  │
│Manager  │ │ Face Mesh│ │ View     │
└─────────┘ └──────────┘ └──────────┘
    │            │            │
    └────────────┼────────────┘
                 │
                 ▼
        ┌──────────────────┐
        │  BlinkDatabase   │
        │  (SQLite/Room)   │
        └──────────────────┘
```

## 🔧 Configuration

### Camera Settings
- **Frame Rate**: 8 fps (optimized for battery)
- **Resolution**: 480x360 (adaptive)
- **EAR Threshold (Blink)**: 0.2
- **EAR Threshold (Staring)**: 0.3
- **Staring Duration**: 5 seconds

### Notification Settings
- **Channel**: Low priority background service
- **Auto-dismiss**: No (persistent)
- **Sound/Vibration**: Disabled

## 📈 Testing Checklist

- [ ] App starts without crashing
- [ ] Camera permissions are requested and granted
- [ ] Foreground service notification appears
- [ ] Camera feed is accessible
- [ ] Eye landmarks are detected on device
- [ ] Blinks are recognized reliably
- [ ] Overlay appears when staring detected
- [ ] Blink data is saved to database
- [ ] Battery drain is minimal (< 5% per hour)
- [ ] App doesn't interfere with other apps

## 🐛 Troubleshooting

### Camera Not Accessible
- Verify CAMERA permission is granted
- Check if another app is using the camera
- Restart the app

### High Battery Drain
- Reduce frame processing rate further
- Check for memory leaks using Android Profiler
- Verify MediaPipe is running on GPU

### Overlay Not Showing
- Ensure SYSTEM_ALERT_WINDOW permission is granted
- Check device is not in Do Not Disturb mode
- Verify WindowManager LayoutParams are correct

## 📚 Resources

- [MediaPipe Documentation](https://developers.google.com/mediapipe)
- [Android Camera2 API](https://developer.android.com/reference/android/hardware/camera2/package-summary)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [Room Database](https://developer.android.com/training/data-storage/room)

## 📄 License

MIT License - feel free to use this project for personal or commercial purposes.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

For issues or questions, please open a GitHub issue or contact the development team.

---

**Built with AI-assisted development** 🤖✨