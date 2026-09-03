# U2 Frontend Planning Experience Traceability

## Story and checkpoint coverage

| Stories    | Primary implementation                                                        | Verification                         |
| ---------- | ----------------------------------------------------------------------------- | ------------------------------------ |
| US-001~003 | App workspace, backlog, task editor and task commands                         | CP-02 plus live MSW journeys         |
| US-004~006 | Timetable, scheduling interaction, schedule commands and conflict dialog      | CP-03 desktop/320px real-U1 journeys |
| US-007~008 | Completion command and URL task list                                          | CP-04 desktop/320px real-U1 journeys |
| US-009     | Tokens, responsive layout, dialog focus, keyboard scheduling and live regions | CP-05 axe/mobile plus NFR-004 tests  |
| US-010     | Safe API error, request ID and connectivity monitor                           | CP-05 transport-loss recovery        |

## Requirement coverage

| IDs         | Code evidence                                               | Test/gate evidence                           |
| ----------- | ----------------------------------------------------------- | -------------------------------------------- |
| FR-001~002  | `shared/time`, `shared/grid`, timetable hooks/components    | calendar/geometry/timetable tests, CP-02     |
| FR-003~005  | task editor/commands and backlog                            | editor/backlog/live-workspace tests, CP-02   |
| FR-006~008  | scheduling, conflict and cache rollback                     | scheduling/conflict/cache tests, CP-03       |
| FR-009~010  | completion and task list/query allowlist                    | live-workspace/task-list/query tests, CP-04  |
| FR-011      | app shell, theme, feedback and dialog focus                 | App/theme/feedback tests, CP-05 axe/mobile   |
| NFR-001~002 | Vitest coverage and fast-check pure-core suite              | 108 tests; PBT fixed seed and shrinking      |
| NFR-003~004 | injection lint, safe rendering, focus/keyboard/live regions | lint plus component and browser axe gates    |
| NFR-005     | scoped cache, stable grid and bundle script                 | one-request E2E, capacity gate, 99.1KB gzip  |
| NFR-006     | mutation coordinator and authoritative replacement          | cache/coordinator/live integration and CP-03 |
| NFR-007     | strict TS, import boundaries and generated types            | typecheck, lint and contract drift           |
| NFR-008     | safe errors, route boundary and connectivity monitor        | error/connectivity tests and CP-05 recovery  |

## Extension coverage

- SECURITY-01~15: final results are recorded in `security-review.md`; all applicable items pass and
  inapplicable infrastructure/identity items have explicit rationale.
- PBT-02/03/07/08/09: calendar and grid property suites use reusable constrained generators,
  fast-check shrinking and fixed seed `20260901` inside the normal Vitest lifecycle.
- PBT-01/04~06/10 are advisory in the approved partial mode.

The final scan found no `_modified`, `_new` or copy files. All wire request/response names outside the
generated declaration are aliases of generated `components['schemas']`; `ApiRequest<T>` is an
internal generic fetch instruction, not a handwritten wire schema.

## Tempo Phase 1 delta coverage

| IDs            | Code evidence                                                        | Test/gate evidence                                      |
| -------------- | -------------------------------------------------------------------- | ------------------------------------------------------- |
| UR-016, UR-025 | `priority-badge`, timetable capacity preview, `LiveWorkspace` wiring | backlog/timetable tests; request-body integration check |
| UR-030~035     | dnd-kit context, draggable backlog/scheduler and droppable slots     | dnd/scheduling tests; desktop/mobile pointer+keyboard   |
| UR-066         | structural `LoadingSurface`, 200ms/10s timers, reduced-motion CSS    | fake-timer and CSS policy tests                         |
| UR-067         | schedule-command feedback, persistent toast and rollback marker      | feedback and live-workspace rollback/success tests      |
| UR-070 scope   | client-only 오늘/이번 주/완료 partition on the fetched page          | grouping test plus unchanged query serialization        |
| NFR-004~005    | accessible labels, stable 56x7 grid, bounded production bundle       | axe E2E 10/10, capacity 1/1, bundle 99.1KB/250KB        |
