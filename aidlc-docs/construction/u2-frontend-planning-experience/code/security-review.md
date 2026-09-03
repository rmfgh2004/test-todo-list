# U2 Frontend Planning Experience Security Review

## Code Generation Steps 3~4 — Pure Time and Grid Core

- **Reviewed at**: 2026-09-01 01:20:50 (KST)
- **Changed application files**: `frontend/src/shared/time/calendar.ts`,
  `frontend/src/shared/grid/geometry.ts`, `frontend/src/shared/grid/capacity.ts`
- **Test support**: three test files and `frontend/tests/generators/planning.ts`
- **Stable IDs**: FR-001, FR-002, FR-006, NFR-002, UR-010~~014, UR-023~~024,
  PBT-02, PBT-03, PBT-07, PBT-08, PBT-09
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule        | Result | Evidence or N/A rationale                                                                         |
| ----------- | ------ | ------------------------------------------------------------------------------------------------- |
| SECURITY-01 | N/A    | No persistence or stored secret was added.                                                        |
| SECURITY-02 | N/A    | No network intermediary exists in this batch.                                                     |
| SECURITY-03 | N/A    | Production logging was not added; seed output exists only in tests.                               |
| SECURITY-04 | N/A    | No HTML-serving endpoint or header configuration changed.                                         |
| SECURITY-05 | PASS   | ISO date/time, estimate, drop and grid inputs have strict type, format and range checks.          |
| SECURITY-06 | N/A    | No IAM or permission policy exists.                                                               |
| SECURITY-07 | N/A    | No network configuration changed.                                                                 |
| SECURITY-08 | N/A    | No endpoint or authorization surface changed.                                                     |
| SECURITY-09 | PASS   | Pure modules contain no HTML injection sink, dynamic execution, debug page or mock transport.     |
| SECURITY-10 | PASS   | No dependency was added or changed; the existing lockfile and audit gate remain intact.           |
| SECURITY-11 | PASS   | Helpers are display-only and do not bypass U1 scheduling authority.                               |
| SECURITY-12 | PASS   | No credential, token, environment secret or persistent authority data was introduced.             |
| SECURITY-13 | PASS   | `TaskView` and `ScheduleView` are aliases of generated OpenAPI types, never manual duplicates.    |
| SECURITY-14 | N/A    | No monitoring or alerting surface changed.                                                        |
| SECURITY-15 | PASS   | Invalid values fail closed with explicit errors; there is no external resource to leak or retain. |

### Code Checklist

- Input/output: PASS — strict allowlists and boundary tests cover malformed dates, time alignment,
  08:00~~22:00 limits and 15~~840-minute estimates; no user HTML or raw error rendering exists.
- Access/data/web: N/A — no API, persistence, credential, browser storage, HTML sink or external
  origin was added.
- Exceptions: PASS — pure validation failures are explicit and contain no internal path or stack
  material intended for the UI.
- Supply chain: PASS — no new package; `npm run verify` and contract drift pass.
- Test evidence: PASS — RED was observed for three missing modules and separately for two
  out-of-window drop cases, then 20 focused tests and the 21-test full suite passed. Coverage is
  96.29% lines and 88.76% branches.

### PBT Compliance

| Rule   | Result | Evidence                                                                                                                  |
| ------ | ------ | ------------------------------------------------------------------------------------------------------------------------- |
| PBT-02 | PASS   | ISO date and HH:mm parse/format round trips run over generated valid values.                                              |
| PBT-03 | PASS   | Week continuity, grid range, alignment and slot-span invariants are generated.                                            |
| PBT-07 | PASS   | Reusable constrained `TaskView`, `ScheduleView`, ISO date and drop generators exist.                                      |
| PBT-08 | PASS   | fast-check shrinking remains enabled; fixed seed `20260901` is printed on every property run. No counterexample occurred. |
| PBT-09 | PASS   | fast-check runs inside the standard Vitest and `npm run verify` lifecycle.                                                |

PBT-01, PBT-04~06 and PBT-10 are advisory under the approved partial mode.

## Code Generation Steps 5~6 — Transport Boundary and Connectivity

