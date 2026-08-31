# U1 Code Security Review

Reviews are append-only. `PASS` means every item applicable to the reviewed change is satisfied; N/A
entries state why the change does not exercise that boundary.

## 2026-08-31T18:34:00+09:00 - Step 1 Backend Scaffold

- **Changed files**: `backend/pom.xml`, Maven Wrapper, `PlanningApplication.java`, smoke test/test config,
  root `.gitignore`.
- **Stable IDs**: FR-012; NFR-001, NFR-003, NFR-007, NFR-008; SECURITY-09, SECURITY-10.
- **Result**: PASS.
- **Automated evidence**: `./mvnw test` passed 1/1; `./mvnw verify` passed after applying the configured
  formatter; CycloneDX generated an SBOM; wrapper JAR SHA-1 matched Maven Central and SHA-256 is pinned.

### A. Input and Output

- [x] N/A - no API/form DTO or user-controlled input exists in this scaffold.
- [x] N/A - no user string rendering, SQL, command or path construction exists.
- [x] N/A - no application error response exists yet.
- [x] N/A - request body/collection handling is introduced and tested in later web steps.

### B. Access and Boundaries

- [x] N/A - no endpoint or security filter chain exists yet.
- [x] N/A - loopback binding/CORS is introduced in Step 12.
- [x] N/A - no state mutation exists.
- [x] N/A - rate limiting is introduced and tested in Steps 11~12.

### C. Data Protection

- [x] No secret, DB key, `.env`, H2 database or trace file is tracked; ignore rules cover runtime files.
- [x] N/A - the development file datasource is introduced in Step 12 and must use AES/runtime injection.
- [x] No application logging of input or secrets was added.
- [x] N/A - no mutation, transaction or audit record exists yet.

### D. Web and Browser

- [x] N/A - headers and CSP-compatible API boundary are introduced in Steps 11~12.
- [x] No CDN, external script, dynamic execution, unsafe HTML or frontend storage was added.

### E. Exceptions and Observability

- [x] N/A - DB/file/network application boundaries and global error mapper do not exist yet.
- [x] N/A - structured request logging and request ID are introduced in Step 12.

### F. Supply Chain and Configuration

- [x] Dependencies/plugins are from Maven Central, necessary for the approved architecture, versioned by
  Spring Boot BOM or explicit maintained release, and use compatible open-source licenses.
- [x] Spring Boot `4.1.1`, H2 `2.4.240` through the BOM, Maven `3.9.11`, Wrapper `3.3.4`, jqwik `1.9.3`,
  ArchUnit `1.4.2`, JaCoCo `0.8.15`, Spotless `3.9.0`, OWASP Dependency-Check `12.1.8` and CycloneDX
  `2.9.1` resolve deterministically.
- [x] CycloneDX SBOM generation passed. The OWASP vulnerability scan remains an explicit later security
  gate because it requires vulnerability database access.
- [x] No sample endpoint or H2 console dependency/configuration was added.
- [x] Maven wrapper integrity is pinned; build/coverage/SBOM products are ignored under `target/`.

### G. Test Evidence

- [x] The application entry-point smoke test passes and contains `NFR_007` in its name.
- [x] Boundary, malformed, security-header/CORS/rate/error tests are N/A for this scaffold and explicitly
  planned before their implementations.
- [x] Test fixtures contain no secret or personal data.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-01~08 | N/A | no data/API/security boundary implemented in scaffold |
| SECURITY-09 | PASS | no H2 console/sample endpoint; supported Spring Boot/Java versions |
| SECURITY-10 | PASS | BOM/versions, wrapper integrity and SBOM; vulnerability scan remains later gate |
| SECURITY-11~15 | N/A | no rate/auth/audit/runtime failure behavior implemented yet |

**Blocking findings**: none.

## 2026-08-31T19:10:00+09:00 - Steps 7~8 Flyway and JPA/H2 Persistence

