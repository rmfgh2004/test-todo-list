# U2 Frontend Planning Experience Functional Design Plan

This plan covers Part 1 of the U2 Functional Design stage. Artifacts are generated only after every
question below is answered and every ambiguity is resolved.

## Part 1 Steps

- [x] Read the U2 unit definition, story map and owned components (F-C01~F-C09)
- [x] Read the approved U1 OpenAPI contract that U2 consumes
- [x] Review the 15 design reference screens in `aidlc-inputs/design/`
- [x] Identify conflicts between the design screens and the approved requirements
- [x] Collect answers to the questions below
- [x] Resolve any follow-up ambiguity
- [x] Generate the functional design artifacts

## Unit Context

- **Mission**: the local React planning experience that turns U1's authoritative API into a usable
  weekly planning workflow.
- **Application root**: `frontend/` under the workspace root.
- **Components**: F-C01 shell, F-C02 weekly planner, F-C03 backlog, F-C04 task editor,
  F-C05 scheduling interaction, F-C06 conflict resolution, F-C07 task list, F-C08 API client,
  F-C09 accessible feedback.
- **Primary requirements**: FR-001, FR-002, FR-005, FR-006, FR-007, FR-008, FR-010, FR-011;
  NFR-001~NFR-004, NFR-007, NFR-008.
- **Dependency**: U1 over local HTTP, through the checked-in `backend/openapi/planning-api.yaml`.
  U2 owns no business authority; every rule decision stays on the server.
- **Stack** (already fixed in `aidlc-inputs/01-tech-stack-decisions.md`): React 19.2 + Vite 8.2,
  TypeScript 5, TanStack Query, CSS Modules with design tokens, dnd-kit, Lucide icons,
  Vitest + React Testing Library + Playwright + fast-check.

## What the Design Screens Establish

These are taken as the visual and interaction language for the approved scope:

- Two-pane desktop layout: a left "미배치 할 일" backlog panel with a count and total duration header,
  and a Monday-to-Sunday grid from 08:00 to 22:00 with weekend columns visually distinguished.
- Backlog and schedule cards carry a priority badge, a duration and a due date.
- A header with previous/next/today controls, the week range, and a right-hand status area.
- Explicit states for loading, empty, error, rate limited, drag in progress, conflict and rollback.
- A drag preview with 15-minute snapping, gap hints, and a keyboard alternative
  ("Space로 집고 방향키로 이동, Enter로 확정").
- An ARIA-live region that announces conflicts and outcomes.
- Errors display a copyable reference number, for example `TMP-7Q4K-2F9A-8C31`.
- Light and dark variants of every screen.

## Conflicts Between the Screens and the Approved Requirements

| # | Design screen shows | Approved requirement says | Needs a decision |
|---|---|---|---|
| 1 | Board (kanban) view, assignee, tags, subtasks, comments, search, recurrence, natural-language task entry | FR-001~FR-013 contain none of these; `01-tech-stack-decisions.md` §7 explicitly rejected the kanban board as scope expansion | Q1 |
| 2 | Conflict dialog offers "겹쳐 두기" (keep the overlap) | FR-007 allows only keep-existing, move-to-next-slot or cancel; the U1 API cannot store an overlap | Q2 |
| 3 | "이번 주 남은 가용 시간 12h 30m" capacity indicator | No requirement defines available time | Q3 |
| 4 | A queue of pending saves that auto-retries after a rate limit | NFR-004 requires rollback and notification on failure, but no queue | Q4 |

## Questions

### Q1 — Scope of the first release

Which of the design screens' extra capabilities should U2 implement?

[Answer]: **Approved scope only.** U2 implements the weekly timetable, the unscheduled backlog, drag
placement, conflict resolution and the list view. The kanban board, assignee, tags, subtasks,
comments, search, recurrence and natural-language entry are out of scope. Rationale:
`01-tech-stack-decisions.md` §7 already rejected the board as scope expansion, requirements §6
lists these as out of scope, and the U1 API carries no field for any of them, so building them
would force a reopen of the approved and completed U1 contract.

### Q2 — Conflict resolution choices

Should the conflict UI keep the design's "겹쳐 두기" option, which the server cannot honour?

[Answer]: **Remove it.** The conflict dialog exposes exactly the three FR-007 outcomes: keep the
existing placement, move to the offered next candidate, or cancel. Rationale: the server rejects an
overlap with 409 and stores nothing, so a fourth button could only ever fail. No disabled control is
rendered either — a dead button is noise, not information.

### Q3 — Available time indicator

Should the header show the remaining available time, and how is it defined?

[Answer]: **Show it, computed on the client as a display-only derived value.** Definition:
`availableMinutes = 98 * 60 - sum(estimateMinutes of incomplete scheduled tasks in the displayed
week)`, where 98h is the planning window (Mon~Sun × 08:00~22:00 = 14h × 7). It is recomputed from the
same `WeeklyPlanView` payload the grid already renders, so it needs no new endpoint and cannot
disagree with the grid. It authorises nothing — placement remains a server decision.

### Q4 — Behaviour when a mutation is rate limited

Should a rejected mutation be queued and retried automatically, or rolled back immediately?

[Answer]: **Roll back immediately and offer an explicit retry.** On 429 the optimistic cache update
is reverted from the captured snapshot, the ARIA-live region announces the failure, and the feedback
surface shows the request ID plus a "다시 시도" control (disabled until `Retry-After` elapses). No
pending-save queue exists. Rationale: NFR-004 requires rollback and notification, and a queue would
let the visible state disagree with the server while inventing queue ordering and re-conflict rules
that no approved requirement defines.

## Decisions Already Fixed Without a Question

- **Request correlation.** The client generates its own opaque ID in the design's
  `TMP-XXXX-XXXX-XXXX` shape and sends it as `X-Request-Id`. It matches the server's accepted format
  `^[A-Za-z0-9._-]{8,64}$`, so the value shown to a user in an error is exactly the value in the
  server log. No backend change is needed.
- **No business authority in the client.** Time geometry may be mirrored for display and for a drag
  preview, but every placement, conflict and completion decision is the server's. A rejected optimistic
  update always rolls back to the server state.
- **Types come from the contract.** Request and response types are generated from
  `backend/openapi/planning-api.yaml`, and a drift between the generated types and the checked-in
  contract fails the build.