- **Reviewed at**: 2026-09-01 20:52:23 (KST)
- **Changed application files**: `frontend/src/shared/api/`
- **Stable IDs**: F-C08, F-N03, F-N05, F-N08, NFR-008, SECURITY-05, SECURITY-09,
  SECURITY-13
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule           | Result | Evidence or N/A rationale                                                                                                                                        |
| -------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SECURITY-01~04 | N/A    | No persistence, secret, TLS intermediary, HTML sink or response-header configuration changed.                                                                    |
| SECURITY-05    | PASS   | Query keys and enum/range values are allowlisted; error fields and request IDs are bounded and filtered.                                                         |
| SECURITY-06~08 | N/A    | No IAM, network exposure or backend authorization surface changed.                                                                                               |
| SECURITY-09    | PASS   | Mock installation requires both development mode and `VITE_USE_MOCK=1`; the production gate rejects the mock module and worker filename.                         |
| SECURITY-10    | PASS   | No dependency or lockfile change was required.                                                                                                                   |
| SECURITY-11    | PASS   | The client performs one request only and introduces no bypass of U1 domain authority.                                                                            |
| SECURITY-12    | PASS   | No credential or persistent authority data was introduced.                                                                                                       |
| SECURITY-13    | PASS   | All transport types come from the generated OpenAPI declaration; untrusted success payloads are validated before cache entry and no unchecked assertion remains. |
| SECURITY-14    | N/A    | Monitoring configuration did not change.                                                                                                                         |
| SECURITY-15    | PASS   | Malformed responses fail closed as sanitized `INVALID_RESPONSE`; transport details and unknown fields are not exposed.                                           |

### Reliability and Test Evidence

- Mutations receive no automatic retry. The transport wrapper performs exactly one fetch.
- Only thrown transport failures enter `disconnected`; HTTP 400, 404, 409, 429 and 500 remain
  ordinary server error paths. Polling is fixed at 5 seconds and stops after 24 attempts.
- Five Step 5 suites failed first because the implementation modules were absent. After Step 6,
  27 focused tests pass. The first full gate exposed 72.79% branch coverage; additional response
  boundary and monitor lifecycle tests raised it without changing thresholds.
- Final `npm run verify`: 62 tests, 93.80% lines, 82.96% branches, 98.27% functions, contract drift
  clean, 58.0KB gzip production bundle.

PBT rules are N/A for this batch: the enabled property invariants belong to the pure time/grid core
and remain covered by the unchanged fixed-seed suite.

## Code Generation Steps 7~8 — Query Cache and Mutation Coordination

- **Reviewed at**: 2026-09-01 21:07:10 (KST)
- **Changed application files**: `frontend/src/shared/api/cache.ts`,
  `frontend/src/shared/api/mutation-coordinator.ts`
- **Stable IDs**: F-N02, F-N04, NFR-004, NFR-005, NFR-006, NFR-008, UR-032, UR-050,
  UR-052, UR-053, UR-062
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule           | Result | Evidence or N/A rationale                                                                                                         |
| -------------- | ------ | --------------------------------------------------------------------------------------------------------------------------------- |
| SECURITY-01~10 | N/A    | No persistence, network endpoint, HTML sink, mock path or dependency changed.                                                     |
| SECURITY-11    | PASS   | `expectedVersion` is copied from the last generated `TaskView`; client state never becomes business authority.                    |
| SECURITY-12    | PASS   | No secret or authority-bearing value is retained.                                                                                 |
| SECURITY-13    | PASS   | The only transport entity is an alias of generated `TaskView`; coordinator interfaces describe local callbacks, not wire schemas. |
| SECURITY-14    | N/A    | No monitoring surface changed.                                                                                                    |
| SECURITY-15    | PASS   | Rollback finishes before failure state publication; unknown failures enter the bounded failed path without leaking payloads.      |

### Reliability and Test Evidence

- Query retries stop after two retries; TanStack mutation retry is explicitly `false`.
- Invalidation is limited to task detail, task-list prefix and explicit source/destination week keys;
  same-week keys are deduplicated and no whole-cache invalidation exists.
- The snapshot is captured before an optimistic write. Failure restores it before `conflict` or
  another terminal state is published. Success passes only the server payload to replacement.
- Delete skips snapshot and optimistic write. A second in-flight command for the same task returns
  without issuing a request.
- Step 7 RED: both suites failed on missing modules. Final `npm run verify`: 71 tests, 93.05% lines,
  81.27% branches, 96.05% functions, contract drift clean and 58.0KB gzip.

