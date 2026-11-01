# 3D Fully Accessible Football

An Android-first football game prototype built with OpenGL ES 2.0.

## Current prototype

- A perspective 3D football pitch rendered on the device
- A controllable blue player, an opponent, and a football
- Touch controls to move and shoot
- A goal counter and simple ball physics
- Accessibility settings for a high-contrast pitch and larger controls
- Android APK builds through GitHub Actions

## Build

The project uses Android Gradle Plugin 9.4.0 and Gradle 9.6.0.

From the repository root, run:

```bash
gradle assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Releases

Pushing a version tag such as `v0.1.0` triggers a release build and attaches the APK to a GitHub Release.

The current release APK is signed with the Android debug key for testing only. Before distributing a production app, configure a private release keystore using GitHub Actions secrets.

## Roadmap

1. ✅ Initial 3D pitch and local training prototype
2. ⬜ Improve player animation, camera, and match rules
3. ⬜ Add accessible menus, audio cues, and adjustable control layouts
4. ⬜ Add online multiplayer and matchmaking
5. ⬜ Set up production app signing and store-ready releases

## Project status

Early development. Online multiplayer is not implemented yet.
