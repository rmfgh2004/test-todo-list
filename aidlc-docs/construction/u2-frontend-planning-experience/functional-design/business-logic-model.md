# U2 Business Logic Model

U2's logic is **coordination logic**: turning a user gesture into one authorised server command, and
turning the server's answer back into a truthful screen. It holds no business authority.

## 1. Mutation Processing Template

Every mutation follows the same seven steps (mirror of U1's command template, from the client side).

1. Validate locally against the `UR-0xx` rules; on failure show field errors and send nothing.
2. Build the typed request from generated contract types, echoing the last known `version` as
   `expectedVersion` (UR-050).
3. Capture a rollback snapshot of every cache key the mutation can touch (UR-032).
4. Apply the optimistic update — except for delete, which waits for the server (UR-053).
5. Send exactly one request with a fresh `X-Request-Id`.
6. On success, replace the affected cache entries with the server payload — never merge the local
   guess with the server answer.
7. On failure, restore the snapshot, classify the error, and route it to the matching surface
   (field errors → form, `SCHEDULE_CONFLICT` → conflict dialog, `STALE_TASK` → refetch,
   everything else → feedback with request ID).

## 2. Query Model and Cache Keys

| Key | Source | Invalidated by |
|---|---|---|
| `['week', weekStart]` | `GET /api/v1/planning/weeks/{weekStart}` | every task mutation touching that week |
| `['tasks', serializedQuery]` | `GET /api/v1/tasks` | create, update, delete, completion, schedule, unschedule |
| `['task', taskId]` | `GET /api/v1/tasks/{id}` | any mutation on that task |

Rules:
- One weekly request serves both the grid and the backlog; the backlog is never fetched separately.
- A mutation invalidates only the affected week key plus list keys — never the whole cache (NFR-005).
- A schedule change that moves a task across a week boundary invalidates **both** week keys.
- Queries retry at most twice with backoff; mutations never retry automatically (UR-062).

## 3. Week Navigation Flow (FR-001, FR-002)

### Input
URL `/(week)?week=YYYY-MM-DD`, or a previous / next / today control.

### Transformation
1. Parse `?week=`; if absent, use the Asia/Seoul current week's Monday.
2. `selectWeek(current, direction)` → previous = `-7d`, next = `+7d`, today = current week's Monday.
3. If the value is not a Monday, normalize to that week's Monday and rewrite the URL (UR-014) so the
   API is never called with a value it would reject with `WEEK_START_INVALID`.
4. Fetch `['week', weekStart]`; build `WeekContext`, seven `DayColumn`s, `ScheduleBlock`s via
   `toGridPosition`, `BacklogItem`s and `WeekCapacity`.

### States
`loading` (skeleton grid, controls enabled) → `ready` | `empty` (week has no placement and no
backlog) | `error` (message + request ID + retry). A refresh restores the identical screen from the
URL alone.

### Responsive branch (FR-011)
Desktop renders the backlog panel and the 7-day grid together. Below the breakpoint the layout
becomes a date selector + single-day timeline + a separate backlog panel; every command stays
reachable at 320px.

## 4. Create Task Flow (FR-003)

1. F-C04 opens the create dialog with focus on the title field.
2. `validateTaskForm` applies UR-001~UR-005; invalid submit focuses the first bad field.
3. `POST /api/v1/tasks` with `TaskContentRequest`.
4. 201 → the new task is unscheduled; invalidate the week and list keys, announce success, close the
   dialog, return focus to the trigger.
5. 400 → map `fieldErrors` onto form fields (UR-008); the dialog stays open with the user's input.

## 5. Update and Delete Flow (FR-004)

### Update — full replacement
The edit form is pre-filled from the current `TaskView`, so submitting an untouched form is a no-op
replacement rather than a data loss. A cleared `description` or `dueDate` sends `null` and clears the
stored value (UR-006). `PATCH` returns the full task; the cache is replaced from it.

If `estimateMinutes` changed on an already-placed task, the server keeps the start and recomputes the
end. U2 handles the three possible answers without predicting which one comes back:
- 200 → the returned `schedule` replaces the block; its span changes.
- 400 `SCHEDULE_OUT_OF_WINDOW` → field error on the estimate, nothing stored.
- 409 `SCHEDULE_CONFLICT` → the conflict dialog opens for the resized placement, nothing stored.

### Delete
Explicit confirmation dialog naming the task → `DELETE ?expectedVersion&confirmed=true` → 204 →
invalidate week and list keys. Never optimistic (UR-053).

## 6. Scheduling Flow (FR-006) — S-F03 state machine

```text
idle ──pick up (drag | Space | form open)──> proposing
proposing ──drop / Enter / submit──> saving        (snapshot captured, optimistic block shown)
proposing ──Esc / invalid preview──> idle          (no request sent)
saving ──200──> scheduled ──> idle                 (server payload replaces the cache)
saving ──409 SCHEDULE_CONFLICT──> conflict         (snapshot restored first)
saving ──409 STALE_TASK──> stale ──refetch──> idle
saving ──400 | 429 | network──> failed ──> idle    (snapshot restored, retry offered)
conflict ──keep existing | cancel──> idle
conflict ──move to candidate──> saving             (a new full request, re-validated)
```

`toSlotProposal(drop, estimateMinutes)` snaps to the 15-minute grid and computes a **preview** end
for the drag ghost only. The request carries `{date, startTime, expectedVersion}`; the server derives
the real end (UR-031). If the previewed end passes 22:00 the drop is refused locally and no request
is sent (UR-013) — this is a UX shortcut, and the server enforces the same rule regardless.

Each transition announces one concise ARIA-live message, e.g. "화요일 10:00에 배치했습니다" or
"충돌이 있어 배치하지 않았습니다".

## 7. Conflict Resolution Flow (FR-007)

1. The 409 body yields `ConflictState` from `ApiError.conflict` (times only).
2. The optimistic block is rolled back **before** the dialog renders (UR-040), so the screen already
   matches the server when the user reads it.
3. The dialog compares the proposed slot with the conflicting slot and offers:
   - **기존 유지** — close, keep the current server state.
   - **다음 빈 시간으로 이동** — a fresh schedule request at `nextCandidate` with
     `resolutionMode: ACCEPT_CANDIDATE`; a second conflict reopens the dialog with the new payload.
   - **취소** — close with no request.
   When `nextCandidate` is absent the move action is not rendered (UR-042).
4. Focus is trapped, Esc cancels, and focus returns to the originating card (UR-044).

## 8. Unschedule Flow (FR-008)

Command from the block's menu or the keyboard path → snapshot → optimistic removal from the grid →
`DELETE /api/v1/tasks/{id}/schedule?expectedVersion` → 200 replaces both grid and backlog from the
returned task. Idempotent: repeating it on an already-unscheduled task is still a 200 and changes
nothing visible.

## 9. Completion Flow (FR-009)

`PUT /api/v1/tasks/{id}/completion` with the desired final state. Optimistic with snapshot rollback.
The same task may be visible in the grid and the list at once, so success invalidates every affected
key and both surfaces settle on the server payload — a completed placement stays on the grid, marked
complete with a non-colour cue, and leaves the backlog.

## 10. Task List Flow (FR-010)

`parseTaskListQuery(URLSearchParams)` reads only allowlisted keys — `status`, `scheduled`,
`priority`, `sort`, `direction`, `page`, `size` — and drops anything else. Values outside the
contract's enums or bounds fall back to the documented defaults (`sort=CREATED_AT`,
`direction=DESC`, `page=0`, `size=25`) instead of being sent to the API.
`serializeTaskListQuery` writes the same keys back to the URL, so filter and sort state survives a
refresh and a shared link. Empty results and a genuinely empty dataset are distinct states (FR-010).

