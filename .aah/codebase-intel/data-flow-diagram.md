# Data Flow

## Overview
Data enters the system when a scorer taps to award a point on either the phone or the watch. The scoring engine computes the new state client-side, serializes it to JSON, and writes it to Firestore. The other client receives the update via a real-time Firestore listener and re-renders. The Wearable Data Layer is used only for the initial match ID handshake.

## Primary Scoring Flow

```mermaid
flowchart TD
    TAP[Scorer taps phone or watch]

    subgraph "Phone Path"
        P_SE[JS scoring functions\ncompute new state]
        P_SER[JSON.stringify stateJson]
        P_CHK{Is online?}
        P_FS[Write to\nmatches/id]
        P_IDB[Queue in\nIndexedDB]
        P_FLUSH[Flush queue\non reconnect]
    end

    subgraph "Watch Path"
        W_SE[ScoringEngine.kt\nawardPoint]
        W_SER[serializeState\nFirestoreRepository]
        W_FS[Write to\nmatches/id]
    end

    subgraph "Firestore"
        FS[(matches/id\nstateJson)]
    end

    subgraph "Receive Update"
        P_SNAP[onSnapshot listener\nPhone PWA]
        W_SNAP[addSnapshotListener Flow\nWatch MatchViewModel]
        P_CHK2{updatedBy ==\nmy deviceId?}
        W_CHK2{updatedBy ==\nmy deviceId?}
        P_RENDER[Re-render score\nlog and stats]
        W_RENDER[Re-render\nScoreScreen]
    end

    TAP --> P_SE & W_SE
    P_SE --> P_SER --> P_CHK
    P_CHK -->|online| P_FS
    P_CHK -->|offline| P_IDB --> P_FLUSH --> P_FS
    W_SE --> W_SER --> W_FS
    P_FS --> FS
    W_FS --> FS
    FS --> P_SNAP & W_SNAP
    P_SNAP --> P_CHK2
    P_CHK2 -->|different device| P_RENDER
    P_CHK2 -->|own write| skip1[Skip re-render]
    W_SNAP --> W_CHK2
    W_CHK2 -->|different device| W_RENDER
    W_CHK2 -->|own write| skip2[Skip re-render]
```

## Match Lifecycle Flow

```mermaid
flowchart LR
    START[User starts match\non phone PWA]
    ID[6-char match ID\ngenerated]
    ACTIVE[Write activeMatchId\nto users/uid]
    WDL[Push match ID\nvia Wearable Data Layer]
    WATCH_JOIN[Watch detects activeMatchId\nauto-connects to match]
    SCORING[Bidirectional scoring\nvia Firestore]
    END[Match complete\ndone=true in stateJson]
    ARCHIVE[Write to\ncompleted_matches/id]
    CLEAR[Clear activeMatchId\nin users/uid]

    START --> ID --> ACTIVE
    ACTIVE --> WDL --> WATCH_JOIN
    ACTIVE --> WATCH_JOIN
    WATCH_JOIN --> SCORING --> END
    END --> ARCHIVE --> CLEAR
```

## Data Pipelines

| Pipeline | Source | Processing | Destination |
|----------|--------|-----------|-------------|
| Point award (phone) | Tap on player name | JS scoring engine → JSON serialize | Firestore `matches/{id}` |
| Point award (watch) | Tap on screen half | `ScoringEngine.awardPoint` → `serializeState` | Firestore `matches/{id}` |
| Live sync (phone) | Firestore `onSnapshot` | Parse `stateJson` → re-render UI | Phone screen |
| Live sync (watch) | Firestore Flow snapshot | `parseState` → ViewModel state | Watch screen |
| Offline queue flush | `online` event | Dequeue IndexedDB → batch Firestore writes | Firestore `matches/{id}` |
| Match archive | Match `done=true` | Extract summary + full point log | Firestore `completed_matches/{id}` |
| Watch auto-connect | Firestore `users/{uid}.activeMatchId` | Watch listener detects change → join match | Watch scoring session |
| Match ID push | Phone starts match | Wearable Data Layer path `/match` key `matchId` | Watch `WearDataListenerService` |
| Spectator join | URL `?match=ID` | Read-only Firestore listener | Phone read-only view |

## Integration Points

- **Inbound:** Scorer taps (phone touch, watch gesture), Google Sign-In token
- **Outbound:** Firestore writes (`matches`, `completed_matches`, `users`), Wearable Data Layer message
- **Bidirectional:** Firestore real-time listeners on both clients
- **Async:** IndexedDB offline queue (phone only), service worker cache (PWA assets)
- **Read-only:** Spectator clients listen to `matches/{id}` without writing