- **Changed files**: Flyway V1 schema, JPA entities/repositories/adapters, bounded query models,
  persistence and transaction integration tests, Flyway/H2 build configuration.
- **Stable IDs**: FR-001, FR-002, FR-005, FR-010, FR-012, FR-013; NFR-003, NFR-005~NFR-007;
  SECURITY-01, SECURITY-05, SECURITY-09, SECURITY-13, SECURITY-15.
- **Result**: PASS.
- **Evidence**: `./mvnw verify` passed 32 tests plus 1,400 generated property checks; persistence tests
  cover Flyway validation, aggregate round trips, stable bounded queries, append-only audit, stale writes,
  audit-failure rollback and a two-statement upper bound for scheduled page reads.

### Checklist Result

- [x] Flyway exclusively creates constrained tables and reviewed composite indexes; Hibernate runs in
  validate-only mode in the isolated test profile.
- [x] Query inputs are typed enums/dates/booleans and Spring Data binds all values; no user-controlled SQL,
  property name or sort expression reaches a query.
- [x] Backlog and list results are capped at 100, weekly reads use `WeekRange`, and all list sorts add TaskId
  as a stable secondary order.
- [x] `@Version` plus an adapter version precondition rejects stale aggregates before overwrite.
- [x] Task mutation and structural audit append share one Spring transaction; a forced audit failure proves
  the task insert is rolled back.
- [x] The audit port and repository expose append only; events store IDs, allowlisted action/actor/request ID,
  time and changed field names, never title, description or raw request content.
- [x] H2 is pinned to Flyway's verified 2.3.232 compatibility level after a reproducible 2.4.240 constraint
  evaluator failure; no console, TCP listener or usable credential was added.
- [x] Entity graphs prevent per-row schedule loading; Hibernate statistics enforce at most content plus count
  statements for a scheduled task page.
- [x] Synthetic test fixtures contain no secret, credential or personal data; errors use controlled domain or
  application exception types without SQL/path/stack disclosure.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-01 | PASS | isolated in-memory H2 and constrained Flyway schema; encrypted file profile remains Step 12 |
| SECURITY-05 | PASS | typed filters, bound predicates, size allowlists and domain rehydration validation |
| SECURITY-09 | PASS | supported pinned H2/Flyway combination; no console/sample surface |
| SECURITY-13 | PASS | append-only, content-free structural audit persistence |
| SECURITY-15 | PASS | stale-write rejection and tested atomic rollback on audit failure |
| SECURITY-02~04, SECURITY-06~08, SECURITY-10~12, SECURITY-14 | N/A | HTTP/platform boundary is introduced in Steps 9~12 |

**Blocking findings**: none.

## 2026-08-31T18:49:38+09:00 - Steps 5~6 Application Tests and Services

- **Changed files**: application commands, ports, audit models, typed failures/outcomes,
  `PlanningService`, fake-port tests.
- **Stable IDs**: FR-001~FR-010, FR-013; NFR-001, NFR-003, NFR-005~NFR-008; SECURITY-05,
  SECURITY-13, SECURITY-15.
- **Result**: PASS.
- **TDD evidence**: application tests first failed compilation for absent ports/services; after minimum
  implementation, `./mvnw verify` passed 24/24 example/PBT/architecture tests.

### Checklist Result

- [x] Commands use typed IDs/enums/date/time/version; domain value constructors revalidate content and
  ranges. HTTP length/body/request-ID validation remains mandatory at the later DTO boundary.
- [x] No raw SQL, command, path, HTML, reflection or dynamic execution exists.
- [x] Conflict and stale-version paths perform zero save/audit calls; typed exceptions contain controlled
  messages only.
- [x] Material service methods have transaction boundaries so later Task+audit adapter writes roll back
  together; fake-port tests verify orchestration behavior.
- [x] Audit events contain IDs, allowlisted action/actor/request ID/time and structural field names only;
  title/description values are excluded. (SECURITY-13)
