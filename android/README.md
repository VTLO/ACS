# VTLOphp

An offline Android app containing every entry of [`backend/laravel.php`](../backend/laravel.php):
Artisan commands, validation rules, migration column types and modifiers, collection methods,
HTTP test assertions and Valet commands.

The PHP cheatsheet is copied into the APK at build time and parsed on the device, so editing
`backend/laravel.php` and rebuilding is all it takes to update the app.

## Features

- Sections from the cheatsheet banners, grouped by their `// HEADING` comments
- Search across every entry (all words must match), plus a filter inside each section
- Tap an entry to copy it; long-press to copy with its description, share it, or open the
  matching Laravel docs page (validation rules and collection methods link to their own anchor)
- Light and dark theme; works offline, needs no permissions

## Build

Requires JDK 17+ and the Android SDK (platform 34, build-tools 34).

```sh
cd android
echo "sdk.dir=/path/to/android-sdk" > local.properties   # or set ANDROID_HOME
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. CI builds the same APK and
uploads it as the `VTLOphp-apk` workflow artifact.
