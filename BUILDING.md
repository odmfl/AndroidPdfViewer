# Building Android PDF Viewer

This document provides detailed instructions for building the Android PDF Viewer library and sample application.

## Prerequisites

### Required Software

- **JDK 11 or higher** - Java Development Kit
- **Android Studio** - Arctic Fox (2020.3.1) or later recommended
- **Android SDK** - API Level 24 or higher
- **NDK** - Version 28.1.13356709 or compatible
- **CMake** - Version 3.22.1 or higher (for native code compilation)

### Environment Setup

1. **Install Android Studio**
   - Download from https://developer.android.com/studio
   - Follow the installation wizard
   - Install the Android SDK through SDK Manager

2. **Configure NDK**
   - Open Android Studio
   - Go to Tools > SDK Manager
   - Click on SDK Tools tab
   - Check "NDK (Side by side)" and "CMake"
   - Click OK to install

3. **Set Environment Variables** (Optional but recommended)
   ```bash
   export ANDROID_HOME=/path/to/android/sdk
   export ANDROID_NDK_HOME=$ANDROID_HOME/ndk/28.1.13356709
   export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
   ```

## Project Structure

```
AndroidPdfViewer/
├── android-pdf-viewer/    # Main PDF viewer library (UI components)
├── pdfium/                # PDFium JNI bindings and native code
├── sample/                # Sample application
├── build.gradle           # Root build configuration
├── settings.gradle        # Project modules configuration
└── gradle/                # Gradle wrapper and version catalog
```

## Building from Command Line

### 1. Clone the Repository

```bash
git clone https://github.com/odmfl/AndroidPdfViewer.git
cd AndroidPdfViewer
```

### 2. Build the Library

Build all modules:
```bash
./gradlew build
```

Build specific modules:
```bash
# Build pdfium library only
./gradlew :pdfium:build

# Build android-pdf-viewer library only
./gradlew :android-pdf-viewer:build

# Build sample app
./gradlew :sample:build
```

### 3. Build APK

Debug APK:
```bash
./gradlew :sample:assembleDebug
```

The APK will be located at:
`sample/build/outputs/apk/debug/sample-debug.apk`

Release APK:
```bash
./gradlew :sample:assembleRelease
```

### 4. Install on Device

```bash
# Install debug APK
./gradlew :sample:installDebug

# Install and run
./gradlew :sample:installDebug
adb shell am start -n com.pp.sample/.MainActivity
```

## Building from Android Studio

### 1. Import Project

1. Open Android Studio
2. Click "File" > "Open"
3. Navigate to the cloned repository
4. Click "OK" to import

### 2. Sync Gradle

Android Studio should automatically sync Gradle. If not:
- Click "File" > "Sync Project with Gradle Files"

### 3. Build Project

- Click "Build" > "Make Project" (or press Ctrl+F9 / Cmd+F9)

### 4. Run Sample App

1. Connect an Android device or start an emulator
2. Select "sample" configuration from the dropdown
3. Click the "Run" button (or press Shift+F10 / Ctrl+R)

## Native Library Compilation

### PDFium Module

The `pdfium` module contains native C++ code that interfaces with the PDFium library.

#### CMake Configuration

Location: `pdfium/src/main/jni/CMakeLists.txt`

Key configurations:
- **Minimum CMake Version**: 3.22.1
- **Target Architectures**: arm64-v8a (primary), armeabi-v7a, x86, x86_64
- **Native Libraries**: 
  - `libpdfium.cr.so` - Chromium-based PDFium library
  - `pdfium` - JNI wrapper library

#### Building Native Code Only

```bash
./gradlew :pdfium:externalNativeBuildDebug
```

#### Cleaning Native Build

```bash
./gradlew :pdfium:clean
./gradlew :pdfium:cleanBuildCache
```

### Supported Architectures

By default, the library builds for all supported architectures:
- arm64-v8a (64-bit ARM)
- armeabi-v7a (32-bit ARM)
- x86 (32-bit Intel)
- x86_64 (64-bit Intel)

To build for specific architectures only, modify `pdfium/build.gradle`:

```groovy
android {
    defaultConfig {
        ndk {
            abiFilters 'arm64-v8a'  // Only build for arm64
        }
    }
}
```

## Build Variants

### Debug Build

Includes debugging symbols and is not optimized:
```bash
./gradlew :sample:assembleDebug
```

### Release Build

Optimized and requires signing configuration:
```bash
./gradlew :sample:assembleRelease
```