- [x] Delete requires explicit confirmation and expected version; all mutations recheck current version.
- [x] No endpoint, datasource, secret, log or external dependency was introduced in this batch.
- [x] Tests cover success, conflict, no-op, stale, destructive confirmation and bounded week behavior with
  synthetic data.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-05 | PASS | typed commands plus domain revalidation before writes |
| SECURITY-13 | PASS | append-only port and content-free structural audit model |
| SECURITY-15 | PASS | transactional methods, typed failures and no writes on rejected outcomes |
| SECURITY-01~04, SECURITY-06~12, SECURITY-14 | N/A | persistence/web/config/observability boundaries unchanged |

**Blocking findings**: none.

## 2026-08-31T18:44:03+09:00 - Step 4 Property and Architecture Tests

- **Changed files**: `SchedulePolicyPropertiesTest`, `ArchitectureTest`.
- **Stable IDs**: NFR-001, NFR-002, NFR-007; PBT-02, PBT-03, PBT-07, PBT-08, PBT-09.
- **Result**: PASS.
- **Evidence**: 4 jqwik properties executed 1,400 generated checks with reproducible seeds; 1 ArchUnit
  dependency rule and all example tests passed, 17 tests total.

### Checklist Result

- [x] Generators produce bounded dates, quarter times, durations and valid same-day slots only.
- [x] No raw user input, secret, DB/file/network access or runtime dependency was added.
- [x] Random failure output includes seed and shrinking is provided by jqwik; no failure required a fixed
  regression example in this batch.
- [x] Architecture verification prohibits Spring, JPA, servlet and SQL imports from domain code.
- [x] Test data is synthetic and no unsafe output, logging or dynamic code path exists.
- [x] Web, access, data-at-rest, transaction and runtime observability checklist items are N/A for test-only
  changes.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-05, SECURITY-15 | PASS | generated boundary/invariant verification and dependency isolation |
| SECURITY-01~04, SECURITY-06~14 | N/A | test-only domain verification; no runtime boundary changed |

**Blocking findings**: none.

## 2026-08-31T18:40:44+09:00 - Steps 2~3 Domain Tests and Implementation

- **Changed files**: domain tests plus `Task`, value objects, `ScheduleSlot`, `WeekRange`,
  `SchedulePolicy`, typed validation exception and enums.
- **Stable IDs**: FR-001, FR-003, FR-004, FR-006~FR-009; NFR-001, NFR-002, NFR-003, NFR-006,
  NFR-007; SECURITY-05, SECURITY-15.
- **Result**: PASS.
- **TDD evidence**: tests first failed compilation for absent domain types; after minimum implementation,
  `./mvnw verify` passed 12/12 tests and Spotless/SBOM gates.

### Checklist Result

- [x] Domain text, estimate, date/time and interval inputs have explicit null/length/range/alignment
  allowlists; Unicode length uses code points. (SECURITY-05)
- [x] Title/description remain untrusted value text and are neither rendered nor logged.
- [x] No SQL, command, file path, reflection, process execution or dynamic code API exists in domain code.
- [x] Failures expose controlled codes/messages only; no internal exception, path, SQL or stack detail is
  created for callers.
- [x] No API body/collection boundary exists yet; N/A until Steps 9~12.
- [x] State rules are enforced by constructors/aggregate operations, not by a future frontend.
- [x] No secret, credential, personal data, log statement or durable store is introduced.
- [x] Schedule and completion changes are immutable and reject backwards update time; persistence
  transaction/rollback is N/A until application/persistence steps.
- [x] Web headers/CORS/rate/auth are N/A because the domain has no HTTP dependency.
- [x] Domain imports no Spring, JPA, SQL or servlet class; automated `rg` inspection passed.
- [x] Boundary tests cover blank/oversize input, invalid estimate, 08:00/22:00, misalignment, touching and
  overlapping slots, next-day/week exhaustion, state idempotency and content-preserving unschedule.
