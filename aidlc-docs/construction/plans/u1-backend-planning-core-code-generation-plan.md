# U1 Backend Planning Core Code Generation Plan

This document is the single source of truth for U1 code generation. Steps execute sequentially and each
checkbox is updated immediately. TDD order is failing test, minimum implementation, then refactor. Every
code-change batch ends with the mandatory security checklist appended to
`aidlc-docs/construction/u1-backend-planning-core/code/security-review.md`.

## Part 1 Approval

- [x] Functional, NFR requirement and NFR design artifacts analyzed
- [x] Unit stories, dependencies, interfaces and owned entities analyzed
- [x] Workspace root and greenfield multi-unit code location validated
- [x] Exact generation paths and sequential TDD steps documented
- [x] FR/NFR, Security and PBT obligations mapped
- [x] Entire generation sequence explicitly approved

## Unit Context

- **Mission**: implement the authoritative local Task, schedule, conflict, audit, H2 and REST backend.
- **Application root**: `backend/` under the workspace root; application code never enters `aidlc-docs/`.
- **Primary components**: B-C01~B-C08 and B-N01~B-N07.
- **Primary requirements**: FR-003, FR-004, FR-006~FR-009, FR-012, FR-013; NFR-002, NFR-003,
  NFR-005~NFR-008.
- **Supporting requirements**: FR-001, FR-002, FR-005, FR-010, FR-011 and NFR-001.
- **Owned data**: Task aggregate, optional ScheduleSlot and append-only AuditEvent.
- **External dependency**: none at runtime. U2 later consumes U1's normalized OpenAPI contract over local
  HTTP; U1 does not depend on U2 source.
- **Contract**: `/api/v1/tasks`, `/api/v1/schedule`, bounded week/list queries, safe `ApiError`, typed `409`
  conflict/stale-version responses, request ID and health.

## Generation Steps

### Step 1 - Greenfield Backend Scaffold and Build Gate

- [x] Create `backend/pom.xml`, `.mvn/wrapper/`, `mvnw`, `mvnw.cmd` and Java 17 Spring Boot project metadata.
- [x] Add only reviewed dependencies/plugins for Web MVC, Validation, Data JPA, Security, Actuator, H2,
  Flyway, JUnit/MockMvc, jqwik, ArchUnit, JaCoCo, Spotless, OWASP scan and CycloneDX SBOM.
- [x] Create `backend/src/main/java/com/timetable/todo/PlanningApplication.java` and minimal test profile.
- [x] Add scaffold smoke test first, make it pass, run the code-batch security review, then mark Step 1.
- **Traceability**: FR-012; NFR-001, NFR-003, NFR-007, NFR-008; SECURITY-09, SECURITY-10.

### Step 2 - Domain Tests First

- [x] Create failing tests under `backend/src/test/java/com/timetable/todo/planning/domain/` for Task/value
  validation, lifecycle, completion, 15-minute scheduling, half-open conflict and next-slot search.
- [x] Include boundary examples for 08:00/22:00, touching slots, completed blockers, self-exclusion and
  week exhaustion; test names contain stable IDs.
- [x] Confirm the tests fail for missing implementation and record the TDD state.
- **Traceability**: FR-003, FR-004, FR-006~FR-009; NFR-001, NFR-002, NFR-006.

### Step 3 - Framework-Free Domain Implementation

- [x] Create domain aggregate/value/outcome/policy code under
  `backend/src/main/java/com/timetable/todo/planning/domain/` with no Spring/JPA/HTTP imports.
- [x] Implement BR-001~057 domain-relevant invariants and typed failures with Javadocs containing stable IDs.
- [x] Make Step 2 tests pass, refactor, perform the security review and immediately mark Steps 2~3.
- **Traceability**: FR-003, FR-004, FR-006~FR-009; NFR-002, NFR-006, NFR-007; SECURITY-05, SECURITY-15.

### Step 4 - Property-Based and Architecture Verification

- [x] Add reusable jqwik generators and properties for parse/format round trip, estimate/alignment/end,
  overlap symmetry/boundaries and valid Task/ScheduleSlot generation.
