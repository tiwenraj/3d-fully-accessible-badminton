# 3D Fully Accessible Badminton

An Android-first 3D badminton training prototype built with OpenGL ES 2.0.

## Current prototype

- Perspective 3D badminton court with service lines and a centre net
- Player and opponent avatars with simplified rackets
- Shuttlecock flight physics and a hit action
- Touch controls to move around the court
- Point counter and accessible screen-reader announcements
- High-contrast court option and larger on-screen controls
- Android APK builds through GitHub Actions

## Build

The project uses Android Gradle Plugin 9.4.0 and Gradle 9.6.0.

From the repository root, run:

```bash
gradle assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Releases

Pushing a version tag such as `v0.2.0` triggers a release build and attaches the APK to a GitHub Release. The current release APK is signed with the Android debug key for testing only; configure a private release keystore before production distribution.

## Status

This is an early single-player training prototype. Full badminton rules, doubles, audio cues, and online multiplayer are not implemented yet.
