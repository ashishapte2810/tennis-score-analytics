# Tennis Score Analytics — Sprint Plan
**Role:** Scrum Master + Tech Lead: Claude  
**Stack:** Vanilla HTML/CSS/JS · Firebase Firestore · GitHub Pages PWA · Kotlin/Jetpack Compose (Wear OS)  
**Velocity target:** ~8 story points per sprint  
**Sprint length:** 1 week  

---

## ✅ Sprint 0 — Foundation (DONE)
**Goal:** Working deployable shell

| # | Story | Points | Status |
|---|-------|--------|--------|
| S0-1 | Single-file PWA scaffold (HTML/CSS/JS) | 2 | ✅ Done |
| S0-2 | Firebase project + Firestore wiring | 2 | ✅ Done |
| S0-3 | Service worker + manifest (installable PWA) | 2 | ✅ Done |
| S0-4 | GitHub Pages deployment pipeline | 1 | ✅ Done |

---

## ✅ Sprint 1 — Core Scoring Engine (DONE)
**Goal:** Full tennis scoring logic end-to-end

| # | Story | Points | Status |
|---|-------|--------|--------|
| S1-1 | Point-by-point scoring (15/30/40/Deuce/Adv) | 3 | ✅ Done |
| S1-2 | Set/game/match logic (advantage + tiebreak + super-TB) | 3 | ✅ Done |
| S1-3 | Serve tracking (1st / 2nd / Double Fault) | 1 | ✅ Done |
| S1-4 | SCORE / LOG / STATS tab layout | 2 | ✅ Done |
| S1-5 | Firebase real-time sync (live match state) | 2 | ✅ Done |
| S1-6 | Match ID URL sharing + live chip indicator | 1 | ✅ Done |
| S1-7 | Undo last point | 1 | ✅ Done |

---

## ✅ Sprint 2 — Archive & Reports (DONE)
**Goal:** Match history, exports, read-only browsing

| # | Story | Points | Status |
|---|-------|--------|--------|
| S2-1 | localStorage archive (up to 100 matches) | 2 | ✅ Done |
| S2-2 | PDF report (window.open, inlined CSS) | 3 | ✅ Done |
| S2-3 | CSV point-by-point export | 1 | ✅ Done |
| S2-4 | Multi-variant seeded player summaries | 2 | ✅ Done |
| S2-5 | Archive screen + browse completed matches | 2 | ✅ Done |
| S2-6 | Read-only mode (disabled scoring UI) | 1 | ✅ Done |
| S2-7 | Firestore backup (completed_matches collection + OWNER_ID) | 3 | ✅ Done |

---

## ✅ Sprint 3 — UI/UX Polish (DONE)
**Goal:** Mobile-first redesign, intuitive score screen

| # | Story | Points | Status |
|---|-------|--------|--------|
| S3-1 | Tennis ball SVG serving indicator (active / inactive states) | 2 | ✅ Done |
| S3-2 | Score cards redesigned: bigger (108px), colored by player | 2 | ✅ Done |
| S3-3 | Serve selector (1st/2nd/DF) repositioned below score cards | 1 | ✅ Done |
| S3-4 | Remove stat bars from score screen (stats tab only) | 1 | ✅ Done |
| S3-5 | Export panel for completed matches (PDF + CSV + result) | 2 | ✅ Done |
| S3-6 | Home/back buttons on all screens | 1 | ✅ Done |
| S3-7 | SW cache busting: network-first HTML, ?v= param, auto-reload | 2 | ✅ Done |
| S3-8 | Label + icon cleanup (1st/2nd/DF, 🏁, ball colors) | 1 | ✅ Done |

---

## 🚧 Sprint 7 — Wear OS Native App (IN PROGRESS)
**Goal:** Pixel Watch 4 scoring from the wrist via native Kotlin app

**Definition of Done:** User can score a live match from Pixel Watch 4, scores sync to phone PWA in real-time, screen stays on during match, match-over screen shown on completion.

**Architecture decision:** Native Kotlin/Jetpack Compose for Wear OS (not PWA — Chrome not available on Wear OS 3+). Shares same Firestore backend as phone PWA.

**Match connection strategy:** Manual 6-char ID entry for now (Option B via Firebase deferred to Sprint 4 Auth, which makes this automatic via shared user.uid).

**UX decision:** Option C — single tap = 1st serve (most common), long press = 2nd serve / double fault overlay.