- [x] Add ArchUnit tests proving domain independence and inward adapter dependencies.
- [x] Preserve jqwik seed/shrunk input and add deterministic regression examples for discovered failures.
- [x] Make tests pass, run the security review and mark Step 4.
- **Traceability**: NFR-001, NFR-002, NFR-007; PBT-02, PBT-03, PBT-07, PBT-08, PBT-09.

### Step 5 - Application Port and Use-Case Tests First

- [x] Create failing application tests for CRUD, completion, schedule/move/unschedule, candidate acceptance,
  bounded week/backlog/list queries, audit generation, no-op behavior and typed failures.
- [x] Use in-memory fake ports and injected Clock; verify command ordering and no write on conflicts/failures.
- [x] Confirm failing state before implementation.
- **Traceability**: FR-001~FR-010, FR-013; NFR-001, NFR-006, NFR-008.

### Step 6 - Application Services and Ports

- [x] Create command/query models, inbound use cases and outbound Task/Audit/query ports under
  `backend/src/main/java/com/timetable/todo/planning/application/`.
- [x] Implement planning/query services, transactional orchestration boundary and safe typed outcomes.
- [x] Make Step 5 tests pass, refactor, run the security review and immediately mark Steps 5~6.
- **Traceability**: FR-001~FR-010, FR-013; NFR-005~NFR-008; SECURITY-13, SECURITY-15.

### Step 7 - Schema and Persistence Tests First

- [x] Create Flyway test expectations and failing isolated-H2 integration tests under persistence test paths.
- [x] Cover mapping round trips, append-only audit, optimistic locking, rollback on audit failure, stable
  backlog/list order, overlap candidates, bounded week/page queries and query-count hazards.
- [x] Confirm failing state before adapter/migration implementation.
- **Traceability**: FR-001, FR-002, FR-005, FR-010, FR-012, FR-013; NFR-005, NFR-006.

### Step 8 - Flyway and JPA/H2 Adapter

- [x] Create `backend/src/main/resources/db/migration/V1__create_planning_schema.sql` with constraints and
  approved indexes for `tasks`, `schedule_slots` and `audit_events`.
- [x] Create adapter-owned JPA entities, repositories, mappers, projections and port implementations under
  `backend/src/main/java/com/timetable/todo/planning/adapter/out/persistence/`.
- [x] Use bound typed queries, `@Version`, bounded results and no audit update/delete interface.
- [x] Make Step 7 tests pass, refactor, run the security review and immediately mark Steps 7~8.
- **Traceability**: FR-001~FR-010, FR-012, FR-013; NFR-003, NFR-005~NFR-007; SECURITY-01, SECURITY-05,
  SECURITY-09, SECURITY-13, SECURITY-15.

### Step 9 - REST Contract Tests First

- [x] Create failing MockMvc tests for task CRUD/completion, schedule conflict/candidate/unschedule, week,
  backlog/list/detail, DTO validation, pagination and all safe error status/schema cases.
- [x] Cover malformed, boundary, oversized, wrong-media and conflicting requests without echoing input.
- [x] Confirm failing state before controller implementation.
- **Traceability**: FR-001~FR-010, FR-012; NFR-001, NFR-003~NFR-005, NFR-008; SECURITY-05, SECURITY-09.

### Step 10 - REST Web Adapter and OpenAPI

- [x] Create dedicated request/response DTOs, mappers and controllers under
  `backend/src/main/java/com/timetable/todo/planning/adapter/in/web/`.
- [x] Create safe error/conflict mapper and a normalized OpenAPI contract at
  `backend/openapi/planning-api.yaml` without exposing JPA/domain/exception objects. The contract is
  authored directly and bound to the code by an automated route-drift test; a locally served Swagger UI
  renders it with no CDN asset.
- [x] Make Step 9 tests pass, refactor, run the security review and immediately mark Steps 9~10.
- **Traceability**: FR-001~FR-010, FR-012; NFR-003, NFR-004, NFR-007, NFR-008; SECURITY-03, SECURITY-05,
  SECURITY-09, SECURITY-15.

