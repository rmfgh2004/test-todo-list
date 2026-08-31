# U1 Backend Planning Core NFR Requirements

## 1. Scope and Quality Gate

This document refines the approved `NFR-001` through `NFR-008` for the local backend unit.
An applicable unmet requirement is a blocking defect. Cloud high availability, multi-user
authentication and external integrations remain out of scope.

## 2. Capacity and Performance

| ID | U1 requirement | Verification |
|---|---|---|
| NFR-005 | Support 10,000 persisted tasks for one local user without changing the architecture. | H2 data-profile integration test and query-plan review. |
| NFR-005 | With 1,000 tasks, ordinary task, backlog and bounded weekly APIs have p95 latency at or below 300 ms on the documented development machine after warm-up. | Repeatable backend performance test; record machine and sample count. |
| NFR-005 | Weekly reads require exactly seven days starting on Monday. List reads use page size 1~100, default 25, and allowlisted filters/sorts. | MockMvc boundary tests and repository query tests. |
| NFR-005 | No endpoint returns an unbounded task or audit collection. | API contract inspection and tests for missing/oversized limits. |
| NFR-008 | The application and database readiness health check becomes successful within 5 seconds after process start on the documented development machine. | Startup smoke test. |

The 300 ms target excludes first JVM startup, build time and frontend rendering. A regression is
blocking when the same controlled test exceeds the target in two consecutive clean runs.

## 3. Availability and Recovery

| ID | U1 requirement | Verification |
|---|---|---|
| NFR-006 | Every material mutation and its audit event commit in one transaction; any domain, persistence or audit failure rolls back the complete change. | Fault-injection H2 integration tests. |
| NFR-006 | Optimistic version checks prevent lost updates and return a safe `409` response. | Concurrent update integration tests. |
| NFR-008 | Liveness and readiness expose application and H2 status without secrets, paths, SQL or component internals. | Actuator configuration and response tests. |
| NFR-006 | File mode documents a stop-copy-restore procedure for the encrypted H2 database. Schema migrations are forward-only and validated at startup. | Restore smoke test against a copied test database. |

Availability is limited to the running local process. Automatic failover, clustering, cloud backup,
RTO and RPO commitments are not required because the resiliency extension is disabled.

## 4. Security and Privacy

`NFR-003` and `aidlc-inputs/04-security-review-checklist.md` are blocking for every code change.

| Baseline | Status | U1 requirement or N/A rationale |
|---|---|---|
| SECURITY-01 | Applicable | File-mode H2 uses `CIPHER=AES`; the key is injected at runtime and never committed. Embedded H2 has no network transport. |
| SECURITY-02 | N/A | No proxy, gateway, load balancer or CDN exists. |
| SECURITY-03 | Applicable | Structured logs contain timestamp, level, request ID and safe message only; raw bodies, task text, keys and response stack traces are prohibited. |
| SECURITY-04 | Applicable | API responses set CSP-compatible policy, `nosniff`, frame deny and referrer policy; the HTTPS profile enables HSTS. |
| SECURITY-05 | Applicable | DTO type, Unicode length, enum, date/time, range, collection and body-size allowlists run before data access; persistence uses parameter binding. |
| SECURITY-06 | N/A | No cloud IAM or service identity exists. |
| SECURITY-07 | Applicable | Server binding defaults to loopback and CORS permits only configured loopback frontend origins, never `*`. |
| SECURITY-08 | Applicable | `/api/v1/**` and health routes are intentionally public only to the local machine; authentication and per-object authorization are out of scope. |
| SECURITY-09 | Applicable | H2 console, sample endpoints, default error page and unsafe error details are disabled; only supported runtime/dependency versions are used. |
| SECURITY-10 | Applicable | Maven/BOM resolves exact versions; CI runs dependency vulnerability checks and creates a CycloneDX SBOM. |
| SECURITY-11 | Applicable | A process-local rate limit applies before commands and returns safe `429` with `Retry-After`; all state rules are revalidated server-side. |
| SECURITY-12 | Partial N/A | User credentials and authentication are absent. The database key is still externalized and redacted under SECURITY-01/03. |
| SECURITY-13 | Applicable | Audit records are append-only, transactional and omit content values; generated API artifacts are integrity-checked. |
| SECURITY-14 | N/A | Cloud security alerting and identity monitoring are absent; local request-ID logging remains required by NFR-008. |
| SECURITY-15 | Applicable | Global exception mapping, rollback and resource cleanup fail closed and expose only safe error codes. |

