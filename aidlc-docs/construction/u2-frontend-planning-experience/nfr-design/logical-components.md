# U2 Frontend Planning Experience Logical Components

## 1. Component Model

```text
[Browser]
    |
    v
[F-N01 Theme and Layout Root] ──> design tokens, data-theme, breakpoint
    |
    v
[F-N02 Query Cache Coordinator] <──> [F-N03 Connectivity Monitor]
    |                                        |
    v                                        v
[Feature slices F-C02~F-C07]        [F-N06 Announcer + Feedback]
    |                                        ^
    v                                        |
[F-N04 Mutation Coordinator] ────────────────+
    |            (snapshot / rollback / single-flight)
    v
[F-C08 API Client] ──> [F-N05 Error Normalizer]
    |
    v
[F-N07 Generated Contract Types]  (build-time, drift-checked)
    |
    v
  U1 over local HTTP        [F-N08 Mock Transport]  (dev/test only, flag-gated)

[F-N09 Verification Gate] validates every component and the contract
```

These are logical responsibilities inside one bundle, not separately deployed units.

## 2. Component Responsibilities

### F-N01 Theme and Layout Root

**Owns**: design tokens, the root `data-theme` attribute, `prefers-color-scheme` detection, the
persisted manual override, and the desktop/mobile breakpoint decision.
**Inputs**: OS colour-scheme preference, `localStorage` override, viewport width.
**Outputs**: CSS custom properties and a layout mode for F-C01.
**Constraint**: no feature component reads the theme; colour lives in tokens only.
**Traceability**: NFR-004, FR-011.

### F-N02 Query Cache Coordinator

**Owns**: cache keys (`['week', weekStart]`, `['tasks', query]`, `['task', id]`), staleness, bounded
query retry (max 2, backoff), and scoped invalidation.
**Inputs**: feature query requests, mutation success events, connectivity state.
**Outputs**: cached view data plus loading / empty / error status.
**Constraint**: never invalidates the whole cache; a cross-week move invalidates both week keys.
**Traceability**: NFR-005, NFR-008.

### F-N03 Connectivity Monitor

**Owns**: the `connected | disconnected` state derived from consecutive transport failures, the
reconnect poll while disconnected, and the disabling of mutating controls.
**Inputs**: transport-level outcomes from F-C08.
**Outputs**: connectivity state for the banner; a resume signal that refetches active queries.
**Constraint**: only transport failures count — any HTTP status is a server answer and routes
normally. It gates nothing server-side and suppresses no message.
**Traceability**: NFR-008.

### F-N04 Mutation Coordinator

**Owns**: the snapshot/rollback lifecycle, `expectedVersion` propagation, the single-flight guard per
task, and the S-F03 interaction state machine
(`idle → proposing → saving → scheduled | conflict | stale | failed`).
**Inputs**: typed commands from feature hooks; normalized errors from F-N05.
**Outputs**: cache writes, conflict-dialog transitions, announcements via F-N06.
**Constraint**: no mutation is ever retried automatically; a snapshot exists before any optimistic
write; success replaces cache data rather than merging it.
**Traceability**: NFR-004, NFR-006.

### F-N05 Error Normalizer

**Owns**: mapping any failure to `SafeApiError`
(`validation | not-found | conflict | stale | rate-limited | network | unknown`), extracting
`Retry-After`, and preserving the request ID.
**Inputs**: fetch rejections, non-2xx responses, schema-validation failures.
**Outputs**: one typed error object per failure.
**Constraint**: an unrecognised code degrades to `unknown` without throwing; only `code`, `message`,
`requestId` and allowlisted field names reach the UI. **Known gap**: U1 returns 429 with
`RATE_LIMITED`, undocumented in the contract — handled explicitly here and scheduled for an additive
contract fix during code generation.
**Traceability**: NFR-003, NFR-008.

### F-N06 Announcer and Feedback

**Owns**: one polite and one assertive ARIA-live region, toasts, skeletons, empty and error surfaces,
the copyable request ID, the `Retry-After`-gated retry control, the connectivity banner and the route
error boundaries.
**Inputs**: state transitions from F-N02/F-N03/F-N04.
**Outputs**: exactly one announcement per transition; visible status surfaces.
**Constraint**: features do not announce directly, so one action cannot produce overlapping messages.
**Traceability**: NFR-004, NFR-008, FR-011.

### F-N07 Generated Contract Types

**Owns**: the TypeScript declarations generated from `backend/openapi/planning-api.yaml` by
`openapi-typescript`.
**Inputs**: the checked-in contract file.
**Outputs**: request/response types consumed by F-C08, the mock handlers and the tests.
**Constraint**: committed and regenerated in CI; any diff fails the build. No hand-written transport
type is permitted anywhere.
**Traceability**: NFR-007, SECURITY-13.

### F-N08 Mock Transport

**Owns**: MSW handlers typed from F-N07, covering success and every failure path (400 field errors,
404, 409 conflict, 409 stale, 429 with `Retry-After`, 5xx, transport loss).
**Inputs**: `VITE_USE_MOCK=1` in development; direct import in unit/component tests.
**Outputs**: deterministic responses without a JVM.
**Constraint**: development and test only; the production build fails if the mock module is reachable
from the shipped bundle.
**Traceability**: NFR-001, SECURITY-09.

### F-N09 Verification Gate

**Owns**: type-check, lint (including the no-HTML-injection and import-boundary rules), format,
Vitest with the coverage gate, fast-check properties with logged seeds, axe scans, the bundle ceiling,
`npm audit`, the SBOM, Playwright desktop/mobile journeys against real U1, and the capacity profile.
**Inputs**: the working tree and the contract file.
**Outputs**: a pass/fail verdict; failure blocks the change.
**Traceability**: NFR-001, NFR-002, NFR-003, NFR-005, NFR-007.

## 3. Mapping to Functional Components

| Functional component | NFR components it relies on |
|---|---|
| F-C01 Application Shell | F-N01, F-N02, F-N06 |
| F-C02 Weekly Planner | F-N01, F-N02, F-N04 |
| F-C03 Backlog | F-N02, F-N04 |
| F-C04 Task Editor | F-N04, F-N05, F-N06 |
| F-C05 Scheduling Interaction | F-N04, F-N05, F-N06 |
| F-C06 Conflict Resolution | F-N04, F-N06 |
| F-C07 Task List | F-N02, F-N04 |
| F-C08 API Client | F-N03, F-N05, F-N07, F-N08 |
| F-C09 Accessible Feedback | F-N06 |

## 4. Deliberately Absent Components

| Absent | Reason |
|---|---|
| Message queue / outbox | The approved Q4 decision rejects a pending-save queue. |
| Service worker / offline cache | A cached plan could contradict the server; no second source of truth is allowed. |
| Circuit breaker | One local dependency; it would hide the errors NFR-008 requires to stay visible. |
| Global state store | TanStack Query owns server state; UI state is local. |
| Client-side authorization | No authentication exists in scope (SECURITY-08 N/A for U2). |
| Analytics / telemetry client | No external network destination is permitted. |

## 5. Dependency Direction

```text
F-C0x UI components → feature hooks → F-N04/F-N02 → F-C08 → F-N07 (types)
```

No arrow points backwards: a UI component never imports the API client, a hook never imports another
feature's components, and generated types never import application code. The import-boundary lint
rule enforces this, mirroring U1's ArchUnit rule.
