# U2 Frontend Planning Experience — Tempo Rework Phase 1 Code Generation Plan

This plan amends (not replaces) `u2-frontend-planning-experience-code-generation-plan.md`. It is the
single source of truth for the Tempo Rework Phase 1 changes only. Nothing is generated that is not
written here, and each step is marked `[x]` in the same interaction in which it completes.

**Approved inputs**: `functional-design/business-rules.md` §3 (UR-025), §7 (UR-066, UR-067), §9 (Tempo
Design Rework — Phase 1/Phase 2 boundary); `functional-design/frontend-components.md` (F-C02, F-C05,
F-C07, F-C09 updates); `nfr-requirements/nfr-requirements.md` (reduced-motion row);
`nfr-requirements/tech-stack-decisions.md` (dnd-kit remediation note); `aidlc-inputs/design/tempo/*`.

No U1 contract change (UR-072 holds — `dueDate` and `priority` needed below already exist on
`TaskView`/`TaskPageView`). No new npm dependency — `@dnd-kit/core` and `@dnd-kit/utilities` are
already in `frontend/package.json` (declared, currently unused).

## Part 1 Approval

- [x] Functional design delta (business-rules.md §3/§7/§9, frontend-components.md) analyzed
- [x] Current implementation read end-to-end for every touched file (see file paths per step)
- [x] Exact generation paths documented (all under `frontend/`, never `aidlc-docs/`)
- [x] No new dependency, no U1 contract change confirmed
- [x] Entire generation sequence below approved by user ("진행하자", 2026-09-03T21:53:13+09:00)

## Unit Context

- **Unit**: U2 Frontend Planning Experience (amendment)
- **Code location**: `frontend/src/` (existing tree, no new top-level directories)
- **Documentation location**: `aidlc-docs/construction/u2-frontend-planning-experience/code/`
- **Scope**: Tempo artboards 1a, 1b, 1c, 2a, 3a, 3c, 3e only. 1d, 2b–2f, 3d and multi-assignee
  capacity are explicitly out of scope (business-rules.md §9).
- **Depends on**: nothing new; same U1 contract as the existing plan.

---

## Step 1 — Design Tokens (F-C09 note, business-rules.md §9)

- [x] `frontend/src/shared/ui/tokens.css`: replace the light/dark hex values with the Tempo-specified
      values while keeping the existing variable names (no renames, no new tokens): `--color-text`
      `#0f172a`/`#f1f5f9`, `--color-text-muted` `#475569`/`#cbd5e1`, `--color-border` `#e2e8f0`/`#334155`,
      `--color-accent` `#4f46e5`/`#818cf8`, `--color-danger-text` `#b91c1c`/`#fca5a5`, `--color-warning`
      `#a16207`/`#fcd34d`, `--color-success` `#15803d`/`#86efac`. `--color-canvas`, `--color-surface*`,
      `--color-sidebar`, `--color-grid`, `--color-accent-soft`, `--color-danger-border`, `--radius-*`,
      `--shadow-lg` are unchanged (Tempo doesn't specify them; no requirement to touch them).
- [x] Re-run the existing WCAG AA contrast check referenced in the original plan's Q1 answer for every
      changed pair; adjust only if a pair fails (structure-faithful, token-derived policy carries over).

## Step 2 — Priority Badge Primitive (UR-016, F-C02, F-C03)

- [x] `frontend/src/shared/ui/` : add `priority-badge.tsx` exporting `<PriorityBadge priority="LOW" |
"MEDIUM" | "HIGH" />` — renders icon (▲/●/▼) + colour (danger/warning/muted) + Korean text (높음/
      보통/낮음), i.e. the triple encoding UR-016 already requires. One shared component, no duplication.
- [x] `frontend/src/features/backlog/BacklogPanel.tsx`: replace the plain `<span className={styles.badge}>{priorityLabel[...]}</span>`
      (line 58) with `<PriorityBadge priority={task.priority} />`.
- [x] `frontend/src/features/timetable/components.tsx`: add `priority: 'LOW' | 'MEDIUM' | 'HIGH'` to
      `TimetableBlockModel` (currently id/title/date/startTime/endTime only); render `<PriorityBadge>`
      inside each `ScheduleBlock` button alongside title/time.
- [x] `frontend/src/app/LiveWorkspace.tsx`: the block-mapping spread that builds `TimetableGrid`'s
      `blocks` prop (`{ id: task.id, title: task.title, ...task.schedule }`) adds `priority: task.priority`
      — `TaskView.priority` already exists on the fetched data, no new query.

## Step 3 — dnd-kit Migration (UR-030~UR-035, tech-stack-decisions.md remediation)

- [x] `frontend/src/features/timetable/dnd.ts` (new): a small `DndContext` wrapper configured with
      `PointerSensor` and `KeyboardSensor` from `@dnd-kit/core`, replacing ad hoc native drag handling.
- [x] `frontend/src/features/backlog/BacklogPanel.tsx`: replace `<li draggable onDragStart=...>` (lines
      40-48) with `useDraggable` from `@dnd-kit/core`; same `taskId` payload, same visual card.
- [x] `frontend/src/features/timetable/components.tsx`: replace `Slot`'s `onDragOver`/`onDrop` (lines
      110-117) with `useDroppable`; the drop callback keeps producing the same `SlotProposalModel`.
