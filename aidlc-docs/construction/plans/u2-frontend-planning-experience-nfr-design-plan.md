# U2 Frontend Planning Experience NFR Design Plan

The last design gate before U2 code generation. Artifacts are generated only after every question
below is answered.

## Steps

- [x] Read the approved U2 NFR requirements and tech stack decisions
- [x] Read the approved U2 functional design artifacts
- [x] Map each NFR to a concrete pattern and a logical component owner
- [x] Evaluate every rule-mandated pattern category for applicability
- [x] Collect answers to the questions below
- [x] Generate `nfr-design-patterns.md` and `logical-components.md`

## Pattern Category Applicability

| Category | Applicable | Reason |
|---|---|---|
| Resilience | Yes, narrowly | Bounded query retry, snapshot rollback, error boundary. Mutations never auto-retry (approved Q4 decision). No circuit breaker: one local dependency, and a breaker would hide the truthful error state NFR-008 requires. |
| Scalability | No | One local user, one browser tab, one backend process. Bounded page sizes and week-scoped queries are already the capacity control. |
| Performance | Yes | Bundle ceiling, render-count control on a 56×7 grid, cache-key scoping, 1,000-task fixture measurement. |
| Security | Yes | CSP-compatible rendering, no HTML injection, generated-contract-only transport, supply chain gates. |
| Logical components | Yes, in-process only | Query cache, live-region announcer, error normalizer, interaction state machine. No queue, no external cache, no worker: the approved Q4 decision rejects a pending-save queue. |

## Questions

### Q1 — What happens when U1 is not running?

The functional design covers per-request failures, but not a sustained backend outage.

[Answer]: **Global banner with reconnect polling.** Consecutive network-level failures raise a
connectivity state that pins a banner under the header and rolls back any in-flight optimistic update.
While disconnected, **queries** poll at a short interval so the app recovers by itself when U1 returns;
**mutations** never auto-retry, and mutating controls are disabled so the user cannot queue doomed
commands. A single successful request clears the state. This is a connectivity signal, not a circuit
breaker: it never suppresses a real error response from the server.

### Q2 — How is the light/dark theme selected?

The design screens provide both variants; no approved document says how a user picks one.

[Answer]: **System default with a manual toggle.** `prefers-color-scheme` decides the initial theme; a
header toggle overrides it and the choice persists in `localStorage`. Implemented purely with CSS
custom properties on a root `data-theme` attribute, so no component reads the theme. Both modes are
covered by the axe contrast gate.

### Q3 — Is there a backend-free development mode?

MSW is already a test dependency. It could also serve the dev server.

[Answer]: **Yes, behind a flag.** With `VITE_USE_MOCK=1` the dev server installs the MSW browser worker
and serves the same handlers the component tests use, so UI work needs no JVM. The default is a real U1
connection, and the production build fails if the mock module is reachable from the shipped bundle.
