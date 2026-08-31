# U1 Backend Planning Core NFR Design Patterns

## 1. Design Context

U1 is one Spring Boot process with an embedded H2 database and no remote dependency. Patterns favor
deterministic local correctness, bounded work and explicit failure over distributed-system machinery.
The functional `Task` aggregate and `SchedulePolicy` remain framework-free.

## 2. Reliability and Recovery Patterns

### Transactional Command Boundary (NFR-006, SECURITY-13, SECURITY-15)

Each material command enters one application-service transaction:

```text
validated command
  -> load aggregate/version
  -> evaluate domain and current schedule
  -> persist aggregate
  -> append audit event
  -> commit
```

- Validation, not-found, stale-version and conflict outcomes perform no durable write.
- Persistence/audit failure marks the transaction rollback-only and becomes one safe error response.
- `@Transactional` belongs to application orchestration, not controllers or domain objects.
- Audit has append-only persistence operations; no update/delete application port exists.
- File restore is an offline stop-copy-start operation. Startup Flyway validation rejects an unknown or
  partially migrated schema instead of attempting repair.

### Optimistic Concurrency and Explicit Retry (NFR-006)

JPA `@Version` is mapped to the domain `expectedVersion`. A mismatch becomes typed `409 STALE_TASK`.
Mutation failures are never automatically retried because replay could duplicate user intent or apply a
stale scheduling choice. The frontend must reload and submit a new explicit command. Idempotent desired-
state operations still use the expected version and create no duplicate audit event for a true no-op.

### Fail-Fast Boundary (NFR-003, NFR-008)

The request is rejected at the earliest owning layer: body size/media type, rate limit, DTO validation,
resource/version lookup, domain invariant, persistence. Every failure maps once to a fixed error code and
request ID. Unexpected exceptions are logged once by the global boundary and never exposed.

Circuit breakers, remote retry, queue redelivery and bulkheads are N/A: U1 calls no remote system and has
one embedded store. JDBC pool limits and request/body/rate bounds provide sufficient resource containment.

## 3. Scalability and Performance Patterns

### Bounded Query Pattern (NFR-005)

- Week plans query `[weekStart, weekStart + 7 days)` only.
- Task list uses `Pageable` with size 1~100 and stable ID tie-breaking.
- Backlog and conflict candidates use explicit predicates and bounded projections, never aggregate-wide
  in-memory filtering.
- Controller DTO enums are mapped to predefined repository specifications; raw sort or predicate strings
  never enter JPQL/SQL.

### Index Design (NFR-005, NFR-006)

| Table | Index | Supports |
|---|---|---|
| `tasks` | primary key `(id)` and optimistic `version` column | aggregate load/update |
| `tasks` | `(status, due_date, priority, created_at, id)` | stable backlog/list filtering |
| `schedule_slots` | `(schedule_date, start_time, end_time, task_id)` | bounded week and overlap candidate reads |
| `audit_events` | `(task_id, occurred_at, id)` | future bounded trace/recovery inspection; no public unbounded API |

Conflict decisions still re-evaluate the half-open interval predicate in domain code inside the mutation
transaction. Indexes narrow candidates; they do not replace domain correctness.

### Projection and No-Cache Baseline (NFR-005)

Read use cases return immutable query projections instead of loading audit events or unused entity graphs.
There is no application data cache in the MVP: 10,000 local rows fit indexed H2 access, while cache
invalidation could serve stale conflicts. A cache may only be introduced after repeatable measurements,
with an explicit invalidation and correctness design.

The performance fixture seeds 1,000 deterministic tasks, warms the JVM/query path, samples ordinary read
APIs, computes p95, records hardware/runtime, and fails after two controlled runs above 300 ms. A separate
10,000-task capacity fixture verifies bounded results and memory stability without redefining the p95 SLO.

## 4. Security Patterns

### Ordered HTTP Boundary (NFR-003)

```text
loopback socket
  -> forwarded-header rejection/default handling
  -> request-ID filter
  -> body-size/media enforcement
  -> bounded token bucket
  -> Spring Security: explicit routes, CORS, headers
  -> controller validation
  -> application/domain
  -> safe exception mapping
```

