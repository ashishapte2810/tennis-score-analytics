# Project Brief — Original Context

## Iteration 1

Since we have point timestamps from the Breakpoint scoring app, I want to record the match video on the same Android phone that runs the scorer, then:
- Overlay scores on the match video (current score, server, set scores per point)
- Auto-generate highlight clips per point, trimmed using the pointLog timestamps
- Let the user select which highlights to keep and export them to the gallery or share
- Provide match analytics (rally length, point duration, serve win %, deuce conversion) computed from the existing pointLog data

The reference app is SwingVision (Apple only). We want something equivalent for Android, but without ball tracking / court detection for now — on-device processing only using ffmpeg-kit-android.

Platform: native Android phone app (Kotlin + Jetpack Compose), sitting alongside the existing Wear OS module. Same Firebase project (same google-services.json). Recording and processing happen on the same device as the scorer PWA.
