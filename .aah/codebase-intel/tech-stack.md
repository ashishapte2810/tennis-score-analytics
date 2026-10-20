# Technology Stack

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Language (PWA)** | HTML/CSS/JavaScript | ES2020+ | Single-file PWA — no transpilation |
| **Language (Watch)** | Kotlin | JVM default (project) | Android Wear OS app |
| **UI Framework (Watch)** | Jetpack Compose for Wear OS | Compose BOM 2024.02.00 | Declarative watch UI |
| **UI Components (Watch)** | Wear Compose Material | 1.3.0 | Wear OS design system components |
| **Navigation (Watch)** | Wear Compose Navigation | 1.3.0 | Screen routing on watch |
| **State Management (Watch)** | Lifecycle ViewModel + Compose | 2.7.0 | Reactive state, `collectAsStateWithLifecycle` |
| **Concurrency (Watch)** | Kotlinx Coroutines | 1.7.3 | Async Firestore flows, lifecycle scopes |
| **Auth** | Firebase Auth (Google provider) | JS SDK 10.12.0 / Firebase BOM 32.7.2 | Google Sign-In on phone and watch |
| **Database / Sync** | Firebase Firestore | JS SDK 10.12.0 / Firebase BOM 32.7.2 | Real-time bidirectional match sync |
| **Offline Storage (PWA)** | IndexedDB (browser native) | — | Write queue for offline scoring on phone |
| **Hosting** | Firebase Hosting | — | CDN delivery of PWA |
| **PWA** | Service Worker + Web App Manifest | Cache v11 | Installable, cache-first asset strategy |
| **Watch Integration** | Android Wearable Data Layer | Play Services Wearable 18.1.0 | One-way match ID push phone → watch |
| **Watch Face** | Wear Watchface Complications KTX | 1.2.1 | ShortText complication showing live score |
| **Build System (Watch)** | Gradle (Groovy DSL) | Wrapper 9.3.0 | Android app build |
| **Target Platform (Watch)** | Wear OS 3.0+ | compileSdk/targetSdk 34, minSdk 30 | Pixel Watch and Wear OS 3 devices |
| **Dev Environment (Watch)** | Android Studio Hedgehog+ | 2023.1.1+ | IDE, emulator, ADB sideload |
| **Deployment (PWA)** | Firebase CLI (`firebase deploy`) | — | Push to Firebase Hosting |
| **Deployment (Watch)** | ADB sideload / Android Studio | — | Install debug APK on watch |
| **Version Control** | Git + GitHub | — | Source control, issue tracking (AAH) |
