# U2 Domain Entities

U2 owns no durable business state. Every type below is either generated from
`backend/openapi/planning-api.yaml` or a **view model** derived from a server payload for
presentation and interaction only. No view model may be persisted, and no view model may decide a
placement, a conflict or a completion — those remain U1's.

## 1. Transport Types (generated, never hand-written)

Generated from the checked-in contract. A drift between the generated output and the contract fails
the build (NFR-007).

| Type | Contract source | Used by |
|---|---|---|
| `Priority` | `LOW \| MEDIUM \| HIGH` | F-C03, F-C04, F-C07 |
| `TaskStatus` | `TODO \| COMPLETED` | F-C02, F-C07 |
| `TaskSort` | `CREATED_AT \| DUE_DATE \| PRIORITY \| TITLE` | F-C07 |
| `SortDirection` | `ASC \| DESC` | F-C07 |
| `ScheduleResolutionMode` | `PROPOSE_ONLY \| ACCEPT_CANDIDATE` | F-C05, F-C06 |
| `ScheduleView` | `{date, startTime, endTime}` half-open interval | F-C02, F-C06 |
| `TaskView` | task content + `status`, `schedule?`, `version` | all features |
| `TaskPageView` | `{content, page, size, totalElements, totalPages}` | F-C07 |
| `WeeklyPlanView` | `{weekStart, weekEndExclusive, scheduled[], backlog[]}` | F-C02, F-C03 |
| `TaskContentRequest` | create body | F-C04 |
| `UpdateTaskRequest` | full-replacement body incl. `expectedVersion` | F-C04 |
| `ScheduleTaskRequest` | `{date, startTime, expectedVersion, resolutionMode?}` | F-C05, F-C06 |
| `SetCompletionRequest` | `{completed, expectedVersion}` | F-C07, F-C02 |
| `ApiError` | `{code, message, requestId, fieldErrors?, currentVersion?, conflict?}` | F-C08, F-C09 |
| `FieldError` | `{field, code, message?}` | F-C04 |
| `ConflictView` | `{proposed, conflicting, nextCandidate?}` | F-C06 |

### Optional-field discipline

The contract omits absent optional fields rather than sending `null`. Generated types therefore use
`field?: T`, and no view model may substitute an empty string for an absent `description` or
`dueDate` — an absent value renders as the empty state, not as blank text.

## 2. Client View Models (derived, in-memory only)

### `WeekContext` — F-C01, F-C02

| Field | Type | Derivation |
|---|---|---|
| `weekStart` | `DateIso` | URL `?week=`; must be a Monday |
| `weekEndExclusive` | `DateIso` | `weekStart + 7d`, confirmed against the server payload |
| `days` | `DayColumn[7]` | Mon~Sun enumeration |
| `today` | `DateIso \| null` | Asia/Seoul today, `null` when outside this week |
| `view` | `'week' \| 'day'` | viewport breakpoint (FR-011) |
| `activeDay` | `DateIso` | day view only; defaults to `today`, else `weekStart` |

### `DayColumn` — F-C02

`{date, weekday, isToday, isWeekend, slots: ScheduleBlock[]}`. `isWeekend` and `isToday` drive a
non-colour marker as well as colour (NFR-004).

### `ScheduleBlock` — F-C02

| Field | Type | Derivation |
|---|---|---|
| `taskId` | `TaskId` | `TaskView.id` |
| `title`, `priority`, `estimateMinutes`, `dueDate?`, `status` | from `TaskView` | copied |
| `schedule` | `ScheduleView` | `TaskView.schedule` (a scheduled task always has one) |
| `geometry` | `GridPosition` | `toGridPosition(schedule)` |
| `isCompleted` | `boolean` | `status === 'COMPLETED'` |

### `GridPosition` — F-C02 (pure geometry)

`{dayIndex: 0..6, startSlotIndex: 0..55, slotSpan: >=1}` over the 15-minute grid of the 08:00~22:00
window (56 slots/day). Pure functions only; no I/O, no cache access — this is the fast-check surface
for PBT-03/PBT-07.