- [x] `frontend/src/features/timetable/SchedulingInteraction.tsx`: replace the `draggable`/`onDragEnd`
      button (lines 49-79) with a dnd-kit-sensor-driven equivalent that preserves the exact same keyboard
      contract byte-for-byte (Space pick up, arrows move by slot/day, Enter confirm, Esc abort, same hint
      text) — this is a transport-layer swap, not a UX change.
- [x] `frontend/src/app/LiveWorkspace.tsx`: wrap the `<aside>`/`<section className={styles.planner}>`
      pair in the Step 3 `DndContext`; `draggingId` state and the `onSlotProposal`/`onDragStart` callback
      wiring keep their current signatures so `use-schedule-commands.ts` needs no change.
- [x] Delete now-dead native drag-event code once the dnd-kit path is confirmed equivalent — no leftover
      duplicate handlers.

## Step 4 — Capacity Delta Preview (UR-025)

- [x] `frontend/src/features/timetable/components.tsx`: lift `TimetableGrid`'s local `preview` state
      (currently `useState` inside the component, line 134) by adding an optional `onPreviewChange`
      prop that fires whenever `setPreview` does; keep the internal preview-layer rendering unchanged.
- [x] `frontend/src/app/LiveWorkspace.tsx`: track the active preview in `LiveWeeklyWorkspace`, resolve
      the relevant task's `estimateMinutes` (from `schedulingTask` or the `draggingId` lookup), and pass a
      computed delta into `CapacityIndicator`.
- [x] `frontend/src/features/timetable/components.tsx`: extend `CapacityIndicator` with an optional
      `previewDeltaMinutes` prop; when present, render the "(−1h 30m)" style suffix per Tempo 1c. Purely
      additive prop — no change to existing callers without a preview.
- [x] Confirm the preview value is never sent in any mutation payload (UR-025's core invariant).

## Step 5 — Loading Skeleton Fidelity (UR-066)

- [x] `frontend/src/app/LiveWorkspace.tsx`: `LiveWeeklyWorkspace`'s `week.isPending` branch currently
      replaces the entire page with `<LoadingSurface />` (lines 89-94). Change it to render the same
      structural chrome (`WeekNavigator`, `CapacityIndicator` shell, `TimetableGrid` axis/headers,
      `BacklogPanel` header) with skeleton placeholders only where `blocks`/`tasks` content would go. Use
      TanStack Query's `placeholderData` (previous week's shape) where available so placeholder geometry
      matches the last render, per the Tempo spec's no-jump requirement.
- [x] `frontend/src/shared/ui/status-surface.tsx`: replace the unconditional 3-dot `LoadingSurface`
      with a skeleton-block primitive usable by the grid/backlog; add a 200ms show-delay (no flash on a
      fast response) and treat a request exceeding 10s as an error-state transition (reuses
      `ErrorStatusSurface`, no new error type).
- [x] `frontend/src/shared/ui/status-surface.module.css`: add the shimmer keyframe plus a
      `@media (prefers-reduced-motion: reduce)` override that shows static placeholders with no animation.

## Step 6 — Rollback Visualization & Feedback Wiring (UR-067, UR-035)

- [x] `frontend/src/shared/ui/feedback.tsx`: `announceOutcome`/`announceFailure` are currently declared
      but never called anywhere in the app — wire them. Extend the failure channel to accept structured
      content (restored position, failed position, reason, one action) instead of a plain string; add an
      explicit dismiss control (the current implementation has no way to clear a message once set).
