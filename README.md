# 3D Fully Accessible Badminton

An Android-first 3D badminton rally game built with OpenGL ES 2.0.

## Playable prototype

- 3D badminton court, net, player, opponent, and shuttlecock
- Touch-and-hold movement controls plus tap-to-move support
- HIT button to serve and return an incoming shuttle
- Opponent movement and basic return/miss logic
- Point counter with spoken accessibility announcement
- Original synthesized background music with an on-screen Music ON/OFF toggle
- Racket-hit and point-won sound effects
- High-contrast court option and larger on-screen controls
- Android APK builds through GitHub Actions

## Controls

- LEFT / RIGHT: move across the court
- FORWARD / BACK: move toward or away from the net
- HIT: serve, then strike the shuttle back when it comes within reach
- Music: turn background music on or off
- Accessibility: enable high contrast or larger controls

## Build

The project uses Android Gradle Plugin 9.4.0 and Gradle 9.6.0.

From the repository root, run:

```bash
gradle assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Releases

Pushing to `main` triggers APK builds. A version tag such as `v0.2.0` also triggers a release build and attaches the APK to a GitHub Release. The release APK currently uses the Android debug key for testing only; configure a private release keystore before production distribution.

## Status

This is an early playable single-player badminton rally prototype. The opponent uses basic automated movement and return logic. Online multiplayer, doubles, full official scoring/rules, and production signing are not implemented yet.
