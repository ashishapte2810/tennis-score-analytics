# Contributing

## Working on the PWA

The phone app is a single HTML file with no build step.

```bash
# Edit directly
open tennis_tracker.html

# Or serve locally with any static server:
npx serve .
# then open http://localhost:3000/tennis_tracker.html
```

To deploy after changes:

```bash
firebase deploy --only hosting
```

The service worker caches aggressively. After deploying, increment the cache name in `sw.js`:

```js
const CACHE_NAME = 'court-tracker-v12';  // bump this number
```

## Working on the watch app

See [wear-os/SETUP.md](wear-os/SETUP.md) for full environment setup.

Short version:

```bash
cd wear-os
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

You need `wear-os/app/google-services.json` from Firebase Console → Project Settings → Android app (`com.tennis.tracker.wear`). This file is gitignored and must be added locally.

## Key files to know

| File | What it does |
|------|-------------|
| `tennis_tracker.html` | Entire PWA — auth, scoring, stats, archive, offline queue |
| `sw.js` | Service worker — cache strategy and version |
| `wear-os/app/src/main/java/.../data/ScoringEngine.kt` | Tennis scoring logic (pure functions, no side effects) |
| `wear-os/app/src/main/java/.../data/FirestoreRepository.kt` | Firestore read/write and state serialization |
| `wear-os/app/src/main/java/.../presentation/MatchViewModel.kt` | State management for the watch UI |
| `wear-os/app/src/main/java/.../presentation/ScoreScreen.kt` | Main scoring UI (Compose) |

## Scoring logic

The scoring engine is duplicated between the phone (JS functions in `tennis_tracker.html`) and the watch (`ScoringEngine.kt`). Both must be kept in sync when scoring rules change. The canonical reference for edge cases is the point log in `MatchState.kt`.

Tennis rules implemented:
- Standard game scoring (15 / 30 / 40 / Deuce / Advantage)
- Tiebreak (first to 7, win by 2)
- Super tiebreak (first to 10, win by 2) — used as deciding set format
- Advantage set (no tiebreak in final set)
- Set and match win detection
- Serve rotation (tiebreak: alternates every 2 points; super TB: every 2)

## Firestore schema

Any change to the `stateJson` format must be backward-compatible — existing in-progress matches in Firestore use the old format. Add new optional fields with a default when missing; never remove or rename fields.

See [docs/data-model.md](docs/data-model.md).

## Known tech debt

See the Tech Debt section of [SPRINT_PLAN.md](SPRINT_PLAN.md) for a full list. Key items:

- **TD-8:** No-ad scoring bug — `scoreNormal()` doesn't check the `noAd` flag at deuce
- **TD-7:** Cross-device archive sync broken (missing composite Firestore index + `ownerId` write timing)
- **S8-2:** Firestore security rules are intentionally loose pending fix for watch `ownerId` omission (S17-8)

## Deployment checklist

### PWA
- [ ] Test scoring flow end-to-end in browser
- [ ] Verify offline queue works (DevTools → Network → Offline)
- [ ] Bump service worker cache version in `sw.js`
- [ ] `firebase deploy --only hosting`

### Watch app
- [ ] Build debug APK: `./gradlew assembleDebug`
- [ ] Test on Wear OS emulator (AVD: round 454×454)
- [ ] Sideload to physical watch: `adb install app-debug.apk`
- [ ] Verify Firestore sync with phone side-by-side
