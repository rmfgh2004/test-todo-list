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
