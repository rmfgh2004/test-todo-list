# U2 Frontend Components

Component hierarchy, props, state, interaction flows, validation and API integration points for
F-C01~F-C09. Directory layout follows `unit-of-work.md` §Greenfield Code Organization.

## 1. Hierarchy

```text
AppShell                                   F-C01
├─ AppHeader                               F-C01
│  ├─ WeekNavigator (이전/다음/오늘, 주 범위)   F-C02
│  ├─ CapacityIndicator (남은 가용 시간)       F-C02
│  └─ ViewTabs (주간 | 목록)                  F-C01
├─ FeedbackProvider  (toast + aria-live)   F-C09
├─ ErrorBoundary                           F-C09
└─ Routes
   ├─ /  WeekPage
   │  ├─ BacklogPanel                      F-C03
   │  │  ├─ BacklogHeader (건수, 총 소요)
   │  │  ├─ BacklogCard[]  (drag source)
   │  │  └─ BacklogEmptyState
   │  ├─ TimetableGrid | DayTimeline        F-C02  (breakpoint)
   │  │  ├─ TimeAxis (08:00~22:00, 15분)
   │  │  ├─ DayColumn[7 | 1]
   │  │  │  ├─ DropSlot[56]                 F-C05
   │  │  │  └─ ScheduleBlock[]              F-C02
   │  │  └─ DragPreview (스냅 미리보기)        F-C05
   │  ├─ TaskEditorDialog                   F-C04
   │  ├─ ScheduleFormDialog (드래그 대체 수단)  F-C05
   │  ├─ ConflictDialog                     F-C06
   │  └─ DeleteConfirmDialog                F-C04
   └─ /tasks  TaskListPage                  F-C07
      ├─ TaskFilters (URL 동기화)
      ├─ TaskTable
      ├─ Pagination
      └─ ListEmptyState | NoResultsState
```

`shared/api` holds F-C08; `shared/ui` holds F-C09 primitives and the design tokens (light/dark).

## 2. Component Contracts

### AppShell / AppHeader (F-C01) — FR-002, FR-010, FR-011

- **Props**: none (route root).
- **State**: `theme` (system default, persisted), current route.
- **Provides**: QueryClient, FeedbackProvider, ErrorBoundary.
- **Interaction**: skip-to-content link; `ViewTabs` switches route without losing filter state.

### WeekNavigator (F-C02) — FR-002

- **Props**: `{weekStart, onSelectWeek(direction)}`.
- **Renders**: 이전 주 / 오늘 / 다음 주 buttons and the week range label (`2026-09-07 ~ 09-13`).
- **Rules**: UR-014 — a non-Monday URL value is normalized and the URL rewritten.
- **A11y**: buttons are real `<button>`s with accessible names; the range is a live label.

### CapacityIndicator (F-C02) — Q3

- **Props**: `{windowMinutes, plannedMinutes, availableMinutes}` from `WeekCapacity`.
- **Renders**: "이번 주 가용 시간 86h 15m 남음" plus a bar with a text equivalent.
- **Note**: the design mock shows "12h 30m", which no requirement defines. The approved definition
  (whole planning window minus incomplete placements) yields a larger figure, so the label is worded
  to match the value it actually shows.
- **Rules**: UR-023, UR-024 — display only, derived from the rendered week payload.

### TimetableGrid / DayTimeline (F-C02) — FR-001, FR-011

- **Props**: `{weekContext, blocks, onOpenTask, onMoveRequest, onUnschedule}`.
- **State**: none beyond derived geometry (`toGridPosition`).
- **Renders**: 56 rows/day at a 15-minute pitch; today and weekend columns carry an icon/label as
  well as colour (UR-016). Each `ScheduleBlock` renders a priority badge (icon + text + colour, not
  colour alone — UR-016) in addition to title and time range (Tempo Phase 1: this was previously only
  implemented on `BacklogCard`, not on placed blocks).
- **States**: skeleton / ready / empty / error, each visually distinct (UR-065); the skeleton state
  follows UR-066 (structural chrome first, data-only placeholders, 200ms/10s thresholds,
  `prefers-reduced-motion` support).
- **A11y**: the grid is a semantic table-like structure; each block is a focusable element exposing
  title, time range, priority and completion state as text.
- **Design tokens (Tempo Phase 1 alignment)** — canonical light/dark hex values `shared/ui` tokens
  must resolve to: text `#0f172a` / `#f1f5f9`, muted text `#475569` / `#cbd5e1`, border `#e2e8f0` /
  `#334155`, accent `#4f46e5` / `#818cf8`, danger `#b91c1c` / `#fca5a5`, warning `#a16207` / `#fcd34d`,
  success `#15803d` / `#86efac`. These replace ad hoc values already in `theme.ts`; no new token names.

