# Codebase Learning Document

**Project**: tennis-score-analytics (Breakpoint)
**Profiled**: 2026-10-20
**Primary Language**: Kotlin (watch) + Vanilla JS (PWA)
**Codebase Size**: ~107 KB source, 13 source files (excluding docs and build artifacts)

---

## 1. System Purpose and Context

Breakpoint is a personal tennis scoring tool for tracking live match points from a phone and a Wear OS watch simultaneously. The scorer taps to award points; the system handles all tennis scoring rules automatically and syncs state in real time between devices. A spectator can join via a shared URL and watch live without a scorer UI. After the match, a full point-by-point analytics view is available with serve stats, momentum, unforced errors, and a PDF/CSV export.

**Users:** Personal use (the app owner and their regular hitting partners). Not a multi-tenant SaaS.

---

## 2. Technology Stack

| Category | Technology | Version (if known) | Notes |
|----------|------------|-------------------|-------|
| Language (PWA) | Vanilla HTML/JS | ES2020+ | No build step — deployed raw |
| Language (Watch) | Kotlin | Project default | Jetpack Compose for Wear OS |
| Auth | Firebase Auth | JS SDK 10.12.0 / BOM 32.7.2 | Google Sign-In only |
| Database | Firebase Firestore | Same versions | Real-time sync, no custom server |
| Hosting | Firebase Hosting | — | `tennis-score-analytics.web.app` |
| Watch UI | Jetpack Compose for Wear | BOM 2024.02.00 | Wear Compose Material |
| Concurrency | Kotlinx Coroutines | 1.7.3 | Flows for Firestore listeners |
| Watch Platform | Wear OS 3.0+ | minSdk 30 | Pixel Watch 4 primary target |

---

## 3. Architecture Overview

**Architectural style:** Two independent clients (PWA + native watch app) sharing a single Firebase backend. No custom server. All business logic runs client-side.

**Sync pattern:** Firestore direct — both clients independently read and write the same `matches/{id}` document. Each client runs a real-time listener and skips updates it generated itself (via `updatedBy` device ID check). The Wearable Data Layer is used only for a one-time match ID handshake from phone to watch at match start.

**State shape:** The entire match state (scores, game/set history, full point log) is a single JSON blob `stateJson` stored atomically in one Firestore document. Writes are whole-document replaces, not field-level merges — avoiding partial update races.

**Key design decisions:**
- Single-file PWA (no build step) — lowest possible deployment friction
- Firestore direct (not Wearable Data Layer for scores) — simpler, works when phone and watch are on different networks
- Embedded point log in stateJson — self-contained documents, no secondary queries for analytics
- IndexedDB offline queue on phone only (watch offline resilience is backlog)

---

## 4. Codebase Structure

| Directory | Purpose | Key Files |
|-----------|---------|-----------|
| `/` (root) | PWA files + docs | `tennis_tracker.html`, `sw.js`, `manifest.json` |
| `docs/` | Developer documentation | `setup.md`, `data-model.md`, `firestore-rules.md` |
| `wear-os/app/src/main/java/.../data/` | Watch business logic | `ScoringEngine.kt`, `FirestoreRepository.kt`, `MatchState.kt` |
| `wear-os/app/src/main/java/.../presentation/` | Watch UI | `ScoreScreen.kt`, `MatchViewModel.kt`, `SignInScreen.kt` |
| `wear-os/app/src/main/java/.../complication/` | Watch face integration | `TennisComplicationService.kt` |

---

## 5. Data Architecture

**Primary store:** Firebase Firestore (3 collections: `matches`, `completed_matches`, `users`).

**State serialization:** `MatchState` is serialized to/from JSON by `FirestoreRepository.serializeState()` / `parseState()` on the watch and by `JSON.parse/stringify` in the PWA. Both must speak the same schema — there is no shared type definition, only an implicit contract documented in `docs/data-model.md`.

**Offline:** PWA uses an IndexedDB queue (`tsa_offline` DB, `queue` store) to buffer Firestore writes during network loss. The queue flushes on the browser `online` event. The watch has no offline resilience.

**TTL:** `matches.expiresAt` (7 days) and `completed_matches.deletedAt` (7-day soft delete) are the cleanup fields. TTL policies must be manually enabled in Firebase Console / Google Cloud Console.

---

## 6. API Surface

| Endpoint/Interface | Method | Purpose |
|-------------------|--------|---------|
| `?match=ID` / `?m=ID` URL param | Read | Join a match as scorer or spectator |
| `matches/{id}` Firestore | onSnapshot | Receive real-time score updates |
| `matches/{id}` Firestore | set | Write point scored |
| `completed_matches/{id}` Firestore | set | Archive on match end |
| `users/{uid}/activeMatchId` Firestore | onSnapshot | Watch auto-connect to active match |
| Wearable Data Layer `/match` | DataItem | Receive match ID from phone |
| `ShortTextComplicationData` | Complication update | Show live score on watch face |

---

## 7. Key Abstractions

