# Discuss PRD — tennis-score-analytics (Breakpoint Video Feature)

## Problem Statement

You are an Android tennis player who records your own matches for review and improvement. Today, scoring and video are completely disconnected — you score on your phone or watch, and separately manage video recordings with no relationship to the points played. You want to close that gap: since every point is already timestamped in the Breakpoint scorer, the video should automatically know where each point starts and ends, so you can relive the match, share highlights, and understand your game without manual editing work.

## Solution

A new native Android phone app (`android-app/` Kotlin + Compose, alongside the existing Wear OS module) that integrates match video with the existing Breakpoint scoring system via shared Firebase infrastructure.

The **match setup and start flow** is redesigned for recorded matches: the user enters player names, format, and no-ad on the phone, opens CameraX live preview to frame the court, mounts the phone on a tripod, then walks away. The Wear OS watch streams a live MJPEG camera preview over local WiFi (Ktor server on phone) so the user can check framing from the court. After the toss, the user confirms who's serving on the watch and taps Start — this sends a WDL message to the phone, which simultaneously activates the match in Firestore and begins recording.

**Video sync** uses the shared device clock: `recordingStartMillis` is captured at the exact moment of the Start signal; pointLog timestamps switch to `Date.now()` (client-side) to eliminate Firestore server-timestamp latency. Clip boundaries are pure arithmetic — no manual sync gesture needed, with an optional ±N second offset slider as escape hatch.

**Raw video** is kept in app-private storage for 48 hours via a WorkManager TTL job, then auto-deleted. Processed clips are exported permanently to the MediaStore gallery.

**Post-match**, a Foreground Service (the same one that kept recording alive through phone locks and app switches) pivots to processing mode, showing a progress notification as it trims and overlays each selected clip. The **highlight browser** organizes points as a Set → Game → Point expandable timeline, with filter chips for Aces, Double faults, Break points, and Tiebreak points. Each clip preview shows the score in a Grand Slam broadcast-style top bar: abbreviated names, set score boxes, game score, and a ball/racket serving indicator. Users can save individual clips to the gallery and/or merge selected clips into a single highlight reel, both shareable via the Android share sheet.

**Match analytics** — rally length (avg + distribution), serve win % by player, break point conversion, momentum graph — are computed on demand from `pointLog` (no video required) and displayed in a dedicated Analytics tab in the phone app. A lightweight summary (serve %, rally avg) is also added to the existing PWA end screen.

## User Stories

1. As a player, I want to mount my phone on a tripod and check the camera framing from my wrist, so I don't have to walk back to the baseline to adjust.
2. As a player, I want to confirm who's serving and start the match from my watch after the coin toss, so the recording and scoring begin at the exact same moment.
3. As a player, I want each point in the match to become a trimmable video clip automatically, so I don't spend time manually editing footage.
4. As a player, I want Grand Slam-style score overlays burned into each clip, so my highlights look professional when I share them.
5. As a player, I want to browse my highlights by set and game, not scroll through 160 points, so I can find the moments that mattered.
6. As a player, I want to export a single merged highlight reel as well as individual clips, so I can share one video or a specific point.
7. As a player, I want my serve win %, rally length, and momentum chart after every match, so I can track patterns in my game.
8. As a player, I want a quick post-match summary on the scorer screen I already use, so I don't have to open a second app for basic stats.

---

## Overview & Intent

| Field | Value |
|-------|-------|
| Project | `tennis-score-analytics` (Breakpoint) |
| Complexity tier | `mvp` |
| Business domain(s) | _None selected (consumer sports app — no harness domain brief applies)_ |
| Technical archetype(s) | _None selected (Firebase + Android — no harness playbook applies)_ |
| Discovery started | 2026-10-21 |
| Constraint gate | ✅ PASS |
| Iteration | 1 |

---

## Scope