### BacklogPanel / BacklogCard (F-C03) — FR-005

- **Props**: `{items, onOpenTask, onStartSchedule}`.
- **Renders**: header count + total duration; each card shows title, priority badge (icon + text),
  estimate and due date, with an overdue marker.
- **Rules**: UR-020~UR-022 — membership and order come from the server.
- **A11y**: each card is a listitem with a "시간 배치" button that opens `ScheduleFormDialog`, so the
  backlog is fully usable without dragging.

### TaskEditorDialog (F-C04) — FR-003, FR-004

- **Props**: `{mode: 'create' | 'edit', task?, onClose}`.
- **State**: `TaskFormValues`, `FieldErrors`, `isSubmitting`.
- **Validation**: UR-001~UR-006 before submit; server `fieldErrors` override local state (UR-008).
- **API**: `POST /api/v1/tasks` | `PATCH /api/v1/tasks/{id}` (full replacement with
  `expectedVersion`).
- **Edit specifics**: the form is pre-filled from the current task; clearing description or due date
  sends `null`; changing the estimate of a placed task may return 200, 400
  `SCHEDULE_OUT_OF_WINDOW` or 409 `SCHEDULE_CONFLICT`, each routed per §5 of the business logic model.
- **A11y**: `role="dialog"`, focus trap, Esc closes, focus returns to the trigger (UR-044).

### DeleteConfirmDialog (F-C04) — FR-004

- **Props**: `{task, onConfirm, onCancel}`; names the task explicitly.
- **API**: `DELETE /api/v1/tasks/{id}?expectedVersion&confirmed=true`. Never optimistic (UR-053).

### DropSlot / DragPreview / ScheduleFormDialog (F-C05) — FR-006, FR-008, NFR-004

- **Props**: `{taskId, estimateMinutes, expectedVersion, onProposal}`.
- **State**: the S-F03 machine — `idle | proposing | saving | conflict | stale | failed`.
- **Interaction paths** (all three produce the same `SlotProposal`):
  1. **Pointer** — drag from a card or block using dnd-kit's pointer sensor, snap to the 15-minute
     grid, drop. (Tempo Phase 1: replaces the native HTML5 `draggable`/`onDrop` implementation, which
     was a defect against the already-ratified `tech-stack-decisions.md` §1 dnd-kit decision.)
  2. **Keyboard** — dnd-kit's keyboard sensor: Space picks up, arrows move by one slot / one day,
     Enter confirms, Esc aborts, with a persistent hint: "Space로 집고 방향키로 이동, Enter로 확정".
  3. **Form** — date + start time inputs restricted to 00/15/30/45 within 08:00~22:00.
  While a proposal is pending, the header capacity preview updates per UR-025.
- **API**: `PUT /api/v1/tasks/{id}/schedule`, `DELETE /api/v1/tasks/{id}/schedule`.
- **Rules**: UR-030~UR-035; a preview whose end passes 22:00 is refused before any request.

### ConflictDialog (F-C06) — FR-007