Additional enforceable limits:

- JSON is the only mutation media type; unsupported types return `415`.
- Requests exceeding the configured body limit return `413` before parsing.
- Rate-limit state is memory-bounded and keyed without storing task content or personal data.
- H2 database files, trace files, runtime keys and `.env` files remain ignored by Git.

## 5. Testing and Property-Based Verification

| ID | U1 requirement | Exit evidence |
|---|---|---|
| NFR-001 | Use TDD and cover domain unit, application unit, MockMvc API and isolated in-memory H2 integration layers. | Test report and commit/work log. |
| NFR-001 | Backend line and branch coverage are each at least 80%; collision-domain branch coverage is at least 90%. | JaCoCo verification gate. |
| NFR-001 | Test names include related stable IDs with hyphens converted to underscores. Every business rule has example-based evidence. | Traceability scan and test report. |
| NFR-002 / PBT-02 | ISO date/time parse-format round trips preserve valid values. | jqwik properties. |
| NFR-002 / PBT-03 | 15-minute alignment, derived end time, overlap symmetry and touching-boundary non-overlap hold for generated valid inputs. | jqwik properties plus examples. |
| NFR-002 / PBT-07 | Reusable generators produce valid and boundary Task/ScheduleSlot values. | Shared test generators. |
| NFR-002 / PBT-08 | Shrunk counterexamples and seeds are printed; every discovered defect gains a fixed regression example. | CI test output and regression test. |
| NFR-002 / PBT-09 | jqwik integrates with JUnit and runs in the standard Maven test lifecycle. | Maven build report. |

PBT-01, PBT-04~06 and PBT-10 are advisory in the approved partial mode. Property tests supplement,
not replace, deterministic examples for conflict, transaction and security behavior.

## 6. Maintainability and Contract Quality

| ID | U1 requirement | Verification |
|---|---|---|
| NFR-007 | Domain code has no Spring, JPA or transport imports; adapters depend inward through ports. | ArchUnit test. |
| NFR-007 | Java 17 compilation, formatting, lint/static analysis and tests are one reproducible Maven command. | Maven verify gate. |
| NFR-007 | OpenAPI documents DTO constraints, pagination, safe errors, `409`, `413`, `415` and `429`; frontend contract generation detects drift. | OpenAPI snapshot/diff test. |
| NFR-007 | Public classes and core operations start Javadoc with related `FR-XXX`/`NFR-XXX`. | Traceability scan. |
| NFR-008 | Exceptions are logged once at the owning boundary and carry a request ID across response and logs. | Unit and MockMvc fault tests. |

## 7. API Usability and Error Contract

All errors use a stable JSON object containing `code`, a safe Korean-capable `message`,
`requestId`, and optional `fieldErrors`. Field errors identify allowlisted field names without echoing
rejected values. Validation returns `400`, not found `404`, stale version or scheduling conflict
`409`, body/media violations `413`/`415`, rate limiting `429`, and unexpected failures `500`.
Conflict responses include the proposed slot, conflicting slot metadata and an optional next candidate,
but never another task's title or description. This is the backend contribution to `NFR-004` and lets
the frontend provide focus management, announcements and recovery without parsing free-form text.

## 8. Definition of Done

U1 NFR work is complete only when the Maven verification gate, coverage thresholds, architecture and
contract tests, applicable security checklist entries, Security Baseline table, PBT obligations and
encrypted file-mode smoke test all pass with evidence recorded under the U1 code artifacts.
