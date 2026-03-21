# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JMusicPlayer is a Kotlin-based Android music player app for playing local music files. Package name: `com.wyj.voice`.

- Min SDK 21 / Target SDK 32 / Compile SDK 32
- Kotlin 1.7.20, AGP 7.3.1, Java 1.8 compatibility
- Data Binding and View Binding enabled

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build signed release APK (uses JMusicPlayerKey.jks)
./gradlew test                 # Unit tests
./gradlew connectedAndroidTest # Instrumented tests on device/emulator
```

## Architecture

**MVVM pattern with Service-based background playback.**

### Player Module (`player/`)
- `IPlayback` — interface defining all playback operations and UI callback contract
- `Player` — singleton MediaPlayer-based implementation, manages playlist and play mode
- `PlaybackService` — foreground Service for background playback with notification controls (custom RemoteViews). Exposes LocalBinder for Activity binding
- `PlayList` — manages song list, current index, and mode-aware next/previous logic
- `PlayMode` — enum: LOOP, LIST, SHUFFLE, SINGLE

### Data Layer (`model/`)
- `SongRepository` — queries local music via MediaStore ContentProvider. Supports both RxJava Observable and Kotlin Flow APIs
- `Song` / `Folder` — data classes

### ViewModel Layer (`viewModel/`)
- `LocalMusicViewModel` — loads local music using CursorLoader + Flow, emits via LiveData
- `MusicPlayerViewModel` — manages PlaybackService binding lifecycle, play mode preferences

### UI Layer (`ui/`)
- `MainActivity` — launcher, music browser with bottom player bar
- `MusicPlayerActivity` — full-screen player with album art, seekbar, gradient background (singleTask launch mode)
- `TrampolineActivity` — Android 12+ notification click trampoline
- Custom views: `MusicPlayerBar` (bottom control bar), `SwipeBackLayout` (swipe-to-close), `TitleBar`

### Key Patterns
- Player is a singleton accessed through PlaybackService's LocalBinder
- PlaybackService notification uses 4 custom intent actions for play/pause/next/previous/close
- PreferenceManager wraps SharedPreferences for persisting play mode
- ActivityManager singleton tracks activity lifecycle and stack

## Dependencies

- **Async:** RxJava 2 + Kotlin Coroutines (lifecycle-runtime-ktx)
- **Image loading:** Glide 4.15 with kapt annotation processor + wasabeef transformations (blur)
- **Media:** AndroidX Media library for media session support
- **Utilities:** BlankJ UtilCodeX
- **UI:** Material Components, ConstraintLayout

## Permissions

READ/WRITE_EXTERNAL_STORAGE, FOREGROUND_SERVICE, INTERNET