| Concept | Implementation | Files |
|---------|---------------|-------|
| Match state | `MatchState` data class (Kotlin) / plain JS object (PWA) | `MatchState.kt`, `tennis_tracker.html` |
| Scoring engine | Pure functions, no I/O: `awardPoint`, `scoreNormal`, `scoreTB`, `winGame`, `winSet` | `ScoringEngine.kt`, JS functions in PWA |
| State serialization | `serializeState()` / `parseState()` — both sides must agree | `FirestoreRepository.kt`, PWA JSON inline |
| Device ID guard | `updatedBy` field prevents re-processing own Firestore writes | Both clients |
| Active match pointer | `users/{uid}/activeMatchId` — watch listens to this for auto-connect | `FirestoreRepository.kt`, PWA `setActiveMatch()` |
| Offline queue | IndexedDB `tsa_offline.queue` — flushes on `online` event | PWA only |
| Complication | `ShortTextComplicationData` — shows game + point score on watch face | `TennisComplicationService.kt` |

---

## 8. Dependency Analysis

**High-impact modules (change carefully):**
- `stateJson` schema — any change must be backward-compatible; existing in-progress Firestore docs use the current format
- `FirestoreRepository.kt` — single point of Firestore I/O on the watch; changes here affect all sync
- `MatchViewModel.kt` — core state hub for the watch; 3 screens depend on it

**Well-isolated modules (safe to modify):**
- `ScoringEngine.kt` — pure functions, no external dependencies; easy to test independently
- `TennisComplicationService.kt` — completely independent; reads Firestore directly
- `WearDataListenerService.kt` — background service; receives match ID, fires once

**No circular dependencies** detected in the watch app. The PWA has no module structure.

---

## 9. Testing Landscape

- **Test framework:** None. No test files exist in either the PWA or the watch app.
- **Test coverage:** Zero.
- **Highest-value test targets (if tests are added):**
  - `ScoringEngine.kt` — pure functions, highly testable, critical correctness
  - JS scoring logic in PWA — same reason
  - `FirestoreRepository.parseState` / `serializeState` — schema compatibility

---

## 10. Build and Deployment

**PWA:**
- No build step. `tennis_tracker.html` is deployed as-is.
- Deploy: `firebase deploy --only hosting`
- Service worker cache version must be incremented (`CACHE_NAME` in `sw.js`) on each deploy to bust client cache.

**Watch app:**
- Build: `cd wear-os && ./gradlew assembleDebug`
- Sideload: `adb install app/build/outputs/apk/debug/app-debug.apk`
- Requires `wear-os/app/google-services.json` (gitignored — download from Firebase Console)
- Release builds: `minifyEnabled false` — no obfuscation; ProGuard configured but disabled

---

## 11. Technical Debt and Risks

| Area | Observation | Impact | Confidence |
|------|-------------|--------|------------|
| No-ad scoring (TD-8) | `scoreNormal()` doesn't check `noAd` flag at deuce — game goes to Advantage instead of sudden-death | Wrong score in no-ad matches | High |
| Cross-device archive (TD-7) | `ownerId` may be null on first write if auth not resolved; missing composite Firestore index; `syncArchiveFromFirestore` race | Archive empty on new device | High |
| Firestore security rules (S8-2) | Rules are intentionally loose (`auth != null`) — any signed-in user can write any match | Security gap until S17-8 (watch `ownerId` fix) | High |
| Watch `ownerId` omission (S17-8) | Watch `saveState()` doesn't include `ownerId` in payload — blocks owner-only rules | Blocks security hardening | High |
| Device ID not persisted (phone) | `DEVICE_ID` re-randomized on page reload — self-update guard breaks after refresh | Cosmetic re-render only | Medium |
| Watch offline (S7-18) | No Room DB queue on watch — Firestore writes fail silently on network loss | Lost points if network drops mid-match | Medium |
| Deprecated APIs (watch) | `AmbientModeSupport`, `Vibrator` (non-Manager) deprecated on API 31+ | Build warnings; will need migration | Low |
| No tests | Zero test coverage on either client | Scoring bugs can ship undetected | High |
| Firestore TTL policies | Require manual Firebase Console setup — not in code | Storage bloat if not configured | Low |

---

## 12. Recommendations for Modification

**Where to start (entry points for understanding):**
1. Read `tennis_tracker.html` from the `startMatch()` function — it's the canonical scoring flow
2. Read `ScoringEngine.kt` + `MatchState.kt` — the scoring rules are clean and well-separated on the watch side
3. Read `FirestoreRepository.serializeState()` / `parseState()` — this is the cross-client contract

**What to be careful about:**
- **`stateJson` schema** — any change must parse cleanly from existing Firestore documents; add optional fields with defaults, never rename/remove
- **Scoring logic duplication** — the JS scoring functions in the PWA and `ScoringEngine.kt` must stay in sync; a rule change needs two implementations
- **Service worker cache version** — always increment `CACHE_NAME` in `sw.js` when deploying PWA changes, or users will see stale code

**What to test after changes:**
- Full scoring flow: start match → score points → tiebreak → set → match end → archive
- Cross-device sync: score on watch, verify phone updates within 2 seconds (and vice versa)
- Spectator mode: join via URL, verify no scoring UI shown and score updates live
- Offline: DevTools → Network → Offline, score a point, reconnect, verify sync

**Potential pitfalls:**
- Firestore `stateJson` is written as a complete replace — concurrent writes from phone and watch within the same ~100ms window can cause one to overwrite the other (last-write-wins)
- The watch `ownerId` bug means strict Firestore security rules cannot be enabled yet — see `docs/firestore-rules.md`
- Firebase free tier Firestore write limits (20k/day) could be hit in high-frequency testing sessions
