# U2 Frontend Planning Experience NFR Requirements

## 1. Scope and Quality Gate

This document refines the approved `NFR-001`~`NFR-008` for the local frontend unit. An applicable
unmet requirement is a blocking defect. U2 owns no durable state, no authorization and no cloud
runtime, so availability, disaster recovery and identity requirements stay with U1 or are N/A.

The backend p95 latency, encryption, rate-limit and audit obligations belong to U1 and are not
restated here. U2's obligation is to consume them truthfully.

## 2. Performance and Capacity

| ID | U2 requirement | Verification |
|---|---|---|
| NFR-005 | The initial JS bundle is at most **250KB gzip**; the production build fails above the ceiling. | Build-time size check in `npm run build`. |
| NFR-005 | The first screen is interactive within **2s** on the documented development machine, measured from navigation to the weekly grid becoming operable. | Playwright timing assertion on a warm dev build. |
| NFR-005 | With a **1,000-task fixture**, week navigation re-renders the grid within 300ms after the query resolves, and the 56×7 slot grid does not re-mount on a hover or drag-over. | Playwright measurement under the `capacity` profile; render-count assertion in component tests. |
| NFR-005 | One weekly request serves both grid and backlog; a mutation invalidates only the affected week and list keys, never the whole cache. | Query-key unit tests and a request-count assertion in E2E. |
| NFR-005 | List requests always send a bounded page size (default 25, max 100) and never request an unbounded collection. | Query serialization unit tests. |
| NFR-005 | The capacity indicator is derived from the already-fetched weekly payload and issues no additional request. | Network assertion in E2E. |

Bundle ceiling and render measurement are separate gates: the ceiling blocks every build, while the
1,000-task render measurement runs under a dedicated profile so the default build stays fast.

## 3. Accessibility (blocking)

| ID | U2 requirement | Verification |
|---|---|---|
| NFR-004 | `axe-core` scans every primary screen (week desktop, week mobile, list) and every dialog (task editor, delete confirm, schedule form, conflict). Any **serious or critical** violation fails the build. | Automated axe run in Vitest/Playwright. |
| NFR-004 | Every scheduling action is completable by keyboard: Space picks up, arrows move, Enter confirms, Esc aborts, plus an equivalent date/time form path. | Playwright keyboard journey. |
| NFR-004 | Dialogs trap focus, close on Esc, and restore focus to the element that opened them. | RTL + Playwright focus assertions. |
| NFR-004 | Every state transition announces exactly one concise ARIA-live message; conflicts and failures use the assertive region, outcomes the polite region. | RTL live-region assertions. |
| NFR-004 | Priority, today, weekend, conflict and completion each carry a non-colour cue. | Component assertions plus the manual contrast/reading-order checklist. |
| NFR-004 | The full journey works at 320px with no horizontal clipping and no overlapping text. | Playwright mobile viewport run. |

Contrast ratios and reading order that axe cannot decide remain on the manual review checklist and
are recorded per release rather than automated.

## 4. Reliability and Data Integrity

| ID | U2 requirement | Verification |
|---|---|---|
| NFR-006 | Every mutation echoes the server `version` as `expectedVersion`; the client never increments or invents a version. | Unit tests per mutation. |
| NFR-006 | Every optimistic update captures a rollback snapshot before applying, and any failure restores it exactly. | Cache rollback tests for schedule, unschedule and completion. |
| NFR-006 | A 409 `SCHEDULE_CONFLICT` rolls back before the conflict dialog renders, so the screen never shows a placement the server rejected. | E2E against real U1. |
| NFR-006 | A 409 `STALE_TASK` refetches and discards the local edit; no client state is written over newer server state. | Concurrent-edit E2E. |
| NFR-006 | Mutations are never retried automatically. Idempotent GET queries retry at most twice with backoff. | Query-client configuration test. |
| NFR-004 / NFR-008 | A 429 rolls back immediately, announces the failure, and offers a manual retry disabled until `Retry-After` elapses. No pending-save queue exists. | MSW-driven component test. |
| NFR-006 | Delete is never optimistic and always sends `confirmed=true` with `expectedVersion`. | Component + E2E tests. |

## 5. Observability and Safe Failure

| ID | U2 requirement | Verification |
|---|---|---|
| NFR-008 | Every request carries a client-generated `X-Request-Id` matching `^[A-Za-z0-9._-]{8,64}$` in the `TMP-XXXX-XXXX-XXXX` shape. | Client unit test. |
| NFR-008 | Every failure surface shows the authored message, the copyable request ID and a retry affordance where retry is meaningful. | Component tests per error class. |
| NFR-008 | Rendered errors use only `code`, `message`, `requestId` and allowlisted field names — never the raw response body. | Error-normalization tests. |
| NFR-008 | An unrecognised error code degrades to a safe generic state without throwing. | Unknown-code test. |
| NFR-008 | An error boundary contains a subtree render failure and keeps navigation usable. | Boundary test. |
| NFR-008 | No `console.log` of request bodies, task content or errors ships in the production build. | Lint rule + build assertion. |

**Contract note.** U1 returns 429 with `code: "RATE_LIMITED"` and a `Retry-After` header, but
`planning-api.yaml` documents neither. U2 handles the real behaviour and the generated types keep an
unknown-code fallback. The additive documentation fix to the contract is scheduled for U2 code
generation (recorded in the functional design, §12).

