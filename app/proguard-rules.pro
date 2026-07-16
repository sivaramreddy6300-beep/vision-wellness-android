# MediaPipe rules
-keep class com.google.mediapipe.** { *; }
-keep interface com.google.mediapipe.** { *; }

# Room rules
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }

# Timber rules
-dontwarn timber.**
-keep class timber.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    *(...);
}

# General
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile