# Vision Wellness Android App

A mobile application to address Computer Vision Syndrome by monitoring eye blinking habits and providing gentle reminders to blink during extended screen usage.

## 🎯 Features

- **Real-time Eye Tracking**: Uses Google MediaPipe Face Mesh for lightweight, on-device facial landmark detection
- **Blink Detection**: Calculates Eye Aspect Ratio (EAR) to detect natural blinks
- **Staring Alerts**: Triggers subtle visual feedback when user hasn't blinked for 5+ seconds
- **Background Service**: Runs continuously as a foreground service without disrupting other apps
- **Battery Optimized**: Processes frames at 8 fps to minimize power consumption
- **Adaptive Frame Rate**: Intelligently adjusts processing based on battery, temperature, and proximity
- **Local Analytics**: Tracks blink statistics using SQLite database
- **Non-Intrusive UI**: System overlay provides gentle reminders without aggressive notifications

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Computer Vision**: Google MediaPipe Face Mesh
- **Database**: SQLite with Room ORM
- **Concurrency**: Kotlin Coroutines
- **Background Work**: WorkManager
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
│   │   │   │   │   ├── OptimizedCameraFrameProcessor.kt
│   │   │   │   │   └── CameraFrameProcessor.kt
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraManager.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── OverlayView.kt
│   │   │   │   │   └── AlertOverlayManager.kt
│   │   │   │   ├── optimization/
│   │   │   │   │   ├── BatteryMonitor.kt
│   │   │   │   │   ├── SensorMonitor.kt
│   │   │   │   │   ├── AdaptiveFrameRateManager.kt
│   │   │   │   │   ├── MemoryProfiler.kt
│   │   │   │   │   ├── BatteryProfilerWorker.kt
│   │   │   │   │   └── BackgroundTaskScheduler.kt
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
   wget https://storage.googleapis.com/mediapipe-assets/face_landmarker.task
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
- `BATTERY_STATS` - Battery monitoring for optimization
- `INTERNET` - Future analytics synchronization (optional)

## 🔋 Phase 4: Battery & Performance Optimization (NEW!)

### Components

**BatteryMonitor.kt**
- Real-time battery percentage tracking
- Battery health status monitoring
- Low power mode detection
- Battery level categorization (Critical/Low/Normal/Healthy)

**SensorMonitor.kt**
- Proximity sensor monitoring (detect when face is near camera)
- Ambient temperature monitoring
- Automatic processing pause when device overheats
- Thermal throttling support

**AdaptiveFrameRateManager.kt**
- Dynamic frame rate adjustment based on:
  - Battery level (Critical: 4fps → Healthy: 10fps)
  - Device temperature (Pause at 45°C, throttle at 40°C)
  - Proximity sensor (3fps when face away)
- Re-evaluates every 10 seconds
- Maintains blink detection reliability while conserving battery

**MemoryProfiler.kt**
- Real-time memory usage monitoring
- Automatic garbage collection triggers
- Memory pressure detection (85%/95% thresholds)
- Heap size reporting and optimization

**BatteryProfilerWorker.kt**
- Background profiling of battery usage patterns
- Periodic logging to app-specific storage
- WorkManager integration for reliable background execution

**BackgroundTaskScheduler.kt**
- Schedules periodic battery profiling (every 30 minutes)
- Uses WorkManager for reliability
- Handles work cancellation gracefully

**OptimizedCameraFrameProcessor.kt**
- Integrates adaptive frame rate with camera processing
- Combines battery, thermal, and proximity optimization
- Seamless frame skip adjustment without interruption

### How It Works

```
Monitored Parameters:
├─ Battery Level (0-100%)
├─ Device Temperature (°C)
├─ Face Proximity (near/far)
├─ Memory Usage (%)
└─ Device State (charging/not charging)

                  ↓
        AdaptiveFrameRateManager
                  ↓
        ┌────────┼────────┐
        ▼        ▼        ▼
    Battery  Thermal Proximity
    Status   Status  Status
        └────────┼────────┘
                  ↓
        Target FPS Decision:
        4fps (Critical) → 10fps (Healthy)
        3fps (Away) → Pause (Too Hot)
        ↓
    OptimizedCameraFrameProcessor
        adjusts frame skip rate
        └─ maintains eye tracking
           without battery drain
```

### Optimization Profiles

**Battery Level Impact**
```
Critical (<15%)  → 4 fps   (minimal processing)
Low (15-30%)     → 5 fps   (reduced processing)
Normal (30-50%)  → 8 fps   (standard processing)
Healthy (>50%)   → 10 fps  (enhanced processing)
```

**Temperature Impact**
```
Cool (<35°C)     → Full processing (no throttle)
Normal (35-40°C) → Full processing (monitor)
High (40-45°C)   → Reduce FPS by 25%
Critical (>45°C) → Pause all processing (0 fps)
```

**Proximity Impact**
```
Face Near        → Full frame rate
Face Away        → 50% of target FPS (conserve battery)
```

### Battery Savings

**Expected Impact**
- Without optimization: ~15-20% battery/hour
- With adaptive FPS: ~5-8% battery/hour
- With all optimizations: ~3-5% battery/hour

