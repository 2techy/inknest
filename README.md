# InkNest

An offline, reading-first Android launcher by [2techy](https://2techy.dev).

## Build

Open the project in Android Studio and use its bundled JDK. Dependency versions
are managed in gradle/libs.versions.toml and the Gradle wrapper configuration.

On Windows PowerShell, if Java is not already configured:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

Debug APK: app/build/outputs/apk/debug/app-debug.apk.
Install using Android Studio or adb install -r.

## Devices

- Primary: iReader Tango 2 (E-Ink).
- Secondary: Samsung Galaxy S25 Ultra.

## Development

One :app module, Jetpack Compose, and manual dependency wiring. Persistent
storage and source adapters are introduced only when a feature needs them.
Generated files, IDE settings and machine-specific configuration are ignored.