## 11. Failure Classification (F-C08, F-C09)

| HTTP / condition | `SafeApiError.kind` | Surface |
|---|---|---|
| 400 with `fieldErrors` | `validation` | form field errors |
| 400 without field errors | `validation` | feedback with message + request ID |
| 404 `TASK_NOT_FOUND` | `not-found` | remove from cache, announce that the task no longer exists |
| 409 `SCHEDULE_CONFLICT` | `conflict` | conflict dialog |
| 409 `STALE_TASK` | `stale` | refetch, discard the local edit, announce the external change |
| 429 `RATE_LIMITED` | `rate-limited` | rollback + retry disabled until `Retry-After` (Q4) |
| 5xx / malformed / offline | `network` \| `unknown` | feedback with request ID + retry |

Classification is driven by status plus the `code` enum. An unrecognised code degrades to `unknown`
and still renders the authored message and request ID — it never throws and never leaks a body.

## 12. Contract Observation Raised During Design

The backend returns **429 with `code: "RATE_LIMITED"` and a `Retry-After` header**
(`platform/RateLimitFilter.java`), but `backend/openapi/planning-api.yaml` documents no 429 response
and omits `RATE_LIMITED` from the `ApiError.code` enum. The existing drift test compares path/method
sets only, so it does not catch this.

U2 needs no U1 behaviour change: the client handles the real 429 as specified above, and the
`unknown`-code fallback keeps it safe. The gap is a **documentation** defect in an approved artifact.
Recommendation: add the 429 response and the `RATE_LIMITED` code to the contract as an additive
documentation fix during U2 code generation, when the generated types are first produced. Raised
here rather than fixed silently, since U1 is an approved unit.

## 13. Traceability

| Requirement | Flow | Components |
|---|---|---|
| FR-001, FR-002 | §3 | F-C01, F-C02, F-C09 |
| FR-003 | §4 | F-C04, F-C08, F-C09 |
| FR-004 | §5 | F-C04, F-C08, F-C09 |
| FR-005 | §3 | F-C03 |
| FR-006 | §6 | F-C02, F-C03, F-C05 |
| FR-007 | §7 | F-C05, F-C06, F-C09 |
| FR-008 | §8 | F-C02, F-C03, F-C05 |
| FR-009 | §9 | F-C02, F-C07 |
| FR-010 | §10 | F-C01, F-C07 |
| FR-011 | §3, §7 | F-C01, F-C02, F-C06, F-C09 |
| NFR-004 | §6, §7, §11 | F-C05, F-C06, F-C09 |
| NFR-006 | §1, §6 | F-C05, F-C08 |
| NFR-007 | §2, §11 | F-C08 |
| NFR-008 | §11 | F-C08, F-C09 |
