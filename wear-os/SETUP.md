# Tennis Tracker — Wear OS Setup Guide

## Prerequisites

1. **Android Studio Hedgehog** (2023.1.1) or later — install from developer.android.com
2. **google-services.json** — download from Firebase console → Project Settings → your Android app
   - Package name: `com.tennis.tracker.wear`
   - Place file at: `wear-os/app/google-services.json`
3. **Pixel Watch 4** — enable Developer Options:
   - Settings → System → About → tap Build number 7× → enable ADB debugging

## Project setup

```bash
cd wear-os
# Open in Android Studio: File → Open → select this folder
# Or build from CLI:
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## How match connection works

### Automatic (phone pushes match ID via Wearable Data Layer)
Add this snippet to `tennis_tracker.html` in the `startMatch()` function.
This requires a companion Android phone app (future Sprint 7b).

### Manual (for now — type ID on watch)
1. Start match on phone PWA as normal
2. Note the 6-char match ID shown in the URL (`?m=XXXXXX`) or tap **Share**
3. On the watch, open Tennis Tracker
4. Type the 6-char ID → tap **Connect**
5. Score away — points sync bidirectionally via Firestore

## UX: Option C — Smart Tap

| Gesture | Action |
|---------|--------|
| Tap left half | P1 wins point — 1st serve assumed |
| Tap right half | P2 wins point — 1st serve assumed |
| Long press left | Overlay appears: 2nd Serve / Double Fault for P1 |
| Long press right | Overlay appears: 2nd Serve / Double Fault for P2 |
| Tap scrim | Dismiss overlay (no point awarded) |

**Double Fault** awards the point to the OPPONENT automatically.

## Haptic patterns

| Event | Pattern |
|-------|---------|
| 1st serve point | Single short buzz (40ms) |
| 2nd serve point | Double buzz (40ms + 40ms) |
| Double fault | Long double buzz (80ms + 80ms) |

## File structure

```
wear-os/
├── app/
│   ├── google-services.json          ← YOU MUST ADD THIS
│   └── src/main/java/com/tennis/tracker/wear/
│       ├── MainActivity.kt           ← Entry, haptic, nav
│       ├── data/
│       │   ├── MatchState.kt         ← State model
│       │   ├── ScoringEngine.kt      ← Pure scoring logic
│       │   ├── FirestoreRepository.kt ← Firestore read/write
│       │   └── WearDataListenerService.kt ← Phone→Watch data layer
│       └── presentation/
│           ├── MatchViewModel.kt     ← State management
│           ├── ScoreScreen.kt        ← Main scoring UI
│           └── MatchIdScreen.kt      ← ID entry fallback
└── SETUP.md                          ← This file
```

## Next steps (Sprint 7b)

- [ ] Add `google-services.json` and build
- [ ] Test on Pixel Watch 4 emulator first: AVD Manager → Wear OS → round 454×454
- [ ] Sideload to physical watch via USB pairing
- [ ] Add match ID auto-push from phone (Wearable Data Layer on phone side)
- [ ] Apple Watch (Swift/WatchKit) — separate Xcode project, same architecture