- [x] Test fixtures use synthetic UUIDs/text/timestamps and contain no real secret or personal data.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-03, SECURITY-05 | PASS | untrusted text is bounded and never logged/rendered; typed allowlists |
| SECURITY-13, SECURITY-15 | Partial PASS | immutable decisions/no partial domain state; atomic persistence follows later |
| SECURITY-01~02, SECURITY-04, SECURITY-06~12, SECURITY-14 | N/A | no data, web, identity, supply-chain or observability boundary changed |

**Blocking findings**: none.

## 2026-08-31T19:58:00+09:00 - Step 9 REST Contract Tests First

- **Changed files**: `backend/pom.xml` (test-scoped `spring-boot-webmvc-test`),
  `AbstractApiContractTest`, `TaskCommandApiTest`, `TaskScheduleApiTest`, `PlanningQueryApiTest`,
  `ApiErrorContractTest`, `PlanningTestStore`, `PlanningTestStoreConfiguration`.
- **Stable IDs**: FR-001~FR-010, FR-013; NFR-001, NFR-003~NFR-006, NFR-008; SECURITY-05, SECURITY-09.
- **Result**: PASS (test-only batch).
- **TDD evidence**: 53 MockMvc contract tests execute and fail with `404`/missing-endpoint assertions
  because no controller or error mapper exists yet; the Spring context itself boots, so the red state
  is caused by the absent web adapter and not by broken wiring.

### Checklist Result

- [x] Tests pin an explicit allowlist contract for every input: title/description code-point bounds,
  15-minute estimate grid, priority/status/sort/direction enums, ISO date/time, page `1~100` and
  `expectedVersion`. (SECURITY-05)
- [x] `assertSafeErrorPayload` asserts that no error body contains a framework package, Hibernate/SQL
  text, `com.timetable` class name, the literal `Exception` or a filesystem path.
- [x] Dedicated tests assert that rejected input is never echoed: oversized title/description, invalid
  priority, malformed UUID, malformed date and an injection-shaped sort value.
- [x] Conflict responses are asserted to carry slot times only and to contain no task title.
- [x] No test writes a secret, credential or personal datum; all fixtures are synthetic.
- [x] Persistence internals stay package-private; contract tests reach persisted state only through the
  narrow `PlanningTestStore` projection, so no test binds to a JPA entity.
- [x] The batch adds no endpoint, datasource, log statement or runtime dependency; the single new
  dependency `spring-boot-webmvc-test` is Spring Boot BOM-managed, test-scoped and Maven Central hosted.
- [x] Filters are disabled in these tests on purpose; request ID, CORS, headers, body limit and rate
  limiting are covered by the Step 11 platform tests and are not silently assumed to pass here.
- [x] Audit assertions read only action and changed-field names, proving content stays out of audit rows.
  (SECURITY-13)

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-05 | PASS | typed allowlist and boundary expectations pinned before implementation |
| SECURITY-03 | PASS | no-echo and no-internal-detail assertions on every error path |
| SECURITY-09 | PASS | test-scoped BOM-managed dependency; no console, sample route or extra surface |
| SECURITY-13 | PASS | audit projection asserts structural fields only |
| SECURITY-01~02, SECURITY-04, SECURITY-06~08, SECURITY-10~12, SECURITY-14, SECURITY-15 | N/A | test-only batch; runtime web/platform boundary lands in Steps 10~12 |

**Blocking findings**: none.

## 2026-08-31T20:41:00+09:00 - Step 10 REST Web Adapter, OpenAPI and Local API Docs

