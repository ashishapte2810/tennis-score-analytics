# Dependency Map

## External Dependencies

### PWA (phone)

| Package | Version | Purpose | Category |
|---------|---------|---------|----------|
| firebase-app-compat.js | 10.12.0 | Firebase SDK core | Backend |
| firebase-auth-compat.js | 10.12.0 | Google Sign-In, auth state | Auth |
| firebase-firestore-compat.js | 10.12.0 | Real-time database, match sync | Backend |
| IndexedDB (browser native) | — | Offline write queue (`tsa_offline`) | Storage |
| Service Worker API (browser native) | — | PWA install, cache-first asset serving | Infrastructure |

### Watch App (Kotlin)

| Package | Version | Purpose | Category |
|---------|---------|---------|----------|
| Compose BOM | 2024.02.00 | Jetpack Compose version management | UI |
| Wear Compose Material | 1.3.0 | Wear OS UI components | UI |
| Wear Compose Foundation | 1.3.0 | Wear OS layout primitives | UI |
| Wear Compose Navigation | 1.3.0 | Screen navigation on watch | UI |
| Activity Compose | 1.8.2 | Compose integration with Activity | UI |
| Lifecycle ViewModel Compose | 2.7.0 | ViewModel scoping for Compose | Architecture |
| Lifecycle Runtime KTX | 2.7.0 | `lifecycleScope`, coroutine integration | Architecture |
| Lifecycle Runtime Compose | 2.7.0 | `collectAsStateWithLifecycle` | Architecture |
| AndroidX Wear | 1.4.0 | Ambient mode support | Platform |
| Fragment KTX | 1.6.2 | Fragment extensions | Platform |
| Firebase BOM | 32.7.2 | Firebase SDK version management | Backend |
| Firebase Firestore KTX | (from BOM) | Real-time match sync | Backend |
| Firebase Auth KTX | (from BOM) | Google Sign-In, auth state | Auth |
| Play Services Auth | 20.7.0 | Google Sign-In intent, credential | Auth |
| Play Services Wearable | 18.1.0 | Wearable Data Layer (match ID push) | Platform |
| Kotlinx Coroutines Android | 1.7.3 | Async flows, lifecycle-aware coroutines | Concurrency |
| Kotlinx Coroutines Play Services | 1.7.3 | `await()` on Tasks (Firestore, Auth) | Concurrency |
| Wear Watchface Complications KTX | 1.2.1 | Watch face complication rendering | Platform |
| Kotlin Compose Compiler Extension | 1.5.14 | Compose annotation processing | Build |

## Internal Module Dependencies

### Watch App

| Module | Depends On | Depended By |
|--------|-----------|-------------|
| `MatchState.kt` | (none) | `ScoringEngine`, `FirestoreRepository` |
| `ScoringEngine.kt` | `MatchState` | `MatchViewModel` |
| `FirestoreRepository.kt` | `MatchState`, Firebase Firestore KTX | `MatchViewModel`, `TennisComplicationService` |
| `AuthRepository.kt` | Firebase Auth KTX, Play Services Auth | `MainActivity`, `SignInScreen` |
| `WearDataListenerService.kt` | Play Services Wearable, `MatchViewModel` | (Android system) |
| `MatchViewModel.kt` | `ScoringEngine`, `FirestoreRepository` | `MainActivity`, `ScoreScreen`, `MatchIdScreen` |
| `MainActivity.kt` | `MatchViewModel`, `AuthRepository`, Vibrator API | (Android system) |
| `ScoreScreen.kt` | `MatchViewModel`, Wear Compose | (none — leaf UI) |
| `SignInScreen.kt` | `AuthRepository`, Wear Compose | (none — leaf UI) |
| `MatchIdScreen.kt` | `MatchViewModel`, Wear Compose | (none — leaf UI) |
| `TennisComplicationService.kt` | `FirestoreRepository`, Wear Complications KTX | (watch face system) |

### PWA (phone)

| Module | Depends On | Depended By |
|--------|-----------|-------------|
| `tennis_tracker.html` (all-in-one) | Firebase JS SDK (CDN), IndexedDB, Service Worker | (none — single file) |