- **Props**: `{conflictState, onKeepExisting, onMoveToCandidate, onCancel}`.
- **Renders**: proposed vs conflicting time ranges (times only, no other task's content) and the next
  candidate when present.
- **Actions**: exactly three — 기존 유지 / 다음 빈 시간으로 이동 / 취소 (UR-041). "겹쳐 두기" is not
  rendered in any form.
- **API**: the move action issues a fresh `PUT .../schedule` with
  `resolutionMode: ACCEPT_CANDIDATE`; a second 409 reopens the dialog with the new payload.
- **A11y**: focus trap, Esc = cancel, outcome announced via ARIA-live, focus returned to the origin.

### TaskFilters / TaskTable / Pagination (F-C07) — FR-009, FR-010

- **Props**: `{query, page, onQueryChange, onToggleCompletion}`.
- **State**: derived entirely from the URL — `status`, `scheduled`, `priority`, `sort`, `direction`,
  `page`, `size`.
- **Renders**: title, priority, estimate, due date, scheduled time and completion per row, grouped by
  due-date bucket (오늘 / 이번 주 / 완료— Tempo Phase 1, presentation-only: grouping is a client-side
  re-partition of the existing paginated result by `dueDate`/`status`, not a new query parameter or
  server capability) with a visible sort control surfacing the existing `sort`/`direction` URL state.
- **API**: `GET /api/v1/tasks`, `PUT /api/v1/tasks/{id}/completion`.
- **Rules**: unknown or out-of-bounds query values fall back to contract defaults rather than being
  forwarded; empty results and an empty dataset are separate states. Grouping never changes which rows
  the current page contains, only how they are visually partitioned.

### API Client (F-C08) — NFR-003, NFR-007, NFR-008

- `request<TResponse, TBody>(contract)` — sets `Content-Type: application/json`, generates
  `X-Request-Id` in the `TMP-XXXX-XXXX-XXXX` shape, and validates the response before it enters the
  cache.
- `normalizeApiError(error)` → `SafeApiError` per §11 of the business logic model, including
  `retryAfterSeconds` from the `Retry-After` header on 429.
- Types are generated from `backend/openapi/planning-api.yaml`; a drift fails the build. No manual
  transport type is permitted.
- Base URL comes from configuration and defaults to the loopback dev server.

### Feedback System (F-C09) — FR-011, NFR-004, NFR-008

- `<LiveRegion>` — one polite ARIA-live region for outcomes, one assertive region for conflicts and
  failures. One concise message per transition (UR-035).
- `<StatusSurface>` — loading skeletons, empty states, and error panels carrying the message, the
  copyable request ID (`TMP-7Q4K-2F9A-8C31`) and a retry control. The loading skeleton follows UR-066
  (structural chrome renders immediately; only server-dependent regions show placeholders).
- `<RetryButton>` — disabled until `Retry-After` elapses on a 429 (UR-061).
- `<ErrorBoundary>` — keeps the header and navigation usable when a subtree fails.
- `<RollbackToast>` (Tempo Phase 1, UR-067) — on a failed optimistic placement, names the restored
  position and the position that failed to save with its reason, does not auto-dismiss, and renders at
  least one concrete alternative-placement action. The corresponding `ScheduleBlock` at its restored
  position carries a non-colour "되돌림" marker.

## 3. User Interaction Flows

### Place a backlog task (FR-006)
Pick up (pointer / Space / "시간 배치" button) → preview snaps to the grid → confirm → optimistic
block + saving state → server 200 replaces it and announces the result. A 409 rolls back first, then
opens the conflict dialog.

### Resolve a conflict (FR-007)
Dialog compares the two ranges → the user keeps, moves or cancels → a move sends a new request that
is re-validated → the outcome is announced and focus returns to the originating card.

### Edit content (FR-004)
Open the editor from a card or a block → the form is pre-filled → submit replaces the full content
set → a resize of a placed task settles as success, an out-of-window field error, or a conflict.

### Mark complete (FR-009)
Toggle from the grid or the list → optimistic state → server payload settles both surfaces → a
failure restores the snapshot and announces the error with its request ID.

## 4. Form Validation Summary

| Field | Client rule | Server rule mirrored |
|---|---|---|
| title | required, trimmed, 1~120 | BR-001 |
| description | optional, ≤ 2,000, live counter | BR-002 |
| priority | one of LOW / MEDIUM / HIGH | BR-003 |
| estimateMinutes | 15~840, multiple of 15, stepper | BR-004 |
| dueDate | optional ISO date | BR-005 |
| schedule date | inside the displayed week | BR-030 |
| schedule startTime | 00/15/30/45, start ≥ 08:00, derived end ≤ 22:00 | BR-011, BR-012 |

## 5. API Integration Map

| Component | Endpoint | Method |
|---|---|---|
| F-C02, F-C03 | `/api/v1/planning/weeks/{weekStart}` | GET |
| F-C07 | `/api/v1/tasks` | GET |
| F-C04 | `/api/v1/tasks` | POST |
| F-C04 | `/api/v1/tasks/{id}` | PATCH |
| F-C04 | `/api/v1/tasks/{id}` | DELETE |
| F-C05, F-C06 | `/api/v1/tasks/{id}/schedule` | PUT |
| F-C05 | `/api/v1/tasks/{id}/schedule` | DELETE |
| F-C02, F-C07 | `/api/v1/tasks/{id}/completion` | PUT |

`/api/v1/health` is not consumed by the UI; it stays an operator endpoint.

## 6. Out of Scope (Q1, refined by business-rules.md §9)

Kanban board, multi-assignee capacity, tags, subtasks, comments, attachments, search, recurrence,
natural-language task entry and the command palette are not implemented and not stubbed in this
phase. The Tempo design screens' visual language is adopted only for the Phase 1 components listed
above; out-of-scope controls are removed, not disabled (UR-070~UR-072). See
`business-rules.md` §9 for the full Phase 2 backlog and why each item is deferred.