**In scope:**
- Native Android phone app (`android-app/` — separate Gradle project alongside `wear-os/`)
- CameraX video recording with Foreground Service (survives phone lock / app switching)
- Match setup flow: phone handles names/format, watch confirms serve + starts match via WDL
- MJPEG live camera preview streamed from phone to watch over local WiFi (Ktor server)
- Video–score sync via shared device clock (`recordingStartMillis` + client-side `Date.now()` pointLog timestamps)
- Raw video 48-hour TTL in app-private storage (WorkManager auto-delete)
- Per-point clip trimming and Grand Slam-style score overlay via `ffmpeg-kit-android`
- Highlight browser: Set → Game → Point timeline, filter chips, inline preview
- Export: individual clips to MediaStore gallery + optional merged highlight reel
- Match analytics tab: rally length, serve win %, break point conversion, momentum graph
- Lightweight analytics summary added to existing PWA end screen

**Deferred (not in scope for this iteration):**
- Ball tracking / court detection / shot classification (Tier 3 — future phase)
- Cloud video processing (revisit when Tier 3 ML processing is added)
- iOS / SwingVision parity
- Multi-tenant or SaaS use cases
- Watch offline resilience (existing backlog item S7-18)

---

## Decisions Made

| slug_id | Area | Decision | Rationale |
|---------|------|----------|-----------|
| `local-storage-strategy` | Data & Storage | hybrid-48h-ttl | Raw video kept 48h in app-private storage with WorkManager auto-delete; processed clips exported to MediaStore gallery permanently; analytics computed on demand |
| `cloud-vs-local` | Deployment | local (on-device) | Android phone app; Firebase backend already exists; no cloud video processing |
| `data-storage-present` | Data & Storage | yes | Raw video (48h TTL), processed clips (MediaStore), Firestore match state |
| `custom-ui-required` | UI/UX | yes | Highlight browser, analytics tab, camera preview, export controls — all custom Compose screens |
| `android-module-structure` | Architecture | separate-gradle-project | New `android-app/` directory at repo root, own `settings.gradle`. Zero risk to existing `wear-os/` build. Migrate to multi-module in a future iteration once shared code surface is clear |
| `recording-lifecycle` *(revised ⚡)* | Solution Flow | watch-triggered-after-phone-setup | Phone handles setup (names, format, camera preview, tripod mounting); watch confirms serving + sends WDL `start_match` message; phone starts match + recording atomically |
| `recording-sync-strategy` | Data & State | shared-device-clock | `recordingStartMillis` captured at Start signal; pointLog timestamps use `Date.now()` (client-side) instead of Firestore server timestamps; all clip offsets are local arithmetic |
| `permissions-model` | Security & Privacy | contextual-at-first-use | Camera + mic requested when user taps "Yes, recording"; storage auto-granted Android 10+; denial handled with inline banner + Settings deep-link |
| `watch-camera-preview` | Integration | mjpeg-over-wifi | Phone runs embedded Ktor HTTP server; watch fetches MJPEG stream over local WiFi at ~15fps 320×240; used for 3–5 min setup window |
| `match-start-workflow` | Solution Flow | phone-setup-then-watch-start | Phase 1 on phone (names, format, no-ad, camera preview + framing). Phase 2 on watch after toss (live preview confirm, serving toggle, START triggers match + recording) |
| `background-processing` | Architecture | foreground-service-coroutines | Foreground service (already needed for recording) pivots to processing mode post-match; persistent notification shows progress; StateFlow drives in-app progress UI; cloud deferred to Tier 3 |
| `score-overlay-design` | UI/UX | grand-slam-broadcast-style | ATP/WTA scorebug reference: abbreviated player names, set score boxes, game score, ball/racket serving indicator; top bar; semi-transparent dark background; static throughout clip; rendered via FFmpeg `drawtext` + `drawbox` filters |
| `highlight-ux-flow` | UI/UX | timeline-set-game-point | Set → Game → Point expandable list; tap to preview inline; tick to select; filter chips: Aces, Double faults, Break points, Tiebreak points |
| `export-flow` | UI/UX | both-individual-and-reel | Individual clips saved to MediaStore gallery; option to merge selected clips into single highlight reel via FFmpeg concat; Android share sheet for sharing |
| `analytics-scope` | Solution Flow | both-phone-rich-pwa-summary | Full analytics in Android phone app (Analytics tab): rally length avg+distribution, serve win % by player, break point conversion, momentum graph. Lightweight summary (serve %, rally avg) added to PWA end screen |
| `watch-match-creation-role` ⚡ | Integration | phone-creates-watch-signals | Phone creates and owns match document; watch sends WDL `{action:start_match, serving:N}` message; phone updates match to active + starts recording; watch remains Firestore read-only |
| `compliance-regimes` | Security & Privacy | none | Personal use tennis scoring tool; no regulatory frameworks apply |
| `docker-installed` | Architecture | yes | Docker available on dev machine |