- The server defaults to `127.0.0.1`; allowed origins are exact configured loopback origins.
- Client request IDs are accepted only when they match an opaque allowlist format and length; otherwise a
  server value is generated. The ID is placed in MDC and the response header, then cleared in `finally`.
- A memory-bounded per-client/local bucket rejects before command execution with `429` and `Retry-After`.
  Its clock and policy are injected; failure to evaluate the limiter fails closed for mutation routes.
- API routes are explicitly `permitAll` because local authentication is out of scope. Any unmatched route
  is denied. CSRF is disabled only for the stateless JSON API and exact loopback CORS boundary.
- CSP, `nosniff`, frame deny and referrer policy are applied to responses. HSTS is enabled only by an HTTPS
  profile so local HTTP does not claim transport security it lacks.

### Typed Input and Output Boundary (SECURITY-03, SECURITY-05, SECURITY-09)

Controllers accept dedicated request DTOs, never persistence/domain entities. Bean validation and explicit
cross-field mapping enforce Unicode code-point lengths, enum allowlists, ISO date/time, page/body limits and
15-minute rules before repository work. JPA criteria or fixed queries provide parameter binding.

Responses serialize dedicated views. Error field names and codes come from allowlists; rejected values,
task content, exception messages, SQL, paths and versions are excluded. Conflict metadata exposes times and
task identifiers needed for recovery, not another task's content.

### Encrypted File Profile and Supply Chain (SECURITY-01, SECURITY-10, SECURITY-12)

- `test` uses uniquely named in-memory H2 databases.
- `file` requires the runtime H2 composite password/key environment value and `CIPHER=AES`; missing values
  fail startup. No usable default is present.
- H2 console, TCP server and trace output are disabled in all profiles.
- Maven wrapper/BOM, vulnerability gate and CycloneDX SBOM make dependency resolution inspectable.
- DB/key/env/trace files remain ignored, and security review scans changes before completion.

## 5. Observability and Safe Error Patterns

Structured logs contain `timestamp`, `level`, `requestId`, a controlled event name/message, resource ID and
safe outcome where relevant. User title/description, raw body, database key, SQL parameters and response
stack trace are forbidden. Expected validation/conflict outcomes use bounded log volume; unexpected failures
are logged once with the correlated ID.

Actuator exposes only health groups needed for liveness/readiness. Readiness includes database connectivity,
but the public response is sanitized to status. Environment, beans, mappings, config properties, heap dump,
loggers and metrics endpoints are not exposed. Startup smoke evidence requires readiness within five seconds.

## 6. Maintainability and Verification Patterns

- Hexagonal dependency checks use ArchUnit: domain depends only on JDK/domain; web and persistence adapters
  depend on application ports; controllers cannot access repositories.
- OpenAPI is produced from dedicated DTOs and normalized for checked-in contract comparison.
- JaCoCo enforces 80% overall line/branch and 90% schedule-collision branch coverage.
- jqwik generators and properties implement PBT-02, PBT-03, PBT-07, PBT-08 and PBT-09 with seed output.
- `./mvnw verify` runs formatting, unit/integration/property/architecture/contract/coverage checks. Expensive
  capacity, encrypted-file restore and dependency scans have documented commands and CI gates.
- Every code-change batch appends a PASS/BLOCKED entry to U1 `code/security-review.md`; an applicable
  unchecked item prevents stage completion.

## 7. Pattern Traceability

| Requirement | Primary pattern evidence |
|---|---|
| NFR-001 | layered TDD suite, JaCoCo gates and stable-ID naming |
| NFR-002 | reusable jqwik generators, properties, shrinking and seed evidence |
| NFR-003 | ordered HTTP boundary, typed DTOs, encrypted file profile and supply-chain gate |
| NFR-004 | semantic error/field/conflict contract supporting accessible frontend recovery |
| NFR-005 | bounded queries, composite indexes, projections and controlled performance fixture |
| NFR-006 | application transaction, optimistic version and no implicit mutation retry |
| NFR-007 | hexagonal dependency tests, reproducible Maven build and OpenAPI drift gate |
| NFR-008 | request correlation, structured redacted logging, sanitized health and global errors |
