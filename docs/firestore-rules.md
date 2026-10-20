# Firestore Security Rules

## Current state (post S17-8 fix)

The watch app's `saveState()` now includes `ownerId` in every write (fixed in
`FirestoreRepository.kt`). The tight rules below are live in `firestore.rules`
and should be deployed with:

```bash
firebase deploy --only firestore:rules
```

## Rules

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
