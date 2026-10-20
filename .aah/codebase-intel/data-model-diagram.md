# Data Model

## Overview
All match state is stored as a single serialized JSON blob (`stateJson`) in Firestore. There is no relational schema — the entire match history and scoring state travels as one atomic document. Both clients (PWA and watch) parse and write the same JSON format.

## Entity Relationship Diagram

```mermaid
erDiagram
    USER {
        string uid PK
        string activeMatchId FK
    }
    MATCH {
        string id PK
        string stateJson
        string updatedBy
        timestamp updatedAt
        string ownerId FK
        date expiresAt
    }
    COMPLETED_MATCH {
        string id PK
        string ownerId FK
        string[] names
        string date
        object[] completedSets
        int winner
        int totalSets
        object[] pointLog
        timestamp savedAt
        timestamp deletedAt
    }
    MATCH_STATE {
        string[] names
        int[] pts
        int[] games
        int[] tbPts
        int[][] completedSets
        int serving
        string mode
        int totalSets
        int setsNeeded
        string dec
        bool done
        int pointNum
        string matchStartedAt
        object[] pointLog
    }
    POINT_LOG_ENTRY {
        int point
        string winner
        int winnerIdx
        string server
        int set
        int gameNum
        string ctx
        string score
        string mode
        string serve
        string ts
        bool ue
    }

    USER ||--o{ MATCH : "activeMatchId"
    USER ||--o{ COMPLETED_MATCH : "owns"
    MATCH ||--|| MATCH_STATE : "stateJson (serialized)"
    MATCH_STATE ||--o{ POINT_LOG_ENTRY : "pointLog[]"
    COMPLETED_MATCH ||--o{ POINT_LOG_ENTRY : "pointLog[]"
```

## Key Entities

| Entity | Purpose | Key Fields | Relationships |
|--------|---------|------------|---------------|
| `users/{uid}` | Per-user state | `activeMatchId` | References `matches` |
| `matches/{id}` | Active/in-progress match | `stateJson`, `updatedBy`, `ownerId`, `expiresAt` | Owned by `users` |
| `completed_matches/{id}` | Archive of finished matches | `ownerId`, `names`, `completedSets`, `winner`, `pointLog`, `deletedAt` | Owned by `users` |
| `MatchState` (embedded) | Full match scoring state | `pts`, `games`, `tbPts`, `completedSets`, `mode`, `serving`, `done` | Embedded in `stateJson` |
| `PointLogEntry` (embedded) | Single point record | `winner`, `serve`, `ts`, `ue` | Array in `MatchState.pointLog` |

## Data Stores

| Store | Type | Purpose |
|-------|------|---------|
| Firestore `matches` | NoSQL document | Real-time active match sync between all clients |
| Firestore `completed_matches` | NoSQL document | Persistent match archive, soft-deletable |
| Firestore `users` | NoSQL document | Per-user active match pointer for watch auto-connect |
| IndexedDB `tsa_offline` | Browser key-value | Offline write queue for PWA (phone only); flushes on reconnect |
