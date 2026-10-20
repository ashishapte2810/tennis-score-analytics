# Data Model

## Firestore collections

### `matches/{id}`

Active and in-progress matches. Document ID is a 6-char alphanumeric string (e.g. `ABC123`).

| Field | Type | Description |
|-------|------|-------------|
| `stateJson` | string | Full serialized match state (see schema below) |
| `updatedBy` | string | Device ID of the last writer — used to skip self-updates |
| `updatedAt` | Timestamp | Server timestamp of last write |
| `ownerId` | string | Firebase Auth UID of match owner |
| `expiresAt` | Date | 7 days from match start — used for Firestore TTL auto-deletion (policy must be enabled; see [docs/setup.md](setup.md)) |

### `completed_matches/{id}`

Archived matches written on match completion. Same document ID as the active match.

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Match ID (redundant with doc ID, for convenience) |
| `ownerId` | string | Firebase Auth UID |
| `names` | string[] | `[player1Name, player2Name]` |
| `date` | string | ISO 8601 timestamp of match start |
| `completedSets` | object[] | `[{p1: int, p2: int}, ...]` — one entry per completed set |
| `winner` | 0 \| 1 | Index of winning player |
| `totalSets` | int | Match format (3 or 5) |
| `pointLog` | object[] | Full point-by-point log (see point log entry schema) |
| `savedAt` | Timestamp | Server timestamp when archive write occurred |
| `deletedAt` | Timestamp | Set on soft-delete; TTL policy purges after 7 days (optional field) |

### `users/{uid}`

Per-user state. Document ID is the Firebase Auth UID.

| Field | Type | Description |
|-------|------|-------------|
| `activeMatchId` | string \| null | Match ID of the user's current active match. The watch listens to this field to auto-connect without manual ID entry. |

---

## stateJson schema

The entire match state is stored as a single JSON string in `matches/{id}.stateJson`. Both the PWA (JavaScript) and watch app (Kotlin `FirestoreRepository`) parse and write this format.

```json
{
  "names": ["Player 1", "Player 2"],
  "pts": [0, 0],
  "games": [0, 0],
  "tbPts": [0, 0],
  "completedSets": [[6, 3], [6, 4]],
  "serving": 0,
  "mode": "normal",
  "totalSets": 3,
  "setsNeeded": 2,
  "dec": "tb",
  "done": false,
  "pointNum": 42,
  "matchStartedAt": "2026-07-11T10:00:00.000Z",
  "pointLog": [...]
}
```

### Field reference

| Field | Type | Description |
|-------|------|-------------|
| `names` | string[2] | Player names |
| `pts` | int[2] | Current game points (0=0, 1=15, 2=30, 3=40) |
| `games` | int[2] | Games won in the current set |
| `tbPts` | int[2] | Points in the current tiebreak (0 when not in tiebreak) |
| `completedSets` | int\[]\[] | Set scores as `[[p1, p2], ...]`, one entry per completed set |
| `serving` | 0 \| 1 | Index of current server |
| `mode` | string | `"normal"` \| `"tiebreak"` \| `"supertb"` |
| `totalSets` | 3 \| 5 | Match format (best of) |
| `setsNeeded` | int | Sets needed to win (2 for best-of-3, 3 for best-of-5) |
| `dec` | string | Deciding set format: `"tb"` (tiebreak) \| `"adv"` (advantage) \| `"stb"` (super tiebreak) |
| `done` | boolean | `true` when match is complete |
| `pointNum` | int | Running point counter (1-indexed) |
| `matchStartedAt` | string | ISO 8601 timestamp |
| `pointLog` | object[] | All points played (see below) |

### Point log entry

Each element of `pointLog` represents one point:

```json
{
  "point": 1,
  "winner": "Player 1",
  "winnerIdx": 0,
  "server": "Player 1",
  "set": 1,
  "gameNum": 1,
  "ctx": "S1 G1",
  "score": "15–0 | G:1–0",
  "mode": "regular",
  "serve": "1st",
  "ts": "2026-07-11T10:00:05.000Z",
  "ue": true
}
```

| Field | Type | Description |
|-------|------|-------------|
| `point` | int | Point number within the match |
| `winner` | string | Name of the player who won the point |
| `winnerIdx` | 0 \| 1 | Index of the winning player |
| `server` | string | Name of the server for this point |
| `set` | int | Set number (1-indexed) |
| `gameNum` | int | Game number within the set (1-indexed) |
| `ctx` | string | Human-readable context label, e.g. `"S2 G4"` |
| `score` | string | Score string at the time of the point, e.g. `"15–0 \| G:1–0"` |
| `mode` | string | `"regular"` \| `"tiebreak"` \| `"supertb"` |
| `serve` | string | `"1st"` \| `"2nd"` \| `"Double Fault"` |
| `ts` | string | ISO 8601 timestamp |
| `ue` | boolean | Present and `true` only when tagged as an unforced error |

---

## Schema compatibility

The `stateJson` format is shared between the PWA and the watch app. When adding fields:

- **New optional fields:** Add with a fallback default when parsing. Never assume the field is present in existing Firestore documents.
- **Never rename or remove fields:** Existing in-progress matches in Firestore use the current format. A rename is a breaking change.
- **Keep both implementations in sync:** `ScoringEngine.kt` + `FirestoreRepository.kt` (watch) and the scoring functions in `tennis_tracker.html` (phone) must agree on the format.