| # | Story | Points | Status |
|---|-------|--------|--------|
| S7-1 | Native Kotlin/Compose Wear OS project scaffold | 3 | ✅ Done |
| S7-2 | Scoring engine (Kotlin) — mirrors phone JS logic exactly | 3 | ✅ Done |
| S7-3 | Firestore sync — reads/writes same matches/{id} doc as phone | 2 | ✅ Done |
| S7-4 | Split-screen scoring UI: P1 (green) left, P2 (orange) right | 2 | ✅ Done |
| S7-5 | Option C serve UX: tap=1st, long press=2nd/DF overlay | 2 | ✅ Done |
| S7-6 | Haptic feedback: short/double/long buzz per serve type | 1 | ✅ Done |
| S7-7 | UI polish: matches phone PWA dark theme, 44sp scores, ball indicator | 2 | ✅ Done |
| S7-8 | Always-on screen + ambient mode (dim between points, save battery) | 2 | ✅ Done |
| S7-9 | Match over screen: winner, trophy, set scores + home button | 1 | ✅ Done |
| S7-10 | Manual match ID entry (6-char picker) — temporary until Sprint 4 Auth | 1 | ✅ Done |
| S7-11 | Wearable Data Layer listener (auto-receive match ID from phone) | 1 | ✅ Done |
| S7-12 | Double fault bug fix: always awards point to receiver of server | 1 | ✅ Done |
| S7-13 | pointLog written to Firestore from watch — phone LOG/STATS now populate | 2 | ✅ Done |
| S7-14 | Sideload to physical Pixel Watch 4 + match day test | 2 | ✅ Done |
| S7-15 | Show match ID on scoring screen (small, bottom of screen) | 1 | ✅ Done |
| S7-16 | Elongated haptic feedback for match-winning point | 1 | 📋 Backlog |
| S7-17 | Apple Watch path: Swift/WatchKit scoping (separate native project) | 2 | 📋 Backlog |

**Risks:**
- Match ID entry is manual until Sprint 4 Auth lands — acceptable for now
- Apple Watch requires separate iOS + watchOS native project (separate engineering track)

---

## 🔜 Sprint 4 — User Auth & Profiles (NEXT — today)
**Goal:** Real user accounts, cross-device sync, secure data

**Definition of Done:** User can sign in with Google, see their full match history on any device, watch auto-connects to active match via shared user.uid (no manual ID entry).

**Key unlock:** Sprint 4 Auth eliminates the manual match ID entry on the watch — watch queries Firestore for `matches where ownerId == user.uid` automatically.

| # | Story | Points | Priority | Notes |
|---|-------|--------|----------|-------|
| S4-1 | Google Sign-In (Firebase Auth, one-tap) | 3 | 🔴 High | ✅ Done |
| S4-2 | Migrate local archive to Firestore on first login | 2 | 🔴 High | ❌ Dropped — user accepted clean start |
| S4-3 | Firestore security rules (users own their data) | 2 | 🔴 High | ✅ Done — rules published in Firebase console |
| S4-4 | Persist login state across sessions | 1 | 🔴 High | ✅ Done — Firebase LOCAL persistence |
| S4-5 | User profile screen (name, avatar initial, stats summary) | 2 | 🟡 Med | ✅ Done — avatar + name bar on setup screen |
| S4-6 | Sign-out flow | 1 | 🟡 Med | ✅ Done |
| S4-7 | Watch auto-connect via shared user.uid (replaces manual ID entry) | 2 | 🔴 High | ✅ Done — silent sign-in + users/{uid}/activeMatch Firestore listener |

**Risks:**
- Anonymous → auth migration: users who already have matches need a smooth transition
- Firestore rules must not break read-only match sharing by URL

---

## 📋 Sprint 5 — Social & Sharing (BACKLOG)
**Goal:** Multi-user awareness, H2H records, invite flow

| # | Story | Points | Priority | Notes |
|---|-------|--------|----------|-------|
| S5-1 | Player H2H record (wins/losses vs named opponent) | 3 | 🟡 Med | Derived from completed_matches, group by opponent name |
| S5-2 | Share completed match to WhatsApp/iMessage (native share API) | 1 | 🟡 Med | navigator.share() with match URL |
| S5-3 | Invite opponent to view live match (send link flow) | 2 | 🟡 Med | Already partially there via match ID |
| S5-4 | Club/group — create a group, see all members' matches | 5 | 🟢 Low | New Firestore collection: groups/{id}/members |
| S5-5 | Match notes (free-text per match, stored in Firestore) | 2 | 🟢 Low | E.g. "windy conditions", "clay court" |

---

## 📋 Sprint 6 — Analytics & Growth (BACKLOG)
**Goal:** Deeper stats, broader reach, leaderboards

| # | Story | Points | Priority | Notes |
|---|-------|--------|----------|-------|
| S6-1 | Landing page (what it does, install CTA, demo video) | 3 | 🟡 Med | Separate page or section on GH Pages |
| S6-2 | Custom domain (e.g. tennytrack.app) | 1 | 🟢 Low | ~$12/yr, DNS + GH Pages config |
| S6-3 | Leaderboard within a club/group | 3 | 🟢 Low | Requires S5-4 |
| S6-4 | Match tagging (surface: clay/hard/grass, indoor/outdoor) | 1 | 🟢 Low | Stored on match doc, filterable in stats |
| S6-5 | Tournament bracket mode | 5 | 🟢 Low | New match type, separate scoring flow |
| S6-6 | Push notifications (live score updates to spectators) | 3 | 🟢 Low | Firebase Cloud Messaging |
| S6-7 | Seasonal stats (wins per month, best serve %, trends) | 3 | 🟢 Low | Aggregated from completed_matches |