PBT rules are N/A for this coordination batch; no new mathematical invariant domain was introduced.

## Code Generation Steps 9~10 — Token Shell and Accessible Feedback

- **Reviewed at**: 2026-09-01 21:20:53 (KST)
- **Changed application files**: `frontend/src/app/`, `frontend/src/shared/ui/`,
  `frontend/tests/setup.ts`
- **Stable IDs**: F-C01, F-C09, F-N01, F-N06, FR-001, FR-011, NFR-004, NFR-008,
  UR-035, UR-061, UR-070~072, SECURITY-09, SECURITY-12, SECURITY-15
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule           | Result | Evidence or N/A rationale                                                                                                                                                          |
| -------------- | ------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SECURITY-01~08 | N/A    | No persistence authority, backend endpoint, credential or network exposure changed. Theme storage is a non-sensitive `light`/`dark` allowlist.                                     |
| SECURITY-09    | PASS   | JSX text nodes only; injection lint passes. No CDN, remote font, external image or dynamic execution was added.                                                                    |
| SECURITY-10    | PASS   | Existing pinned `lucide-react` and `axe-core` packages are used; no dependency or lockfile changed.                                                                                |
| SECURITY-11    | PASS   | The shell shows display-only placeholders and makes no business decision.                                                                                                          |
| SECURITY-12    | PASS   | Only a theme preference is stored; no secret is read from environment or browser storage.                                                                                          |
| SECURITY-13~14 | N/A    | No new transport schema or monitoring surface was added.                                                                                                                           |
| SECURITY-15    | PASS   | Route failures expose an authored message and request ID only; stack and component details never render. The boundary wraps the route main while leaving header/navigation usable. |

### Accessibility, Scope and Test Evidence

- Exactly one polite and one assertive live region own visual toast announcements. Focus outlines,
  skip navigation and real buttons/links are present.
- `Retry-After` disables only manual retry for the specified countdown; it creates no automatic
  mutation replay. Disconnected state disables mutating controls while preserving navigation.
- Light/dark axe integration reports no serious or critical violations. Colour contrast is deferred
  to the approved real-browser axe gate in Step 13 because jsdom has no layout/color engine.
- All 15 design inputs were inspected. Implemented vocabulary: 16.5rem backlog, seven-day grid,
  indigo focus/action, neutral light and navy dark surfaces, red danger and amber warning. Board,
  tags, recurrence and assignee controls are absent rather than disabled.
- Chromium visual verification passed at 1440x900 and 320x800. Step 9 RED was three missing modules.
  Final `npm run verify`: 83 tests, 93.29% lines, 82.14% branches, 95.28% functions, contract drift
  clean and 64.3KB gzip.

PBT rules are N/A for this visual/feedback batch; enabled pure-core properties remain unchanged.

## Code Generation Steps 11~12 — Feature Slices and U1 Integration

- **Reviewed at**: 2026-09-01 21:54:04 (KST)
- **Changed application files**: `frontend/src/features/`, `frontend/src/app/LiveWorkspace.tsx`,
  `frontend/src/shared/api/planning-api.ts`, `frontend/src/shared/api/task-cache.ts`
- **Stable IDs**: F-C02~~F-C08, F-N02, F-N04, F-N07, NFR-004, NFR-005, UR-001~~072
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule           | Result | Evidence or N/A rationale                                                                                                     |
| -------------- | ------ | ----------------------------------------------------------------------------------------------------------------------------- |
| SECURITY-01~04 | N/A    | No secret, persistence authority, TLS boundary or response header changed.                                                    |
| SECURITY-05    | PASS   | Form values, URL filters, dates and times pass bounded local validation and server validation remains authoritative.          |
| SECURITY-06~08 | N/A    | No permission, origin exposure or backend authorization change was made.                                                      |
| SECURITY-09    | PASS   | JSX text rendering only; injection and production mock-reachability gates pass.                                               |
| SECURITY-10    | PASS   | No dependency or lockfile change was required.                                                                                |
| SECURITY-11    | PASS   | Client previews are defensive only; conflict, version and final task payload always come from U1.                             |
| SECURITY-12    | PASS   | No credential or authority-bearing state is stored in browser storage.                                                        |
| SECURITY-13    | PASS   | Wire requests and responses exclusively alias generated OpenAPI types; no handwritten transport type exists.                  |
| SECURITY-14    | N/A    | Monitoring configuration did not change.                                                                                      |
| SECURITY-15    | PASS   | Mutation failures roll back before conflict UI, deletion is non-optimistic and route failures expose sanitized messages only. |