## 6. Security

`NFR-003` and `aidlc-inputs/04-security-review-checklist.md` are blocking for every U2 change.

| Baseline | Status | U2 requirement or N/A rationale |
|---|---|---|
| SECURITY-01 | N/A | U2 stores no durable data; encryption at rest is U1's. |
| SECURITY-02 | N/A | No proxy, gateway, load balancer or CDN. |
| SECURITY-03 | Supporting | The request ID is displayed and propagated; no task content, key or stack trace is logged in the browser. |
| SECURITY-04 | Applicable | The app runs under U1's CSP without inline scripts or `eval`; the Vite dev server binds to loopback and loads no third-party origin. |
| SECURITY-05 | Supporting | Client validation mirrors the server constraints as defence in depth and never widens them. |
| SECURITY-06 | N/A | No cloud IAM. |
| SECURITY-07 | Supporting | Dev server and API base URL are loopback by default. |
| SECURITY-08 | N/A | No authentication or per-object authorization exists in scope. |
| SECURITY-09 | Applicable | No user text is rendered as HTML (`innerHTML`, `dangerouslySetInnerHTML`, `eval` are lint-blocked); no debug page or source map ships in the production build. |
| SECURITY-10 | Applicable | `package-lock.json` pins exact versions; `npm audit` at the configured severity and a CycloneDX SBOM run in CI and block on findings. |
| SECURITY-11 | Supporting | Rate-limit and conflict outcomes are surfaced explicitly instead of being retried around. |
| SECURITY-12 | N/A | No credentials, tokens or secrets exist in the frontend; no secret is read from `import.meta.env`. |
| SECURITY-13 | Applicable | Transport types are generated from the U1 contract and drift-checked; no manual transport type is permitted. |
| SECURITY-14 | N/A | No cloud alerting. |
| SECURITY-15 | Applicable | Failures fail closed: cache rollback, error boundary, and no partial state left on screen. |

Additional enforceable limits:

- No external CDN, font, script or image host at runtime or in tests.
- No third-party analytics or telemetry.
- Build artifacts, `node_modules`, `.env` files and Playwright traces stay Git-ignored.

## 7. Testing and Property-Based Verification

| ID | U2 requirement | Exit evidence |
|---|---|---|
| NFR-001 | TDD: failing test → minimal implementation → refactor, across unit, component, contract-mock and E2E layers. | Test report and work log. |
| NFR-001 | Coverage floors: statements, functions and lines at 80%; branches at 75%. | Vitest coverage gate. |
| NFR-001 | Test names include the related `FR-XXX`/`NFR-XXX`/`UR-XXX` with hyphens converted to underscores. | Traceability scan. |
| NFR-001 | Unit and component tests run against MSW handlers typed from the generated contract; a contract change breaks compilation. | Type-check in CI. |
| NFR-001 | Playwright runs the desktop and 320px journeys against a **real U1 process** on the in-memory H2 profile. | E2E report. |
| NFR-002 / PBT-02 | ISO date/time parse-format round trips preserve valid values across the transport boundary. | fast-check properties. |
| NFR-002 / PBT-03 | Grid geometry invariants: 15-minute alignment, `slotSpan = estimateMinutes / 15`, positions inside 08:00~22:00, and week-range boundaries. | fast-check properties plus examples. |
| NFR-002 / PBT-07 | Reusable generators produce valid and boundary `TaskView`, `ScheduleView` and drop-position values. | Shared test generators. |
| NFR-002 / PBT-08 | Shrunk counterexamples and seeds are printed; every discovered defect gains a fixed regression example. | CI output and regression test. |
| NFR-002 / PBT-09 | fast-check integrates with Vitest and runs in the standard test lifecycle. | Build report. |

PBT-01, PBT-04~06 and PBT-10 remain advisory under the approved partial mode.

## 8. Maintainability

| ID | U2 requirement | Verification |
|---|---|---|
| NFR-007 | TypeScript `strict` is on; `any` and non-null assertions are lint errors outside generated files. | Type-check and lint gate. |
| NFR-007 | Type-check, lint, format, unit tests, coverage and bundle size run as one reproducible command. | `npm run verify`. |
| NFR-007 | Transport types are generated from `backend/openapi/planning-api.yaml`; CI regenerates and fails on any diff. | Drift check in CI. |
| NFR-007 | Feature slices own their hooks; UI components never call the API client directly. | Import-boundary lint rule. |
| NFR-007 | The frontend builds independently of the backend build; only the contract file crosses the boundary. | Separate build commands. |
| NFR-007 | Exported components and hooks document their related `FR-XXX`/`UR-XXX`. | Traceability scan. |

## 9. Availability

Availability is limited to the local dev server and the built static bundle. Failover, clustering,
CDN caching, RTO and RPO commitments are not required — the resiliency extension is disabled and the
application serves one local user. The only availability obligation is that a backend outage renders
as a truthful, retryable error state rather than a blank screen (NFR-008).

## 10. Traceability

| Requirement | Sections |
|---|---|
| NFR-001 | §7 |
| NFR-002 | §7 |
| NFR-003 | §6 |
| NFR-004 | §3, §4 |
| NFR-005 | §2 |
| NFR-006 | §4 |
| NFR-007 | §8 |
| NFR-008 | §5, §9 |