### Revisions (surgical re-float)

| slug_id | Prior response | New response | Reason |
|---------|---------------|--------------|--------|
| `recording-lifecycle` | manual-start-stop | watch-triggered-after-phone-setup | User clarified full workflow: phone handles setup + camera preview + mounting; watch handles live preview + serve confirmation + atomic Start (match + recording) |

---

## Pre-Resolved (brownfield / inferred)

| slug_id | Value | Source |
|---------|-------|--------|
| `backend-language` | Kotlin | codebase (`tech-stack.md`) |
| `ui-framework` | Jetpack Compose | codebase (`tech-stack.md`) |
| `auth-provider` | Firebase Auth (Google Sign-In) | codebase (`codebase-learning.md`) |
| `database-sync` | Firebase Firestore | codebase (`codebase-learning.md`) |
| `state-schema` | `stateJson` with embedded `pointLog` timestamps | codebase (`data-model-diagram.md`) |
| `firebase-project` | `tennis-score-analytics` (shared) | codebase (`codebase-learning.md`) |
| `video-processing` | On-device via `ffmpeg-kit-android` | inferred (`knowledge/project-brief.md`) |
| `ball-tracking` | Deferred (not in scope) | inferred (`knowledge/project-brief.md`) |

---

## Constraints & Compliance

- **No compliance regime** — personal use app; no HIPAA, SOC2, GDPR, PCI obligations
- **Same Firebase project** — `google-services.json` shared; no new Firebase project provisioning
- **Android only** — iOS parity not a goal; no cross-platform framework (React Native, Flutter) considered
- **On-device processing** — no cloud video pipeline for MVP; ffmpeg-kit-android only
- **minSdk ≥ 24** — MediaStore write auto-granted Android 10+; no `WRITE_EXTERNAL_STORAGE` needed
- **Wear OS read-only for Firestore** — watch sends only WDL signals; no new Firestore security rules required
- **Tier 3 deferred** — ball tracking, court detection, shot classification explicitly out of scope

---

## Open Questions

| # | Question | Impact |
|---|----------|--------|
| 1 | What `minSdk` for the phone app? (24 = Android 7, 26 = Android 8) | Affects MediaStore and CameraX API surface |
| 2 | Landscape or portrait recording? | Affects overlay layout and FFmpeg output dimensions |
| 3 | Should the watch MJPEG preview work when phone and watch are not on the same WiFi? | If not on same WiFi, fall back to WDL JPEG snapshots (2s refresh) |
| 4 | Should raw video be stored in internal storage (app-private) or external (SD card if present)? | External is user-accessible but not available on all devices |

---

## Constraint Audit

| Check | Result | Detail |
|-------|--------|--------|
| A — Mandatory coverage | ✅ PASS | All mandatory slugs answered |
| B — Forbidden options | ✅ PASS | No forbidden options presented |
| C — Whitelist adherence | ✅ PASS | Clean |
| D — Service scope | ✅ PASS | Clean |
| E — Mandated options | ✅ PASS | Clean |
| F — Elimination provenance | ✅ PASS | Clean |

**Gate result: PASS — 2026-10-21**
