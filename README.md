# InkNest

An offline, reading-first Android launcher by [2techy](https://2techy.dev).

## Current status

Phase 0 cleanup is complete. Phase A / M1 adds the Compose bootstrap screen
showing InkNest and the application version. KOReader integration and Android
Home launcher support will follow in later milestones.

## Build

Open the project in Android Studio and use its bundled JDK. The project uses
the checked-in Gradle wrapper, AGP 9.5.0, SDK 37 and a Gradle daemon JVM 25.
Kotlin support is built into AGP; the Compose compiler plugin uses Kotlin 2.2.10.
Minimum Android version: API 26 (Android 8.0).

On Windows PowerShell, if Java is not already configured:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
Install it using Android Studio or `adb install -r` and open InkNest from the
device's app drawer.

## Devices and validation

- Primary: iReader Tango 2 (E-Ink).
- Secondary: Samsung Galaxy S25 Ultra.

M1 acceptance requires launching, closing and reopening the app on both devices.
A successful build alone does not establish device compatibility.

M1 validation on 2026-11-02:

- Debug build and Android lint passed (0 errors, 7 warnings).
- Galaxy S25 Ultra (SM-S938B, Android 17): installation, version display,
  Back/reopen, and process restart verified.
- iReader Tango 2: device validation pending.

Lint warnings currently include dependency update notices, an obsolete
transitive lifecycle lint check, and Android 12+ backup configuration guidance.

## Development

One `:app` module, Jetpack Compose, and manual dependency wiring. Persistent
storage and source adapters are introduced only when a feature needs them.
Generated files, IDE settings and machine-specific configuration are ignored.