- [x] `frontend/src/features/timetable/use-schedule-commands.ts`: on a successful `schedule`/`unschedule`
      call `announceOutcome`; on a caught rollback (the `rollback` branch already exists in
      `mutation-coordinator.ts`) call the new structured `announceFailure` with the restored task's position
      and the attempted position that failed, plus one concrete alternative action.
- [x] `frontend/src/app/LiveWorkspace.tsx`: track a short-lived "recently reverted" task id (cleared on
      the next successful interaction with that task) so `TimetableGrid` can render the non-colour
      "되돌림" marker on the affected block.
- [x] `frontend/src/features/timetable/components.tsx`: `ScheduleBlock` renders the "↩ 되돌림" marker
      when its task id matches the reverted-id passed down.

## Step 7 — List View Due-Date Grouping (2a, presentation-only, no query change)

- [x] `frontend/src/features/task-list/TaskListPage.tsx`: add `dueDate?: string` to
      `TaskListItemModel` (the field already exists on the fetched `TaskView`/`TaskPageView.content`
      returned by `GET /api/v1/tasks` — see `planning-api.d.ts` `TaskView.dueDate` — it is simply not on
      this narrower view-model type yet).
- [x] Same file: partition the already-fetched, already-paginated `tasks` array into 오늘 / 이번 주 /
      완료 groups (client-side, by `dueDate`/`status`) and render each as a labelled section instead of one
      flat `<table>`. The current page's row set, sort and pagination controls are unchanged — grouping is
      a visual re-partition only, matching UR rule in business-rules.md §9 ("no new query parameter").
- [x] Reuse the Step 2 `<PriorityBadge>` in each row for visual consistency with the backlog/grid.

## Step 8 — Test Updates (NFR-001 coverage floor, UR test-naming convention)

- [x] `frontend/src/features/backlog/backlog.test.tsx`: badge renders icon+colour+text; drag source is
      a dnd-kit draggable.
- [x] `frontend/src/features/timetable/timetable.test.tsx` / `scheduling.test.tsx`: block renders
      priority badge and (when applicable) the reverted marker; keyboard journey (Space/arrows/Enter/Esc)
      still passes byte-for-byte after the dnd-kit swap (`UR_030`); capacity preview delta appears and
      disappears correctly and is never part of a request body (`UR_025`).
- [x] `frontend/src/shared/ui/feedback.test.tsx`: structured failure content renders both positions,
      the reason and the action; message persists until explicit dismissal (`UR_067`).
- [x] `frontend/src/app/LiveWorkspace.test.tsx`: coordinator logic remains covered unchanged; its
      consuming schedule-command integration asserts that rollback/success invoke structured failure and
      outcome feedback respectively.
- [x] `frontend/src/features/task-list/task-list.test.tsx`: grouping renders three sections with the
      correct membership from a fixed fixture; page/sort/filter URL state unchanged (`UR_070` scope guard
      — no new query param leaks in).
- [x] New: a skeleton-timing test asserting no skeleton before 200ms and an error transition at 10s
      (`UR_066`); a `prefers-reduced-motion` test asserting no shimmer animation.

## Step 9 — Accessibility, PBT and E2E Spot-Checks (NFR-004)

- [x] Re-run `axe-core` assertions on the changed surfaces (grid, backlog, list, loading skeleton,
      feedback toast) — serious/critical violations block, per the existing gate.
- [x] Playwright desktop + 320px keyboard journey re-verified against the dnd-kit-based
      `SchedulingInteraction` (same script, no new journey needed — this proves the swap preserved UR-030).
- [x] fast-check properties (PBT-03) re-run unchanged — grid geometry invariants are untouched by any
      Phase 1 step.

## Step 10 — Documentation

- [x] Update `aidlc-docs/construction/u2-frontend-planning-experience/code/code-summary.md` with the
      Phase 1 changes (new/modified files per step above).
- [x] Update `aidlc-docs/construction/u2-frontend-planning-experience/code/traceability.md` with
      UR-025/UR-066/UR-067 → component/test mappings.

---

## Completion Criteria

- All steps above marked `[x]`.
- `npm run verify` passes (type-check, lint, format, unit/component tests + 80/75 coverage gate,
  build + 250KB bundle ceiling — unchanged gates from the existing plan).
- `npm run test:e2e` passes against a real U1 instance.
- No U1 contract diff. No new dependency beyond the already-declared `@dnd-kit/*` packages actually
  being used for the first time.
- Ready for the Build and Test re-run (`aidlc-docs/aidlc-state.md` → U2 Tempo Design Rework checklist).