**Real-world results vary based on:**
- Device model and CPU efficiency
- Screen brightness
- Other background processes
- Environmental temperature

## 🧠 AI Agent Workflow

### Phase 1: ✅ Core Architecture (Complete)
- Foreground Service setup
- Permission handling
- Camera framework
- Database schema

### Phase 2: ✅ Eye Detection Engine (Complete)
- MediaPipe Face Mesh integration
- Eye Aspect Ratio calculation
- Blink detection with debounce
- Staring detection
- Frame throttling (8fps)

### Phase 3: ✅ Alert Overlay System (Complete)
- System-wide overlay window
- Blue border gradient animation
- Fade-in/hold/fade-out sequence
- Non-intrusive UI

### Phase 4: ✅ Battery & Performance Optimization (Complete!)
- Battery monitoring and profiling
- Sensor integration (proximity, temperature)
- Adaptive frame rate management
- Memory profiling and optimization
- Background task scheduling

### Phase 5: ⏳ Analytics Dashboard (Ready for Agent)
**Optional enhancement: Create a statistics UI**

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
    ┌────────────┼─────────────────┐
    ▼            ▼                 ▼
┌─────────┐ ┌──────────┐ ┌──────────────────┐
│ Camera  │ │MediaPipe │ │  Optimization    │
│Manager  │ │ Face Mesh│ │  (Battery,       │
└─────────┘ └──────────┘ │   Thermal,       │
    │            │       │   Proximity)     │
    │            │       └──────────────────┘
    └────────────┼────────────┐
                 ▼            ▼
        ┌──────────────────┐  ┌──────────┐
        │ Overlay Manager  │  │ Database │
        │ (UI Alerts)      │  │ (SQLite) │
        └──────────────────┘  └──────────┘
```

## 🔧 Configuration

### Battery Thresholds
```kotlin
const val BATTERY_LEVEL_CRITICAL = 15  // Below 15%
const val BATTERY_LEVEL_LOW = 30       // Below 30%
const val BATTERY_LEVEL_NORMAL = 50    // Below 50%
```

### Temperature Thresholds
```kotlin
const val TEMPERATURE_CRITICAL = 45f  // Stop at 45°C
const val TEMPERATURE_HIGH = 40f      // Throttle at 40°C
const val TEMPERATURE_NORMAL = 35f    // Resume at 35°C
```

### Frame Rate Profiles
```kotlin
const val FPS_CRITICAL = 4       // Critical battery
const val FPS_LOW = 5            // Low battery
const val FPS_NORMAL = 8         // Normal battery
const val FPS_HIGH = 10          // Good battery
const val FPS_FACE_AWAY = 3      // Face away from camera
```

### Memory Thresholds
```kotlin
const val MEMORY_PRESSURE_HIGH = 0.85f      // 85%
const val MEMORY_PRESSURE_CRITICAL = 0.95f  // 95%
```

## 📈 Testing Checklist

- [ ] App starts without crashing
- [ ] Camera permissions are requested and granted
- [ ] Overlay permissions are requested and granted
- [ ] Battery monitoring works (check logcat)
- [ ] Sensor monitoring starts on service start
- [ ] Frame rate adapts when battery changes
- [ ] Frame rate reduces when temperature increases
- [ ] Frame rate reduces when face moves away
- [ ] Memory profiler logs periodically
- [ ] Staring alerts still trigger reliably
- [ ] Battery drain is < 5% per hour
- [ ] App handles thermal throttling gracefully
- [ ] Background profiling works (check app files)

## 📊 Monitoring & Debugging

### Check Battery Status
```bash
adb shell dumpsys battery
```

### Monitor Memory Usage
```bash
adb logcat | grep "Memory Usage"
```

### View Battery Logs
```bash
adb pull /data/data/com.example.visionwellness/files/battery_logs/
```

### Enable Verbose Logging
```bash
// In Timber initialization
Timber.plant(Timber.DebugTree())  // Already enabled in debug build
```

## 🐛 Troubleshooting

### High Battery Drain
- Check if device is too hot (throttling should activate)
- Verify proximity sensor is working
- Check device temperature in logcat
- Ensure face is being detected (blink count should increase)

### Frame Rate Not Adapting
- Verify battery level (check Android battery settings)
- Check temperature readings in logcat
- Ensure BatteryMonitor is initialized
- Check SensorMonitor is started

### Memory Issues
- Monitor memory usage in Android Profiler
- Check if garbage collection is being triggered
- Verify bitmap cleanup in frame processor
- Look for memory leaks in MediaPipe integration

## 📚 Resources

- [MediaPipe Documentation](https://developers.google.com/mediapipe)
- [Android Battery APIs](https://developer.android.com/guide/topics/health-fitness/battery)
- [Android Sensor Framework](https://developer.android.com/guide/topics/sensors)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Memory Profiling](https://developer.android.com/studio/profile/memory-profiler)

## 📄 License

MIT License - feel free to use this project for personal or commercial purposes.

## 👥 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

For issues or questions, please open a GitHub issue or contact the development team.

---

**Built with AI-assisted development** 🤖✨

**Current Status**: Phase 4 (Battery & Performance Optimization) ✅ COMPLETE
