# Play Store Deployment Guide

This guide covers the setup and deployment process for Vision Wellness on Google Play Store.

## Prerequisites

- Google Play Developer Account ($25 one-time registration fee)
- Android keystore (.jks file)
- App signing certificate
- Play Store graphics (icons, screenshots, banners)

## Step 1: Create a Keystore

Generate a signing keystore for your app:

```bash
keytool -genkey -v -keystore vision-wellness.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias vision-wellness
```

**Important:** Save this file securely. You'll need it for all future updates.

## Step 2: Configure Local Signing

1. Copy `local.properties.example` to `local.properties`
2. Update with your keystore details:

```properties
STORE_FILE=/path/to/vision-wellness.jks
STORE_PASSWORD=your_keystore_password
KEY_ALIAS=vision-wellness
KEY_PASSWORD=your_key_password
```

⚠️ **Never commit `local.properties` to version control**

## Step 3: Update Build Configuration

Edit `app/build.gradle.kts` to add signing config:

```kotlin
android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("STORE_FILE") ?: "vision-wellness.jks")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS") ?: "vision-wellness"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## Step 4: Build Release Bundle

Generate a signed App Bundle (AAB) for Play Store:

```bash
./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Step 5: Create Play Store Listing

1. Go to [Google Play Console](https://play.google.com/console)
2. Create new app with package name: `com.visionwellness`
3. Add app details:
   - **Title:** Vision Wellness
   - **Category:** Health & Fitness
   - **Rating:** Add content rating questionnaire
   - **Privacy Policy:** Link to your privacy policy
   - **Contact Info:** Your support email

## Step 6: Prepare Store Graphics

Create and upload:

- **App Icon:** 512x512 PNG
- **Feature Graphic:** 1024x500 PNG
- **Screenshots:** Min 2, Max 8 (9:16 aspect ratio)
- **Description:** Clear, compelling description
- **Short Description:** Max 80 characters

## Step 7: Configure Content Rating

1. Fill out Google Play's content rating questionnaire
2. Save your rating

## Step 8: Set Pricing & Distribution

1. Set app as Free or Paid
2. Select countries/regions for distribution
3. Configure content guidance

## Step 9: Submit for Review

1. Upload signed AAB file
2. Review all information
3. Submit for review

Review typically takes 24-48 hours.

## Version Management

For future updates:

1. Increment `versionCode` in `app/build.gradle.kts`
2. Update `versionName` (e.g., 1.0.1)
3. Build new AAB: `./gradlew bundleRelease`
4. Upload to Play Store

## Testing Before Release

### Internal Testing Track:
```bash
./gradlew bundleRelease
# Upload to Play Store Console > Testing > Internal Testing
```

### Closed Testing Track:
```bash
# Limited group of testers (up to 100)
```

### Open Testing Track:
```bash
# Public beta testing
```

## Important Notes

- **Keystore Security:** Keep your keystore file secure and backed up
- **Password Management:** Never hardcode passwords; use environment variables
- **Signing Certificate:** Never change the signing certificate (app becomes unupdatable)
- **ProGuard Rules:** Review and update `proguard-rules.pro` for your dependencies
- **Minimum SDK:** Currently set to 24 (Android 7.0)

## Troubleshooting

**Build fails with signing error:**
- Verify `local.properties` paths are correct
- Check keystore password is accurate
- Ensure keystore file exists

**App rejected by Play Store:**
- Review Play Store policy requirements
- Check content rating guidelines
- Ensure privacy policy is accessible

**APK too large:**
- Enable `isShrinkResources = true`
- Check for unused dependencies
- Consider using App Bundle (AAB) instead of APK

## Resources

- [Google Play Console Help Center](https://support.google.com/googleplay/android-developer)
- [Android App Signing Documentation](https://developer.android.com/studio/publish/app-signing)
- [Google Play Policy Center](https://play.google.com/about/developer-content-policy/)