- **Changed files**: request/response DTOs, `QuarterHourEstimate(+Validator)`, `TaskViewMapper`,
  `TaskController`, `TaskScheduleController`, `WeeklyPlanController`, `ScheduleOutcomeResponder`,
  `ApiError`, `ApiErrorHandler`, `PlanningConfiguration`, `platform/RequestCorrelation`,
  `Task.update`, `PlanningService.update/unschedule/findById/weekPlan`, `UpdateTaskCommand`,
  `UnscheduleTaskCommand`, `WeeklyPlan`, `ScheduleOutcome.Committed`, `StaleTaskVersionException`,
  `TaskRepositoryPort.findScheduledInWeek` and its JPA query, `backend/openapi/planning-api.yaml`,
  `static/docs/*`, `OpenApiContractDriftTest`, `ApiDocumentationTest`, `backend/pom.xml`.
- **Stable IDs**: FR-001~FR-010, FR-012, FR-013; NFR-001, NFR-003~NFR-008; SECURITY-03, SECURITY-04,
  SECURITY-05, SECURITY-09, SECURITY-13, SECURITY-15.
- **Result**: PASS.
- **Automated evidence**: `./mvnw verify` exits 0 with 100/100 tests, 1,400 property checks, the
  architecture rule, the OpenAPI route-drift check and the Spotless format gate.

### A. Input and Output

- [x] Every request field has a type, size, range and format allowlist: `@NotBlank @Size(max=120)`
  title, `@Size(max=2000)` description, `Priority`/`TaskStatus`/`TaskSort`/`SortDirection` enums,
  `@QuarterHourEstimate` for the 15~840 minute grid, ISO date, `HH:mm` time and
  `@NotNull @PositiveOrZero` expected version. Page bounds are re-checked in `TaskListQuery`.
  (SECURITY-05)
- [x] Title and description cross the boundary as plain JSON text only; no template, HTML fragment or
  string-built markup exists in the adapter.
- [x] No SQL, command or path is built from user text. Filters map allowlisted enums to fixed
  specifications and sorting uses `TaskSort.property()`, never a client-supplied string.
- [x] `ApiError` carries an allowlisted code, an authored message, the request ID and allowlisted
  field names only. Automated tests assert that a rejected title, description, priority, UUID, date
  and injection-shaped sort value are never echoed, and that no framework type, Hibernate/SQL text,
  `com.timetable` class name, stack trace or filesystem path appears in any error body.
- [x] Collections are bounded: page size 1~100 (default 25), week reads are limited to seven days and
  the weekly backlog is capped at 100 rows. The HTTP body-size limit is Step 12 platform work and is
  explicitly still open.

### B. Access and Boundaries

- [x] Public routes are the nine documented `/api/v1/**` operations plus the local docs assets
  (`/docs/**`, `/openapi/planning-api.yaml`, `/webjars/swagger-ui/**`). Local use has no
  authentication by approved design; Step 12 must declare each of these `permitAll` explicitly and
  deny every unmatched route. (SECURITY-08)
- [x] N/A in this batch - loopback binding and CORS allowlist are configured and tested in Step 12.
- [x] Every state change is re-validated by the domain and application layer inside the transaction:
  version match, 15-minute alignment, planning window, half-open overlap and delete confirmation.
  The client cannot skip a rule by shaping a request.
- [x] N/A in this batch - rate limiting is introduced and tested in Steps 11~12.

### C. Data Protection

- [x] No secret, DB key, `.env` or H2 file is added or tracked; a repository scan found none.
- [x] N/A - the encrypted file datasource is Step 12 work.
- [x] The only new log statement is the unexpected-failure record, which logs an event name and the
  request ID and passes the throwable to the logger without ever serializing it to the client.
  No request body, task title, description or parameter value is logged. (SECURITY-03)
- [x] All mutations remain inside the existing `@Transactional` application boundary; a conflict,
  stale version, not-found or validation failure performs no write.
- [x] Audit rows keep IDs, action, actor, request ID, time and structural field names only; the new
  update path records `UPDATED:content` with no task content. (SECURITY-13)

### D. Web and Browser

