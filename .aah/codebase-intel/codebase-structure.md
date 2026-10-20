# Codebase Structure

## Directory Layout

```
tennis-score-analytics/
├── index.html                          # Landing page / home
├── tennis_tracker.html                 # Full PWA scoring app (single-file SPA, ~105 KB)
├── manifest.json                       # PWA manifest (name, icons, display mode)
├── sw.js                               # Service worker — cache-first strategy, v11
├── icon.svg                            # App icon source
├── firebase.json                       # Firebase Hosting config + rewrite rules
├── .firebaserc                         # Firebase project binding
├── .gitignore                          # Root + wear-os artifact exclusions
├── README.md                           # Project overview and quick start
├── ARCHITECTURE.md                     # Data flow, sync mechanism, folder structure
├── CONTRIBUTING.md                     # Dev workflow, deployment checklists
├── SPRINT_PLAN.md                      # Sprint history and tech debt log
├── docs/
│   ├── setup.md                        # Firebase project setup guide
│   ├── data-model.md                   # Full stateJson schema reference
│   └── firestore-rules.md             # Security rules and known issues
└── wear-os/                            # Kotlin Wear OS app (Android Studio project)
    ├── build.gradle                    # Root Gradle build
    ├── settings.gradle                 # Project settings
    ├── gradle.properties               # Gradle JVM args
    ├── gradlew / gradlew.bat           # Gradle wrapper scripts
    ├── gradle/wrapper/                 # Gradle wrapper JAR + properties
    ├── SETUP.md                        # Watch app build and sideload guide
    └── app/
        ├── build.gradle                # App-level dependencies and build config
        ├── google-services.json        # GITIGNORED — download from Firebase Console
        └── src/main/
            ├── AndroidManifest.xml     # Permissions, services, activities
            ├── java/com/tennis/tracker/wear/
            │   ├── MainActivity.kt     # Entry point, navigation, haptic feedback
            │   ├── complication/
            │   │   └── TennisComplicationService.kt  # Watch face complication
            │   ├── data/
            │   │   ├── MatchState.kt               # State data model (Kotlin data class)
            │   │   ├── ScoringEngine.kt             # Pure tennis scoring logic
            │   │   ├── FirestoreRepository.kt       # Firestore read/write + serialization
            │   │   ├── AuthRepository.kt            # Google Sign-In + Firebase Auth
            │   │   └── WearDataListenerService.kt   # Receives match ID from phone
            │   └── presentation/
            │       ├── MatchViewModel.kt            # ViewModel, coroutine flows, state
            │       ├── ScoreScreen.kt               # Main scoring UI (Jetpack Compose)
            │       ├── SignInScreen.kt              # Google Sign-In screen (Compose)
            │       └── MatchIdScreen.kt             # Manual match ID entry (Compose)
            └── res/
                ├── drawable/                        # App icons, complication icon
                ├── mipmap-*/                        # Launcher icons (all densities)
                └── values/strings.xml               # App strings
```

## Entry Points

| File | Role | Description |
|------|------|-------------|
| `tennis_tracker.html` | Phone PWA entry | Single-file app; Firebase SDK loaded from CDN; `DOMContentLoaded` bootstraps auth and routing |
| `index.html` | Landing page | Static home/redirect page served by Firebase Hosting |
| `wear-os/app/src/main/java/.../MainActivity.kt` | Watch app entry | Android Activity; initializes Compose navigation, requests ambient mode, sets up haptic |
| `sw.js` | Service worker | Registers cache, intercepts fetch, serves cached PWA assets; cache name `court-tracker-v11` |

## API Routes

| Route / Interface | Method | Handler | Description |
|-------------------|--------|---------|-------------|
| `?match=ID` (URL param) | GET | PWA query param parser | Joins match in spectator / scorer mode |
| `?m=ID` (URL param) | GET | PWA query param parser | Short-form match join URL (shared links) |
| `matches/{id}` | Firestore onSnapshot | `listenToMatch()` in PWA | Real-time score updates |
| `matches/{id}` | Firestore set | `saveToFirestore()` in PWA / `saveState()` in watch | Write updated state |
| `users/{uid}/activeMatchId` | Firestore onSnapshot | `watchActiveMatch()` Flow in watch | Watch auto-connect |
| `/match` (Wearable Data Layer) | DataItem put | `WearDataListenerService` on watch | Receive match ID from phone |
| `ShortTextComplicationData` | Complication update | `TennisComplicationService` | Watch face score display |

## Module Organization

The codebase uses two distinct organizational styles:

- **PWA:** Single-file monolith. All auth, scoring, stats, archive, offline queue, and UI live in `tennis_tracker.html`. No build step, no bundler, no imports.
- **Watch app:** Layered by concern. `data/` contains pure logic and I/O; `presentation/` contains Compose UI screens; `complication/` is standalone. Clean separation with no circular dependencies.