### Reliability, Scope and Test Evidence

- Mutations send once with stable per-task single-flight coordination; query retry remains capped at
  two. The coordinator survives hook rerenders.
- Cache snapshots precede optimistic writes. Server success replaces every relevant cached
  appearance, with scheduled tasks added only to their actual week. Delete invalidates after 204.
- The 56x7 slot nodes retain identity during drag preview. Board, tag, recurrence and assignee
  controls are absent.
- Six Step 11 suites failed first on missing modules. Final `npm run verify`: 98 tests, 90.51% lines,
  81.85% branches, 85.27% functions, contract drift clean and 84.2KB gzip. U1 regression: 150 tests.

PBT rules remain satisfied by the unchanged fixed-seed time/grid suite; this batch adds no new
mathematical invariant domain.

## Code Generation Step 13 — E2E, Accessibility, Capacity and Supply Chain

- **Reviewed at**: 2026-09-02 20:29:33 (KST)
- **Changed application files**: `frontend/playwright.config.ts`, `frontend/tests/e2e/`,
  `frontend/tests/capacity/`, `frontend/src/shared/ui/dialog-focus.ts`, task editor/conflict/scheduling
  components and their tests
- **Stable IDs**: CP-02~~CP-05, NFR-004, NFR-005, NFR-007, NFR-008, SECURITY-01~~15,
  PBT-02, PBT-03, PBT-07, PBT-08, PBT-09
- **Verdict**: PASS — no blocking finding

### Security Baseline

| Rule        | Result | Evidence or N/A rationale                                                                                                                                                               |
| ----------- | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SECURITY-01 | N/A    | U2 owns no durable store; theme preference is non-sensitive and U1 owns encrypted persistence.                                                                                          |
| SECURITY-02 | N/A    | The approved local topology has no gateway, load balancer, CDN or other intermediary.                                                                                                   |
| SECURITY-03 | PASS   | U2 logs no task/request body or secret; request IDs are propagated and safely displayed. Central logging belongs to U1.                                                                 |
| SECURITY-04 | PASS   | The bundle uses no inline/evaluated/external script, is compatible with U1 CSP, and local Vite binds to loopback. Production TLS/HSTS remains outside the skipped infrastructure scope. |
| SECURITY-05 | PASS   | Forms, dates, times, estimates and URL query values are bounded/allowlisted; U1 revalidates all state changes.                                                                          |
| SECURITY-06 | N/A    | No identity policy or cloud role exists in U2.                                                                                                                                          |
| SECURITY-07 | PASS   | Dev, preview and E2E servers bind to explicit loopback ports; CORS uses the explicit frontend origin.                                                                                   |
| SECURITY-08 | N/A    | Authentication and object authorization are explicitly outside this single-user local product scope. U2 adds no endpoint.                                                               |
| SECURITY-09 | PASS   | Injection/dynamic-evaluation lint, safe error rendering, no source maps and mock exclusion all pass the production gate.                                                                |
| SECURITY-10 | PASS   | Exact versions and lockfile remain; audit reports 0 vulnerabilities; CycloneDX 1.6 SBOM contains 454 components; bundle gate passes.                                                    |
| SECURITY-11 | PASS   | U2 cannot bypass U1 validation/rate limiting and never automatically retries mutations or predicts conflicts as authority.                                                              |
| SECURITY-12 | PASS   | No credential, token, API key or authority-bearing browser state was introduced.                                                                                                        |
| SECURITY-13 | PASS   | Contract drift is clean, runtime responses are validated, and real-U1 E2E exercises the generated contract. No external resource is loaded.                                             |
| SECURITY-14 | N/A    | The local-only U2 has no authentication events, cloud log store, alerts or dashboard responsibility.                                                                                    |
| SECURITY-15 | PASS   | Cache rollback, route boundary, transport recovery and modal cleanup fail closed; UI errors expose no stack or internal body.                                                           |

### Project Checklist and Gate Evidence

- Input/output and browser checks: PASS. Text-only rendering and injection lint remain blocking;
  keyboard arrows are bounded to 08:00 through the latest valid task start, and modal focus is trapped.