- [x] The Swagger UI page loads its CSS, bundle and preset from this application's own
  `/webjars/**` path and reads the local contract, so no CDN or external origin is referenced.
  A test asserts the page and its initializer contain no `http://` or `https://` URL. (SECURITY-04)
- [x] No `eval`, `new Function`, dynamic script construction or unsafe HTML sink was added; the
  initializer only passes a local URL to Swagger UI.
- [x] No frontend storage, credential or permission datum is written.

### E. Exceptions and Observability

- [x] `ApiErrorHandler` is the single boundary: not-found, stale version, unconfirmed deletion,
  domain rule, bean validation, parameter validation, type mismatch, missing parameter, unreadable
  body, media type, method, unknown route and unexpected failure each map to one fixed status and
  code. Nothing reaches the default container error page. (SECURITY-15)
- [x] Every response body carries a request ID from `RequestCorrelation`, which reads the MDC entry
  the Step 12 filter will populate and otherwise generates a server-side value, so no request can
  answer without correlation. (NFR-008)
- [x] Expected validation and conflict outcomes are not logged as errors; only the unexpected branch
  logs, and it logs once. (SECURITY-03)

### F. Supply Chain and Configuration

- [x] Two dependencies were added: `spring-boot-webmvc-test` (test scope, Spring Boot BOM version)
  and `org.webjars:swagger-ui` 5.29.4 (runtime, static assets only, no executable server code and no
  new endpoint handler). Both come from Maven Central and carry Apache-2.0 licensing.
- [x] Versions resolve deterministically through the Spring Boot BOM and the pinned
  `swagger-ui.version` property. (SECURITY-10)
- [x] The OWASP vulnerability scan stays the separately gated `security-scan` profile command; the
  CycloneDX SBOM still generates during `verify`.
- [x] No H2 console, sample controller or springdoc runtime documentation endpoint was enabled. The
  contract is a static file, so `/v3/api-docs` and a live spec generator do not exist. The docs
  assets are deliberate and must be reviewed again when a non-local deployment is ever considered.
  (SECURITY-09)
- [x] Build outputs stay ignored; the published contract is copied from the single checked-in source
  during `process-resources`, and a test asserts the served bytes equal the checked-in file.

### G. Test Evidence

- [x] 64 MockMvc contract tests cover success, malformed, boundary, oversized, wrong-media-type,
  wrong-method, unknown-route, not-found, stale-version and conflict paths.
- [x] Safe error shape, no-echo behaviour, request-ID presence and the exact error field set are
  asserted automatically. Security headers, CORS and rate limiting remain Step 11 obligations and are
  not claimed here.
- [x] The conflict-on-resize behaviour was written as a failing test first and exposed a real defect:
  candidate search used the stored estimate instead of the proposed interval length. The fix derives
  the length from the proposed slot, so a resized placement can no longer be offered a candidate that
  is too short.
- [x] All fixtures are synthetic; no secret or personal datum appears in tests or the contract.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-03 | PASS | authored messages, no echo, no internal detail, single correlated failure log |
| SECURITY-04 | PASS | fully self-hosted documentation assets, no CDN or dynamic execution |
| SECURITY-05 | PASS | typed DTO allowlists plus domain revalidation before any write |
| SECURITY-08 | Partial PASS | public routes are enumerated here; explicit declaration and unmatched denial land in Step 12 |
| SECURITY-09 | PASS | no console, sample route or live spec endpoint; static contract only |
| SECURITY-10 | PASS | BOM-managed and pinned dependency versions, SBOM generated |
| SECURITY-13 | PASS | append-only structural audit including the new UPDATED action |
| SECURITY-15 | PASS | single safe error boundary, no write on any rejected outcome |
| SECURITY-01~02, SECURITY-06~07, SECURITY-11~12, SECURITY-14 | N/A | data-at-rest, identity, rate limit and health boundaries are Step 11~12 work |

