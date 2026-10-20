# Architecture

## Overview

Breakpoint is two clients sharing one Firebase backend. There is no custom server — all business logic runs client-side on both the phone and the watch.

```
Phone PWA (tennis_tracker.html)
        │
        │  Google Sign-In (same uid)
        │
        ▼
  Firebase Auth ────────────────────────────────────────────
        │                                                   │
        ▼                                                   ▼
  Firestore                                          Watch App (Kotlin)
  ─────────────────────────────────────────────────────────
  matches/{id}            ◄──── read/write ────►   ScoringEngine
    stateJson (full state)                          FirestoreRepository
    updatedBy (device ID)
    updatedAt (server ts)
    ownerId (auth uid)
    expiresAt (7-day TTL)

  users/{uid}             ◄──── read/write ────►   active match listener
    activeMatchId                                   (auto-connect on watch)

  completed_matches/{id}  ◄──── write only  ────   (phone writes on match end)
    full point log + stats
    deletedAt (soft delete, 7-day TTL)
  ─────────────────────────────────────────────────────────
        │
        │  Wearable Data Layer (one-way, match ID push only)
        ▼
  WearDataListenerService (watch)
  path: /match, key: matchId
```

## Sync mechanism

**Score sync uses Firestore directly — not the Wearable Data Layer.**

Both the phone and the watch independently read and write the same `matches/{id}` document in Firestore. Firestore's real-time listeners (`onSnapshot` on phone, `Flow` via `addSnapshotListener` on watch) deliver updates to whichever device did not make the write.

The Wearable Data Layer is used for exactly one purpose: the phone can push the current match ID to the watch (path `/match`, key `matchId`) so the watch can auto-join without the user typing the ID manually. After that handshake, all score data flows through Firestore.

## State serialization

The entire match state is a single JSON string (`stateJson`) stored in the Firestore document. This means:

- Atomic writes — no partial update races between individual fields
- Both phone (JS `JSON.parse/stringify`) and watch (Kotlin `FirestoreRepository.parseState/serializeState`) speak the same format
- History (full point log) is embedded in the state; no separate collection

See [docs/data-model.md](docs/data-model.md) for the full schema.

## Device ID and update loop prevention

Both clients tag writes with a `updatedBy` device ID. On receiving a Firestore snapshot, each client checks `if (data.updatedBy === DEVICE_ID) return` to avoid re-processing its own writes.

**Known limitation:** The phone generates a new `DEVICE_ID` on each page load (`Math.random()`), so this guard breaks after a page refresh. It does not cause data corruption but can cause a redundant re-render.

## Authentication flow

Both clients use Google Sign-In backed by Firebase Auth.

**Phone:**
1. `signInWithPopup` → fallback to `signInWithRedirect` if popup blocked or PWA standalone mode
2. `getRedirectResult()` called on load to complete redirect flows
3. `auth.onAuthStateChanged` drives all UI state

**Watch:**
1. Silent sign-in attempted on cold start (re-uses cached credentials)
2. Falls back to full Google Sign-In intent if no cached account
3. After sign-in: `signInWithCredential(GoogleAuthProvider.getCredential(idToken))` to Firebase

Both use the same Firebase project. Matching `uid` is what links phone and watch to the same `users/{uid}/activeMatchId`.

## Offline behaviour

| Client | Offline support |
|--------|----------------|
| Phone PWA | IndexedDB write queue (`tsa_offline` DB, `queue` store). Buffers `matches` and `completed_matches` writes. Flushes on `online` event. |
| Watch app | None. Firestore writes fail silently on network loss. (Backlog: S7-18) |

## Folder structure

```
tennis-score-analytics/
├── index.html                  Landing / home page
├── tennis_tracker.html         Full PWA scoring app (single file)
├── manifest.json               PWA manifest
├── sw.js                       Service worker (cache-first, v11)
├── icon.svg                    App icon
├── firebase.json               Firebase Hosting config
├── wear-os/                    Kotlin Wear OS app
│   ├── app/
│   │   ├── build.gradle
│   │   ├── google-services.json   ← NOT committed; download from Firebase Console
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/com/tennis/tracker/wear/
│   │       │   ├── MainActivity.kt
│   │       │   ├── complication/TennisComplicationService.kt
│   │       │   ├── data/
│   │       │   │   ├── AuthRepository.kt
│   │       │   │   ├── FirestoreRepository.kt
│   │       │   │   ├── MatchState.kt
│   │       │   │   ├── ScoringEngine.kt
│   │       │   │   └── WearDataListenerService.kt
│   │       │   └── presentation/
│   │       │       ├── MatchIdScreen.kt
│   │       │       ├── MatchViewModel.kt
│   │       │       ├── ScoreScreen.kt
│   │       │       └── SignInScreen.kt
│   │       └── res/
│   └── SETUP.md
├── docs/
│   ├── setup.md                Firebase project setup guide
│   ├── data-model.md           Firestore schema reference
│   └── firestore-rules.md      Security rules and known issues
├── ARCHITECTURE.md             This file
├── CONTRIBUTING.md
└── SPRINT_PLAN.md              Sprint history and tech debt log
```
