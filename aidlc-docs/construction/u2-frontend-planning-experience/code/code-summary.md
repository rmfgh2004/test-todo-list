# U2 Frontend Planning Experience Code Summary

## Outcome

U2 is complete as a local React/Vite planning client backed by the real U1 OpenAPI contract. It
provides a responsive weekly/day timetable, backlog, task create/update/delete, pointer/keyboard/form
scheduling, conflict resolution, unscheduling, completion, URL-driven list filtering, safe error
feedback and light/dark themes.

## Structure

| Area            | Main paths                                  | Responsibility                                                       |
| --------------- | ------------------------------------------- | -------------------------------------------------------------------- |
| App shell       | `frontend/src/app/`                         | Layout, routes, feedback ownership and feature composition           |
| Feature slices  | `frontend/src/features/`                    | Backlog, timetable, task editor, conflict and list UI/hooks          |
| Transport/cache | `frontend/src/shared/api/`                  | Generated-contract client, validation, cache and mutation policy     |
| Pure core       | `frontend/src/shared/time/`, `shared/grid/` | Seoul dates, 15-minute geometry and capacity                         |
| Shared UI       | `frontend/src/shared/ui/`                   | Tokens, theme, status, error boundary, dialog focus and live regions |
| Gates           | `frontend/scripts/`, `frontend/tests/`      | Drift, bundle, capacity, Playwright and test generators              |

## Key decisions preserved

- U1 remains authoritative; successful optimistic operations replace local values with its payload.
- GET queries retry at most twice; mutations never retry automatically.
- Transport loss alone raises disconnected state; HTTP errors remain explicit server outcomes.
- The 56x7 grid keeps stable slot identity and renders 1,000 task blocks under the 300ms gate.
- Transport types are generated from `backend/openapi/planning-api.yaml` and drift-checked.
- The production build excludes mock transport, source maps, external assets and unsafe HTML sinks.

## Final evidence

`npm run verify`, `npm run test:e2e`, `npm run test:e2e:journey`, `npm run test:capacity`,
`npm run audit:deps` and `npm run sbom` all pass. The Tempo Phase 1 production bundle is 99.1KB gzip
against a 250KB ceiling.

## Tempo Design Rework — Phase 1

- Applied the imported Tempo colour values while retaining the semantic token API; every changed
  foreground/background pair passes WCAG AA (minimum measured ratio 4.92:1).
- Added one shared icon/colour/text priority badge across backlog, timetable and list rows.
- Replaced native HTML drag/drop with dnd-kit pointer and keyboard sensors, droppable 15-minute slots,
  live capacity delta preview and unchanged Space/arrows/Enter/Escape keyboard semantics.
- Added 200ms-delayed, reduced-motion-safe structural skeletons and a 10-second timeout transition.
- Connected optimistic scheduling outcomes to persistent structured rollback feedback and a
  non-colour timetable rollback marker.
- Grouped the already-fetched task-list page into 오늘/이번 주/완료 without changing its query or the
  U1 OpenAPI contract.
- Verification: 22 files / 108 tests; 85.37% statements, 80.58% branches, 81.09% functions, 87.73%
  lines; desktop/mobile real-U1 E2E 10/10; A–Z journey 1/1; 1,000-task capacity 1/1; dependency audit
  0 vulnerabilities; contract drift clean.