### Step 11 - Security Platform Tests First

- [x] Create failing MockMvc/configuration tests for request ID/MDC cleanup, loopback CORS, explicit public
  routes, unmatched denial, headers, body/media limit, bounded rate limit, safe failure and health exposure.
- [x] Add datasource-profile tests for isolated memory DB, missing file key rejection, AES file startup and
  H2 console/TCP disablement.
- [x] Confirm failing state before platform implementation.
- **Traceability**: NFR-003, NFR-008; SECURITY-01, SECURITY-03~05, SECURITY-07~12, SECURITY-14, SECURITY-15.

### Step 12 - Security, Configuration and Observability Platform

- [x] Implement ordered filters, Spring Security config, bounded token bucket, request correlation, structured
  redacted logging and constrained Actuator under `backend/src/main/java/com/timetable/todo/platform/`.
- [x] Create `application.yml`, `application-test.yml` and `application-file.yml` with loopback, Flyway,
  validate-only ORM and encrypted-file fail-fast settings; never include a usable secret.
- [x] Make Step 11 tests pass, refactor, run all SECURITY-01~15 checklist items and mark Steps 11~12.
- **Traceability**: NFR-003, NFR-006, NFR-008; SECURITY-01~15.

### Step 13 - Capacity, Coverage and Contract Gates

- [x] Add deterministic 1,000-task p95 fixture, 10,000-task capacity fixture, readiness smoke, encrypted DB
  backup/restore smoke and OpenAPI drift checks under backend test/tool paths.
- [x] Configure JaCoCo 80% line/branch overall and 90% collision-domain branch gates.
- [x] Run standard `./mvnw verify`; run/document separately gated performance, restore, dependency scan and
  SBOM commands where environment/runtime makes them unsuitable for every unit-test loop.
- [x] Resolve all failures without lowering thresholds; run the security review and mark Step 13.
- **Traceability**: NFR-001~NFR-008; SECURITY-01, SECURITY-03, SECURITY-09, SECURITY-10, SECURITY-15.

### Step 14 - Backend Documentation and U1 Evidence

- [x] Create `backend/README.md` covering memory/file startup, required secret injection, local URL, API,
  migration, backup/restore and verification commands without real secret examples.
- [x] Create/update `aidlc-docs/construction/u1-backend-planning-core/code/code-summary.md`,
  `test-summary.md`, `security-review.md` and traceability evidence.
- [x] Verify no secrets/H2 files, duplicate classes, unbounded API/query, unchecked plan item or applicable
  security failure remains; mark all implemented stories and Step 14.
- **Traceability**: FR-001~FR-013; NFR-001~NFR-008; SECURITY-01~15; selected PBT obligations.

## Story Coverage

| Story | U1 contribution | Steps |
|---|---|---|
| US-001 | week plan and task detail queries | 5~10 |
| US-002 | create task and backlog persistence | 2~10 |
| US-003 | edit/delete with confirmation contract and audit | 2~10 |
| US-004 | authoritative placement and time rules | 2~10 |
| US-005 | conflict response and next candidate | 2~10 |
| US-006 | move/unschedule with preserved content | 2~10 |
| US-007 | completion/reopen and audit | 2~10 |
| US-008 | bounded filtered list API | 5~10 |
| US-009 | semantic API outcomes supporting accessible alternatives | 9~10 |
| US-010 | persistence, security, observability and recovery foundation | 1, 4, 7~14 |

## Completion Gate

All 14 steps, story contributions, applicable Security Baseline entries and selected PBT obligations must be
complete. Generated code then receives the standardized Code Generation review gate before U2 begins.

**Status**: all 14 steps are complete. `./mvnw verify` exits 0 with 147 tests, 1,400 property checks, the
architecture rule, the OpenAPI drift check, 80% line/branch and 90% collision-branch coverage gates and the
format gate. Gated suites pass on demand: `-Pcapacity` (3 tests) and `-Prestore` (3 tests). Every applicable
Security Baseline entry is PASS with no blocking finding and no carried obligation. Awaiting the U1
completion approval gate.