**Blocking findings**: none. **Carried obligations**: SECURITY-08 route declaration, body-size limit,
CORS, security headers and rate limiting must be completed in Steps 11~12 before U1 closes.

## 2026-08-31T21:20:00+09:00 - Steps 11~12 Security, Configuration and Observability Platform

- **Changed files**: `PlatformProperties`, `PlatformConfiguration`, `PlatformSecurityConfiguration`,
  `RequestIdFilter`, `RequestBodyLimitFilter`, `RateLimitFilter`, `TokenBucketRateLimiter`,
  `SafeErrorWriter`, `FileDatasourceGuard`, `FileDatasourceValidator`, `application.yml`,
  `application-file.yml`, `application-test.yml` (renamed from the shadowing test `application.yml`),
  `SecurityPlatformTest`, `RateLimitTest`, `FileDatasourceGuardTest`, `backend/pom.xml`.
- **Stable IDs**: NFR-003, NFR-006, NFR-008; SECURITY-01, SECURITY-03~05, SECURITY-07~12,
  SECURITY-14, SECURITY-15.
- **Result**: PASS.
- **Automated evidence**: `./mvnw verify` exits 0 with 122/122 tests. 22 of them run the full filter
  chain, which the REST contract tests deliberately bypass.

### A. Input and Output

- [x] The `X-Request-Id` header is accepted only against `^[A-Za-z0-9._-]{8,64}$`; a script-shaped or
  200-character value is discarded and replaced by a server value, verified by test. (SECURITY-05)
- [x] No user string is rendered as HTML anywhere in the platform.
- [x] No SQL, command or path is built from request data in this batch.
- [x] Filter-level rejections use the same `ApiError` shape through `SafeErrorWriter`, so a 413 or 429
  exposes only a code, an authored message and the request ID.
- [x] The body limit rejects a declared length above 64 KiB before parsing; DTO size bounds and the
  1~100 page cap still bound everything that gets through.

### B. Access and Boundaries

- [x] Public routes are now declared explicitly: `/api/v1/**`, GET `/docs/**`, `/openapi/**`,
  `/webjars/**` and `/actuator/health`. `anyRequest().denyAll()` closes everything else, anonymous
  authentication is disabled, and a test proves an undeclared route returns 403. This closes the
  SECURITY-08 obligation carried from the Step 10 review.
- [x] The server binds `127.0.0.1` and CORS allows only the exact configured loopback origins.
  `PlatformProperties` throws at startup if any origin contains a wildcard, and a test proves an
  outside origin is refused with no `Access-Control-Allow-Origin` header. (SECURITY-08)
- [x] All state changes still run through the domain and application rules; the platform only rejects
  earlier, never approves.
- [x] The bounded token bucket runs before the dispatcher, so no command executes for a rejected
  request. Its client cache is a fixed-size LRU map and a limiter failure fails closed for every
  mutating method. (SECURITY-11, SECURITY-15)

### C. Data Protection

- [x] No secret, key, `.env` or database file is tracked. `application-file.yml` references
  `${PLANNING_DB_PASSWORD:}` with no usable default and the repository scan found no key material.
  (SECURITY-01, SECURITY-12)
- [x] The file profile requires `jdbc:h2:file:` plus `CIPHER=AES` and a non-blank runtime key;
  `FileDatasourceGuard` fails startup otherwise and its messages name only the environment variable,
  never the supplied value. A `tcp://` URL is rejected outright, so no H2 server or console can be
  reached. (SECURITY-01, SECURITY-09, SECURITY-12)
- [x] The log pattern emits timestamp, level, request ID and message only. No filter logs a header,
  body, parameter or key. (SECURITY-03)
- [x] Transaction and rollback behaviour is unchanged and still covered by the persistence tests.
- [x] Audit behaviour is unchanged; the correlation ID recorded on each event now comes from the
  filter rather than a per-request fallback. (SECURITY-13)

### D. Web and Browser

