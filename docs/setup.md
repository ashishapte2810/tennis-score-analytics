# Firebase Setup Guide

This guide covers everything needed to connect to the Firebase project — required for both the PWA and the watch app.

## Firebase project

- **Project ID:** `tennis-score-analytics`
- **Hosting URLs:** `tennis-score-analytics.web.app` and `tennis-score-analytics.firebaseapp.com`
- **Services used:** Authentication, Firestore, Hosting

## Services to enable in Firebase Console

### Authentication

1. Firebase Console → Authentication → Sign-in method
2. Enable **Google** provider
3. Add your app's SHA-1 fingerprint for Android (required for watch app — see [wear-os/SETUP.md](../wear-os/SETUP.md))

### Firestore

1. Firebase Console → Firestore Database → Create database
2. Start in **production mode**
3. Deploy the security rules in [docs/firestore-rules.md](firestore-rules.md)
4. Create required indexes (see below)

### Hosting

Already configured in `firebase.json`. Deploy with:

```bash
npm install -g firebase-tools
firebase login
firebase deploy --only hosting
```

## Required Firestore indexes

Without these, queries will fail on first use (Firebase logs the exact index creation URL):

| Collection | Fields | Order |
|------------|--------|-------|
| `completed_matches` | `ownerId` ASC, `savedAt` DESC | Composite |

To create: Firebase Console → Firestore → Indexes → Add index.

## TTL policies (must be manually enabled)

Two TTL policies are needed. The `expiresAt` and `deletedAt` fields are written by the app, but Firebase TTL auto-deletion must be enabled separately.

| Collection | Field | TTL |
|------------|-------|-----|
| `matches` | `expiresAt` | 7 days — auto-deletes in-progress match documents |
| `completed_matches` | `deletedAt` | 7 days — purges soft-deleted archived matches |

To enable: Firebase Console → Firestore → TTL → Add TTL policy for each.

Without this, old `matches` documents and soft-deleted archive entries accumulate indefinitely.

## Watch app: google-services.json

The file `wear-os/app/google-services.json` is not committed (it is gitignored).

To get it:

1. Firebase Console → Project Settings → Your apps
2. Find the Android app with package name `com.tennis.tracker.wear`
3. Download `google-services.json`
4. Place it at `wear-os/app/google-services.json`

If the Android app does not exist yet:
1. Add app → Android
2. Package name: `com.tennis.tracker.wear`
3. Register your debug SHA-1 (see [wear-os/SETUP.md](../wear-os/SETUP.md) for how to get it)
4. Download `google-services.json`

## Firebase config in the PWA

The Firebase config (API key, project ID, etc.) is hardcoded in `tennis_tracker.html` around line 684. This is intentional — Firebase web API keys are designed to be public-facing. Security is enforced by Firestore security rules, not by keeping the key secret. See [docs/firestore-rules.md](firestore-rules.md).

## Firebase free tier limits

The app is designed to run within Firebase Spark (free) plan limits:

| Resource | Free limit | Notes |
|----------|-----------|-------|
| Firestore reads | 50k/day | Each score update = 1 read per listening client |
| Firestore writes | 20k/day | Each point = 1 write |
| Firestore storage | 1 GiB | stateJson grows with point log; large matches ~50 KB |
| Auth | Unlimited | |
| Hosting | 10 GiB/month bandwidth | PWA is ~300 KB total |

Monitor usage: Firebase Console → Usage and billing.
