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