---

## 📋 Sprint 8 — Resilience & Privacy (BACKLOG)
**Goal:** Offline-first scoring, secure match access, spectator experience

| # | Story | Points | Priority | Notes |
|---|-------|--------|----------|-------|
| S8-1 | Offline point queue (IndexedDB, sync on reconnect) | 5 | 🔴 High | Points lost today if connection drops mid-match |
| S8-2 | Match privacy: longer token or auth-gated access | 3 | 🔴 High | 6-char IDs are guessable; anyone can view any live match |
| S8-3 | Spectator mode (auto-refresh, no accidental scoring) | 2 | 🟡 Med | Read-only view with live Firestore listener, clean UI |
| S8-4 | Court-side display mode (tablet/TV large screen) | 2 | 🟡 Med | Landscape layout, 120px+ score font, high contrast |
| S8-5 | Match templates (save/load setup presets) | 2 | 🟢 Low | e.g. "Club night: BO3, no-ad, match TB" |

---

## 📋 Sprint 9 — Career Stats & Intelligence (BACKLOG)
**Goal:** Aggregated stats across matches, historical trends, H2H

| # | Story | Points | Priority | Notes |
|---|-------|--------|----------|-------|
| S9-1 | Career stats dashboard (all-time win rate, serve %, DF rate) | 5 | 🟡 Med | Aggregated from completed_matches collection |
| S9-2 | H2H records per opponent | 3 | 🟡 Med | Group completed_matches by opponent name |
| S9-3 | Monthly/seasonal trends (wins per month, serve % over time) | 3 | 🟢 Low | Chart view, requires S9-1 |
| S9-4 | Match tagging: surface, indoor/outdoor, notes | 1 | 🟢 Low | Stored on match doc, filterable |

---

## 📋 Sprint 10 — Apple Watch (BACKLOG)
**Goal:** Native Swift/WatchKit app for Apple Watch 4+

**Technical note:** Completely separate engineering track from the Wear OS app. Requires Xcode + Swift + WatchKit. Shares same Firestore backend. Scope only after Sprint 4 Auth is complete (shared user.uid needed for auto-connect).

| # | Story | Points | Priority | Notes |
|---|-------|--------|----------|-------|
| S10-1 | Xcode project scaffold: WatchKit + iOS companion | 3 | 🔴 High | Requires paired iPhone app |
| S10-2 | Scoring engine in Swift — mirrors Kotlin logic | 3 | 🔴 High | Same rules, same Firestore schema |
| S10-3 | Split-screen scoring UI in SwiftUI | 3 | 🔴 High | Same Option C UX as Wear OS |
| S10-4 | Firebase iOS SDK + Firestore sync | 2 | 🔴 High | Same matches/{id} doc |
| S10-5 | Haptic feedback (WKHapticType) | 1 | 🟡 Med | CoreHaptics on watchOS |
| S10-6 | Always-on display (Extended Runtime Session) | 2 | 🟡 Med | watchOS workout session keeps screen on |
| S10-7 | Auto-connect via shared Firebase Auth user.uid | 2 | 🔴 High | Requires Sprint 4 Auth |

---

## Backlog — Technical Debt
| # | Item | Notes |
|---|------|-------|
| TD-1 | Firestore index: completed_matches (ownerId, savedAt desc) | Needed for archive query — Firebase will prompt on first run |
| TD-2 | Error boundary: graceful UI when Firestore is unreachable | Currently shows raw alert() |
| TD-3 | Offline point logging with sync on reconnect | IndexedDB queue |
| TD-4 | Split HTML into modules (if file grows beyond 1800 lines) | Consider a simple build step |
| TD-5 | Wear OS: move project off OneDrive to local disk | OneDrive + Gradle = slow builds and sync conflicts |

---

## Architecture decisions log
| Decision | Choice | Reason |
|----------|--------|--------|
| Phone platform | PWA (HTML/JS) | Cross-platform, zero install, URL sharing |
| Watch platform | Native Kotlin (Wear OS) | No browser on Wear OS 3+; PWA not viable |
| Apple Watch | Native Swift (future) | No web support on watchOS |
| Watch serve UX | Option C — tap/long-press | Optimises for 1st serve (80%+ of points) |
| Watch match connect | Manual ID entry now, auto via Auth in Sprint 4 | Auth eliminates need for any pairing flow |
| Firestore schema | Single matches/{id} doc, shared by phone + watch | Simplest real-time sync; no extra infra |

---

## Metrics to track
- **Engagement:** matches completed per user per week
- **Retention:** % users who play 3+ matches
- **Reliability:** Firestore write failures (check Firebase console)
- **Performance:** SW cache hit rate, first load time
- **Watch usage:** % matches scored from watch vs phone

---

*Last updated: 2026-07-13 · Sprint 7 (Wear OS) in progress · Next: Sprint 4 — User Auth*
