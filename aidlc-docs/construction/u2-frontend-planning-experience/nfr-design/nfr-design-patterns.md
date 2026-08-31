# U2 Frontend Planning Experience NFR Design Patterns

## 1. Design Context

U2 is one static bundle running in one browser tab against one local backend. Patterns favour
truthful state, bounded work and explicit failure over distributed-system machinery. There is no
queue, no worker, no service mesh and no circuit breaker: the single dependency is U1, and hiding its
errors behind a breaker would violate NFR-008's requirement that failures stay visible and
attributable to a request ID.

The controlling invariant behind every pattern below: **the server's answer replaces the client's
guess.** A pattern that would let local state outlive a contradicting server response is rejected.

## 2. Reliability Patterns

### Snapshot-and-Rollback (NFR-004, NFR-006)

Every optimistic update follows one shape:

```text
capture snapshot of every affected cache key
  -> apply optimistic value
  -> send exactly one request
  -> success: replace cache from the server payload   (never merge)
  -> failure: restore the snapshot verbatim, then route the error
```

- The snapshot is captured **before** the optimistic write, or the write does not happen.
- On a 409 `SCHEDULE_CONFLICT` the rollback completes **before** the conflict dialog renders, so the
  screen already agrees with the server when the user reads the choices.
- Delete is never optimistic: a destructive action waits for the server.
- Success replaces rather than merges, so a stale local field can never survive a round trip.

### Asymmetric Retry (NFR-006, NFR-008, approved Q4 decision)

| Operation class | Retry policy |
|---|---|
| Idempotent GET query | at most 2 automatic retries with backoff |
| Any mutation | **never** automatic; only an explicit user action |
| 429 rate limited | rollback, then a manual retry disabled until `Retry-After` elapses |
| Sustained connectivity loss | queries poll to recover; mutations stay manual |

Replaying a mutation could duplicate user intent or apply a placement decided against stale data, so
the client always makes the user re-issue it. No pending-save queue exists — the approved decision
rejects one because a queue lets the visible state disagree with the server.

### Connectivity State, Not a Circuit Breaker (NFR-008)

Consecutive network-level failures (no HTTP response at all) raise a `disconnected` state that pins a
banner under the header, rolls back in-flight optimistic updates and disables mutating controls.
Queries poll at a short interval; the first successful response clears the state.

Deliberate limits:
- Only transport failures count. A 4xx or 5xx **is** a server answer and routes to its normal
  surface, never to the banner.
- The state never suppresses or replaces an error message; it adds context.
- It gates nothing on the server side — U1's own rate limit remains the only throttle.

### Error Boundary Containment (NFR-008, SECURITY-15)

An error boundary wraps each route subtree, not the whole app, so a failing grid leaves the header,
navigation and theme toggle usable. Boundaries fail closed: the fallback offers a reload and shows
the last known request ID, never a stack trace or component name.

### Single-Flight Interaction Guard (NFR-006)

One scheduling mutation per task is in flight at a time; a second gesture on the same task is ignored
until the first settles. This prevents two placements racing to different slots with the same
`expectedVersion`.

## 3. Performance Patterns

### Week-Scoped Cache Keys (NFR-005)

`['week', weekStart]`, `['tasks', serializedQuery]`, `['task', id]`. A mutation invalidates only the
affected week and list keys; a cross-week move invalidates both week keys. Nothing invalidates the
whole cache. One weekly request serves both the grid and the backlog, and the capacity indicator is
derived from that same payload rather than a second request.

### Stable Grid Identity (NFR-005)

The 56×7 slot grid is the render hot spot. Controls:
- Slot cells are keyed by `date + slotIndex` and never remount on hover or drag-over.
- Drag feedback moves a single preview element; slot cells do not re-render per pointer move.
- Geometry is computed by pure functions memoized per `(schedule, estimateMinutes)`.
- Component tests assert render counts, so a regression fails a test rather than being noticed later.

### Budget as a Build Gate (NFR-005)

The 250KB gzip ceiling is checked in the production build and blocks it. The 1,000-task render
measurement runs under a separate `test:capacity` script, mirroring U1's `-Pcapacity` split, so the
default verify stays fast.

