# Breakpoint — Tennis Score Tracker

A real-time tennis scoring system with a phone PWA and a Wear OS watch app that sync bidirectionally via Firebase.

**Live app:** [tennis-score-analytics.web.app](https://tennis-score-analytics.web.app)

---

## What it does

- Score a tennis match from your **phone** or your **wrist** — both stay in sync in real time
- Full point-by-point log with serve type, unforced errors, and timestamps
- Post-match stats: 1st serve %, break points, momentum, unforced error breakdown
- Spectator mode — share a URL, anyone can watch live
- Match archive with search and export (PDF / CSV)
- Installable as a PWA on Android and iOS

---

## Components

| Component | Stack | Location |
|-----------|-------|----------|
| Phone scorer / spectator app | Vanilla HTML/JS + Firebase | `tennis_tracker.html` |
| Wear OS watch app | Kotlin + Jetpack Compose | `wear-os/` |
| Backend / sync | Firebase Firestore + Auth | Cloud |
| Hosting | Firebase Hosting | `tennis-score-analytics.web.app` |

---

## Quick start

### Phone (PWA)

No build step required.

1. Open [tennis-score-analytics.web.app](https://tennis-score-analytics.web.app) in Chrome on Android (or any browser)
2. Sign in with Google
3. Tap **New Match**, configure players and format, tap **Start**
4. Tap the player name to award them a point

To install as an app: tap the browser menu → **Add to Home Screen**.

### Watch app

See [wear-os/SETUP.md](wear-os/SETUP.md) for full build and sideload instructions.

Requires:
- Android Studio Hedgehog or later
- `wear-os/app/google-services.json` downloaded from Firebase Console
- Wear OS 3.0+ watch (minSdk 30)

```bash
cd wear-os
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## How scoring works

1. Sign in on both phone and watch with the same Google account
2. Start a match on the phone — a 6-char match ID is generated
3. The watch auto-connects via the active match ID stored in Firestore
4. Score from either device — points sync instantly via Firestore
5. On match end, the full point log is saved to the archive

---

## Documentation

| Doc | Contents |
|-----|----------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | Data flow, sync mechanism, Firestore schema |
| [docs/setup.md](docs/setup.md) | Firebase project setup, Firestore rules, TTL policies |
| [docs/data-model.md](docs/data-model.md) | Full `stateJson` schema and Firestore collections |
| [docs/firestore-rules.md](docs/firestore-rules.md) | Security rules, known issues, planned improvements |
| [wear-os/SETUP.md](wear-os/SETUP.md) | Watch app build, sideload, ADB setup |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to work on the PWA and watch app |
| [SPRINT_PLAN.md](SPRINT_PLAN.md) | Sprint history and known tech debt |

---

## Tech stack

**PWA:**
- Vanilla HTML/CSS/JS (no build step)
- Firebase JS SDK v10.12.0 (Auth, Firestore)
- Service worker v11 (cache-first, installable)
- IndexedDB offline queue

**Watch:**
- Kotlin + Jetpack Compose for Wear OS
- Compose BOM 2024.02.00 / Wear Compose Material 1.3.0
- Firebase Firestore + Auth (Kotlin KTX)
- Kotlinx Coroutines 1.7.3
- Wear OS complication (watch face integration)