For release builds, configure signing in `sample/build.gradle` or use command-line options:
```bash
./gradlew :sample:assembleRelease \
  -Pandroid.injected.signing.store.file=/path/to/keystore \
  -Pandroid.injected.signing.store.password=password \
  -Pandroid.injected.signing.key.alias=alias \
  -Pandroid.injected.signing.key.password=password
```

## Testing

### Run Unit Tests

```bash
# All unit tests
./gradlew test

# Specific module
./gradlew :pdfium:test
./gradlew :android-pdf-viewer:test
```

### Run Instrumented Tests

Requires a connected device or emulator:
```bash
# All instrumented tests
./gradlew connectedAndroidTest

# Specific module
./gradlew :pdfium:connectedAndroidTest
```

## Publishing

### Build AAR Libraries

```bash
# Build release AARs
./gradlew :pdfium:assembleRelease
./gradlew :android-pdf-viewer:assembleRelease
```

AAR files will be located at:
- `pdfium/build/outputs/aar/pdfium-release.aar`
- `android-pdf-viewer/build/outputs/aar/android-pdf-viewer-release.aar`

### Publish to Maven Local

```bash
./gradlew :pdfium:publishToMavenLocal
./gradlew :android-pdf-viewer:publishToMavenLocal
```

## Troubleshooting

### Common Issues

#### 1. NDK Not Found

**Error**: `NDK is not installed`

**Solution**:
- Install NDK through Android Studio SDK Manager
- Or set `ANDROID_NDK_HOME` environment variable
- Or specify NDK path in `local.properties`:
  ```
  ndk.dir=/path/to/ndk
  ```

#### 2. CMake Version Mismatch

**Error**: `CMake version X.X.X is required`

**Solution**:
- Install required CMake version through SDK Manager
- Or update `cmake` version in `pdfium/build.gradle`:
  ```groovy
  externalNativeBuild {
      cmake {
          version '3.22.1'
      }
  }
  ```

#### 3. Out of Memory During Build

**Error**: `OutOfMemoryError`

**Solution**:
Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m
org.gradle.parallel=true
org.gradle.daemon=true
```

#### 4. Build Cache Issues

**Solution**:
```bash
# Clean build
./gradlew clean

# Clean build cache
./gradlew cleanBuildCache

# Invalidate caches in Android Studio
# File > Invalidate Caches / Restart
```

#### 5. Native Library Not Found at Runtime

**Error**: `UnsatisfiedLinkError: dlopen failed`

**Solution**:
- Ensure the correct ABI is built for your target device
- Check that `abiFilters` in build.gradle matches device architecture
- Verify native libraries are included in APK:
  ```bash
  unzip -l sample-debug.apk | grep "\.so$"
  ```

### Debug Native Code

To debug native C++ code:

1. Open Android Studio
2. Set breakpoints in C++ files
3. Select "Debug" configuration
4. Click "Debug" button
5. Native debugger will attach automatically

Or use command line:
```bash
# Build debug version with symbols
./gradlew :pdfium:assembleDebug

# Use ndk-gdb
cd pdfium
ndk-gdb --verbose
```

## Performance Optimization

### Build Performance

Enable Gradle build cache and parallel builds in `gradle.properties`:
```properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

### Native Build Performance

Use Ninja build system (faster than Make):
```groovy
externalNativeBuild {
    cmake {
        arguments "-GNinja"
    }
}
```

### APK Size Optimization

Enable APK splits for different architectures in `sample/build.gradle`:
```groovy
android {
    splits {
        abi {
            enable true
            reset()
            include 'arm64-v8a', 'armeabi-v7a'
            universalApk false
        }
    }
}
```

## CI/CD Integration

### GitHub Actions

Example workflow (`.github/workflows/android.yml`):
```yaml
name: Android CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        
    - name: Build with Gradle
      run: ./gradlew build
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Build APK
      run: ./gradlew :sample:assembleDebug
      
    - name: Upload APK
      uses: actions/upload-artifact@v4
      with:
        name: sample-apk
        path: sample/build/outputs/apk/debug/*.apk
```

## Additional Resources

- [Android NDK Documentation](https://developer.android.com/ndk/guides)
- [CMake Documentation](https://cmake.org/documentation/)
- [Gradle Build Tool](https://gradle.org/guides/)
- [PDFium Documentation](https://pdfium.googlesource.com/pdfium/)

## Getting Help

If you encounter issues:

1. Check this document for common solutions
2. Review existing [GitHub Issues](https://github.com/odmfl/AndroidPdfViewer/issues)
3. Create a new issue with:
   - Gradle version (`./gradlew --version`)
   - Android Studio version
   - NDK version
   - Error messages and stack traces
   - Steps to reproduce