- Access/data/observability: PASS or N/A by ownership. U1 stays authoritative and the browser stores
  no secret. The disabled resiliency extension was checked and skipped.
- Supply chain: PASS. `npm audit --audit-level=high` found 0 vulnerabilities; the ignored local SBOM
  parsed as CycloneDX 1.6 with 454 components and 496 dependency relationships.
- Automated evidence: `npm run verify` passes 101 tests at 89.32% lines, 82.54% branches and 83.33%
  functions; production gzip is 85.0KB/250KB. Real-U1 Playwright passes 10/10 across desktop and
  320px with zero serious/critical axe findings. The 1,000-task rerender passes in 120ms/300ms.
- Test-artifact handling: PASS. Successful Playwright runs record WebM evidence in the Git-ignored
  `frontend/test-results/` directory. The journeys use generated titles and in-memory test-profile
  data only; videos, traces and screenshots are not production or source-control artifacts.

### PBT Compliance

| Rule   | Result | Evidence                                                                                    |
| ------ | ------ | ------------------------------------------------------------------------------------------- |
| PBT-02 | PASS   | ISO date/time parse-format round trips remain in the standard 101-test suite.               |
| PBT-03 | PASS   | Week, 15-minute alignment, span and grid range invariants remain enabled.                   |
| PBT-07 | PASS   | Reusable constrained task, schedule, date and drop generators remain centralized.           |
| PBT-08 | PASS   | fast-check shrinking is enabled and fixed seed `20260901` is printed on every property run. |
| PBT-09 | PASS   | fast-check remains pinned and integrated with Vitest/`npm run verify`.                      |

PBT-01, PBT-04~06 and PBT-10 are advisory under the approved partial mode. Step 13 adds browser,
capacity and supply-chain evidence rather than a new mathematical invariant domain.

## Build and Test Follow-up — Live Timetable Axes and Tracked A–Z Evidence

- **Changed files**: `frontend/src/features/timetable/components.tsx`, timetable CSS and tests,
  A–Z E2E assertions, test documentation, and the reviewed A–Z WebM evidence.
- **Rendering boundary**: PASS. Weekday/date and 08:00–22:00 labels derive only from the bounded
  `weekStart`; task titles and times continue through React text nodes and no HTML sink was added.
- **Scheduling authority**: PASS. The change exposes existing KST/15-minute coordinates and styles
  the server-returned schedule; it does not move conflict, validation or persistence authority out
  of U1.
- **Accessibility**: PASS after correction. The first E2E run detected 4.39:1 contrast on block time
  text; it was raised to the normal text color and desktop/320px axe gates then passed 10/10.
- **Artifact exception**: PASS by explicit user authorization. Only the 832KB A–Z `video.webm` is
  tracked (`246c8d0`); it contains generated task names and in-memory test-profile data. Other videos,
  traces and screenshots remain ignored.
- **Automated evidence**: timetable tests 3/3, capacity 1/1, `npm run verify` 101/101 with 89.45%
  line and 82.60% branch coverage, real-U1 E2E 10/10, and A–Z journey 1/1 in 25.2s.

## Tempo Design Rework Phase 1 — Code Generation Review

- **Reviewed at**: 2026-09-03 22:58:26 (KST)
- **Verdict**: PASS — no blocking finding
- Native HTML drag payloads were removed; dnd-kit carries only the internal task id and bounded slot
  proposal. Preview-only capacity values never enter an API payload.
- Task titles, dates, positions and rollback reasons remain React text; no unsafe HTML, dynamic code,
  credential storage, new endpoint, new authority boundary or automatic mutation retry was added.
- U1 remains authoritative after optimistic scheduling, and rollback completes before persistent
  failure feedback is shown. Structured feedback exposes sanitized API messages.
- Exact already-approved dnd-kit versions are now exercised. `npm audit --audit-level=high` reports
  0 vulnerabilities and the CycloneDX SBOM was regenerated successfully.
- `npm run verify` passes 108/108 with 87.73% line and 80.58% branch coverage; contract drift is clean;
  bundle is 99.1KB/250KB. Real-U1 desktop/320px E2E passes 10/10 with blocking axe findings 0; A–Z
  passes 1/1 and the 1,000-task capacity gate passes 1/1.