## 4. Accessibility Patterns

### Dual-Input Command Path (NFR-004)

Every scheduling command has three entry points that converge on one `SlotProposal`: pointer drag,
keyboard (Space / arrows / Enter / Esc) and a date-time form. The proposal type is the seam — the
mutation layer cannot tell which input produced it, so keyboard and pointer paths cannot drift apart.

### Single-Announcement Discipline (NFR-004)

Two live regions only: polite for outcomes, assertive for conflicts and failures. Exactly one message
per state transition, emitted by the feedback component rather than by each feature, so a single
action can never produce three overlapping announcements.

### Focus Ownership (NFR-004, FR-011)

Every dialog owns its focus lifecycle: trap on open, Esc closes, focus restored to the invoking
element. The invoker is passed in explicitly instead of being read from `document.activeElement` at
close time, which is unreliable after a rollback re-render.

### Redundant Encoding (NFR-004)

Priority, today, weekend, conflict and completion each carry an icon or text label in addition to
colour. Both themes are checked by the axe contrast gate.

## 5. Security Patterns

### Text-Only Rendering (NFR-003, SECURITY-09)

User content is rendered as text nodes. `innerHTML`, `dangerouslySetInnerHTML` and `eval` are lint
errors with no allowlist, which also keeps the app compatible with U1's CSP without inline scripts.

### Generated-Contract-Only Transport (NFR-007, SECURITY-13)

Transport types exist only in the generated file; CI regenerates and fails on a diff. Responses are
runtime-validated before entering the cache, so a contract change surfaces as a typed failure rather
than an undefined field deep in a render.

### Allowlist at Both Edges (SECURITY-05)

Outbound: URL query parameters are serialized from an allowlist, and an out-of-range value falls back
to the documented default instead of being forwarded. Inbound: rendered error content is limited to
`code`, `message`, `requestId` and allowlisted field names — never the raw body.

### Mock Isolation (SECURITY-09)

`VITE_USE_MOCK=1` enables the MSW browser worker in development only. The production build fails if
the mock module is reachable from the shipped bundle, so a mock transport can never ship.

## 6. Maintainability Patterns

### Feature Slice with a Hook Seam (NFR-007)

Each feature owns its components and its query/mutation hooks. UI components never import the API
client; an import-boundary lint rule enforces it. This is the frontend mirror of U1's ports-and-
adapters direction.

### Pure Geometry Core (NFR-002, NFR-007)

Time and grid geometry are pure functions with no cache, DOM or network access — the fast-check
surface for PBT-02, PBT-03 and PBT-07. They may mirror U1's time policy for display but authorise
nothing.

### Theme by Token (NFR-004)

A root `data-theme` attribute plus CSS custom properties. No component reads the theme, so adding or
adjusting a mode touches token files only.

## 7. Explicitly Rejected Patterns

| Rejected | Reason |
|---|---|
| Circuit breaker | One local dependency; it would hide the truthful error state NFR-008 requires. |
| Offline write queue / outbox | Rejected by the approved Q4 decision — visible state would disagree with the server, and queue ordering plus re-conflict rules are undefined. |
| Service worker caching of API responses | A cached plan could contradict the server; the client is not allowed a second source of truth. |
| Client-side conflict prediction to skip a request | Overlap is decided in a server transaction; predicting it would create a second authority (UR-024). |
| Global state store for server data | Duplicates TanStack Query's role and invites business state in the client. |
| Automatic mutation retry | Could duplicate intent or apply a stale placement. |

## 8. Traceability

| Requirement | Patterns |
|---|---|
| NFR-002 | §6 Pure Geometry Core |
| NFR-003 | §5 all |
| NFR-004 | §2 Snapshot-and-Rollback, §4 all |
| NFR-005 | §3 all |
| NFR-006 | §2 Snapshot-and-Rollback, Asymmetric Retry, Single-Flight Guard |
| NFR-007 | §5 Generated-Contract-Only Transport, §6 Feature Slice |
| NFR-008 | §2 Connectivity State, Error Boundary Containment, §5 Allowlist at Both Edges |
