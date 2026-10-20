# Firestore Security Rules

## Current state

The current rules allow any authenticated user to read and write any document:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

This means any signed-in user can read or modify any other user's matches. This is intentional for now (see **Why rules are loose** below).

## Why rules are loose

The intended rules would restrict writes to the match owner:

```javascript
match /matches/{matchId} {
  allow write: if request.auth != null
    && (resource == null || resource.data.ownerId == request.auth.uid);
}
```

This rule was implemented but reverted because the watch app's `saveState()` function does not include `ownerId` in its Firestore write payload. When the watch writes a point, `ownerId` is set to `null` (or omitted), which causes the owner-check to fail on the next write from the phone.

**Fix required (S17-8):** Add `ownerId` to the watch app's `saveState()` payload in `FirestoreRepository.kt`. Once that is deployed, the strict rules can be re-enabled.

## Target rules (post S17-8)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Active matches — owner can write; anyone authenticated can read (spectator mode)
    match /matches/{matchId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null
        && request.resource.data.ownerId == request.auth.uid;
      allow update: if request.auth != null
        && resource.data.ownerId == request.auth.uid;
      allow delete: if request.auth != null
        && resource.data.ownerId == request.auth.uid;
    }

    // Completed archive — owner only
    match /completed_matches/{matchId} {
      allow read, write: if request.auth != null
        && (resource == null || resource.data.ownerId == request.auth.uid);
    }

    // User state — own document only
    match /users/{userId} {
      allow read, write: if request.auth != null
        && request.auth.uid == userId;
    }
  }
}
```

## Known issues

### Watch ownerId omission (S8-2 / S17-8)
The watch's `FirestoreRepository.saveState()` currently omits `ownerId` from its write payload. This means any match the watch writes ends up with `ownerId = null` in Firestore, which breaks owner-only write rules. The fix is to pass the authenticated user's UID in the watch's Firestore write.

### Missing composite index (TD-7)
The archive query (`completed_matches` ordered by `ownerId + savedAt`) requires a composite Firestore index. Without it, the query fails on first use. Create it in Firebase Console → Firestore → Indexes, or follow the URL in the Firebase error log when the query runs.

### ownerId write timing (TD-7)
On the phone, `ownerId` can be `null` in the first Firestore write if the match starts before `auth.onAuthStateChanged` has resolved. The match document is created without `ownerId`, which means it won't appear in the owner's archive query. Fix: gate match creation on auth resolution.

## Deploying rules

```bash
firebase deploy --only firestore:rules
```

Rules live in `firestore.rules` (create this file at the repo root if not already present). The `firebase.json` must reference it:

```json
{
  "firestore": {
    "rules": "firestore.rules"
  }
}
```