- [x] `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer` and a Content Security
  Policy are asserted by test. HSTS is explicitly disabled so plain local HTTP never claims transport
  security it does not have. (SECURITY-04)
- [x] The CSP allows `'unsafe-inline'` for styles only, because Swagger UI injects its own style
  elements. Script, object, frame-ancestors, base-uri and form-action stay restricted to `'self'` or
  `'none'`, and this relaxation is limited to styling with no script implication.
- [x] No CDN, external script or dynamic code execution was added.
- [x] No frontend storage or credential handling exists.

### E. Exceptions and Observability

- [x] Every boundary has an explicit rejection path: oversized body, exhausted bucket, denied route,
  unmatched route and unexpected failure all produce a controlled JSON body. (SECURITY-15)
- [x] The MDC entry is removed in a `finally` block on the success, rejection and exception paths, and
  a test asserts no correlation value leaks between requests. (NFR-008)
- [x] Structured output carries timestamp, level, request ID and message. (SECURITY-03)
- [x] Denials record no attacker-usable detail; the client receives a generic message plus the ID.

### F. Supply Chain and Configuration

- [x] No new dependency was added in this batch; only Spring Boot and Spring Security modules already
  present are configured.
- [x] Versions still resolve through the Spring Boot BOM and pinned properties. (SECURITY-10)
- [x] The OWASP scan remains the gated `security-scan` profile; the CycloneDX SBOM still generates.
- [x] Actuator access defaults to `none` with health-only exposure, `show-details: never` and
  `show-components: never`. Tests prove env, beans, mappings, configprops and loggers are unavailable
  and that no H2 console route exists. (SECURITY-09, SECURITY-14)
- [x] Every platform limit also has a code-level default in `PlatformProperties`, so a missing or
  replaced configuration file cannot silently widen a boundary. The test configuration was renamed to
  `application-test.yml` and activated by a surefire profile property, so it no longer shadows the
  production `application.yml` and tests now exercise the real platform settings.

### G. Test Evidence

- [x] Correlation, header, CORS, route-denial, body-limit, rate-limit and health behaviour each have a
  dedicated automated test rather than a configuration assertion.
- [x] Security headers, CORS, rate limiting and safe error responses are all automated. (checklist G2)
- [x] The datasource guard is tested for a missing key, a blank key, an unencrypted URL, a TCP URL and
  the valid case, including an assertion that a failure message never contains the supplied key.
- [x] All fixtures are synthetic; no real secret or personal datum exists in any test.

### Security Baseline Status

| Rules | Status for this change | Evidence |
|---|---|---|
| SECURITY-01 | PASS | encrypted-file requirement enforced at startup; no tracked database file |
| SECURITY-03 | PASS | request-ID allowlist, redacted log pattern, no echo in any rejection |
| SECURITY-04 | PASS | CSP, nosniff, frame deny, referrer policy; HSTS deliberately absent on HTTP |
| SECURITY-05 | PASS | header allowlist and body-size rejection before parsing |
| SECURITY-07 | PASS | stateless session policy, CSRF disabled only for the JSON API behind exact CORS |
| SECURITY-08 | PASS | every public route declared; all others denied |
| SECURITY-09 | PASS | health-only Actuator, no console, no TCP, no sample route |
| SECURITY-10 | PASS | no new dependency; BOM-pinned versions unchanged |
| SECURITY-11 | PASS | bounded token bucket with Retry-After, tested at the boundary |
| SECURITY-12 | PASS | runtime-injected key with no default and no key in any message |
| SECURITY-14 | PASS | sanitized status-only health; all other endpoints unavailable |
| SECURITY-15 | PASS | fail-closed limiter for mutations and one safe response per failure |
| SECURITY-02, SECURITY-06, SECURITY-13 | PASS (unchanged) | persistence, identity-free local scope and append-only audit are unaffected |

**Blocking findings**: none. All carried obligations from the Step 10 review are now closed.