### `BacklogItem` — F-C03

`TaskView` plus `{isOverdue, dueInDays?, sortKey}`. `isOverdue` compares `dueDate` with Asia/Seoul
today. `sortKey` mirrors the server's BR-032 order for a stable local render; when the server order
and the local order disagree, **the server order wins** and the local key is only a tie-break aid.

### `WeekCapacity` — F-C02 header (Q3 decision)

| Field | Type | Derivation |
|---|---|---|
| `windowMinutes` | `number` | `7 * 14 * 60 = 5880` (constant) |
| `plannedMinutes` | `number` | Σ `estimateMinutes` of `scheduled` where `status === 'TODO'` |
| `availableMinutes` | `number` | `windowMinutes - plannedMinutes`, floored at 0 |

Display-only. Derived from the already-rendered `WeeklyPlanView`, so it can never disagree with the
grid. Completed placements are excluded because they no longer consume plannable time. The figure
covers the whole displayed week and does not shrink as the clock advances — an elapsed-time variant
would need a "now" definition that no requirement provides.

### `TaskFormValues` — F-C04

`{title, description, priority, estimateMinutes, dueDate}` as form-shaped strings, plus
`expectedVersion` when editing. Converted to `TaskContentRequest` / `UpdateTaskRequest` at submit.

### `SlotProposal` — F-C05

`{taskId, date, startTime, estimateMinutes, endTimePreview, expectedVersion, source: 'drag' | 'keyboard' | 'form'}`.
`endTimePreview` is a preview label only; the server derives the authoritative end from the stored
estimate.

### `ConflictState` — F-C06

`{taskId, taskTitle, proposed, conflicting, nextCandidate?, expectedVersion, requestId}`, built
strictly from the 409 `ApiError.conflict`. It never contains the other task's content because the
contract does not send it.

### `SafeApiError` — F-C08

| Field | Type | Meaning |
|---|---|---|
| `kind` | `'validation' \| 'not-found' \| 'conflict' \| 'stale' \| 'rate-limited' \| 'network' \| 'unknown'` | normalized class |
| `code` | `string` | server code, or `'UNKNOWN'` when unrecognised |
| `message` | `string` | authored server message, or an authored client fallback |
| `requestId` | `string` | shown to the user, copyable |
| `fieldErrors` | `FieldError[]` | validation only |
| `currentVersion` | `number?` | `STALE_TASK` only |
| `conflict` | `ConflictView?` | `SCHEDULE_CONFLICT` only |
| `retryAfterSeconds` | `number?` | `Retry-After` on 429 |

An unrecognised code degrades to `kind: 'unknown'` with the server's message; it never throws and
never renders raw response text as markup (NFR-003).

## 3. Identity, Time and Correlation

- `TaskId` is the server UUID. The client never generates a task ID.
- `version` is opaque: read from the last server payload, echoed as `expectedVersion`, never
  incremented locally.
- Dates are `YYYY-MM-DD` and times `HH:mm` wall clock, Asia/Seoul. No `Date` object crosses the API
  boundary and no UTC conversion happens anywhere in U2.
- `RequestId` is client-generated per request in the `TMP-XXXX-XXXX-XXXX` shape and sent as
  `X-Request-Id`, so the value in an error message is exactly the value in the server log.

## 4. Relationships

```text
WeekContext 1 ── 7 DayColumn 1 ── * ScheduleBlock ──1 TaskView
WeekContext 1 ── 1 WeekCapacity        (derived from the same WeeklyPlanView)
WeeklyPlanView.backlog * ── 1 BacklogItem ──1 TaskView
SlotProposal 1 ──0..1 ConflictState    (created only from a 409 response)
TaskView 1 ──0..1 ScheduleView         (absent schedule ⇒ the task is in the backlog)
```

A `TaskView` appears in exactly one of `scheduled` or `backlog` for a given week, and the client
never derives one list from the other.
