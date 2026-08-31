# U1 Backend Planning Core Logical Components

## 1. Component Model

```text
[Local React client]
        |
        v
[B-N01 HTTP Platform Chain]
        |
        v
[B-C07 REST Web Adapter] ---> [B-N02 Safe Error Mapper]
        |
        v
[B-C03/C04 Application Services] ---> [B-N03 Transaction Coordinator]
        |                                      |
        v                                      v
[B-C01/C02 Domain]                    [B-C05 Persistence Ports]
                                               |
                                               v
                                      [B-C06 JPA/H2 Adapter]
                                               |
                                    +----------+----------+
                                    v                     v
                           [B-N04 Flyway Schema]  [B-N05 Encrypted H2]

[B-N06 Observability] observes boundaries without receiving task content
[B-N07 Verification Gate] validates all components and contracts
```

The NFR components are logical responsibilities within U1, not separately deployed services.

## 2. Component Responsibilities

### B-N01 HTTP Platform Chain

**Owns**: request ID lifecycle, body/media limits, bounded rate limiting, explicit route authorization,
loopback CORS and response security headers.  
**Inputs**: servlet request plus validated configuration.  
**Outputs**: admitted request or safe `413`, `415`, `429`/denial response.  
**Dependencies**: `Clock`, bounded rate-limit store, Spring Security configuration.  
**Traceability**: NFR-003, NFR-008; SECURITY-03~05, SECURITY-07~11, SECURITY-15.

Execution order is fixed by explicit filter ordering and covered by MockMvc tests. MDC cleanup is mandatory
on success, rejection and exception paths.

### B-N02 Safe Error Mapper

**Owns**: the stable `ApiError(code, message, requestId, fieldErrors?)` and schedule conflict response.  
**Inputs**: allowlisted validation/domain/application exceptions plus unknown throwable boundary.  
**Outputs**: `400`, `404`, `409`, `413`, `415`, `429` or sanitized `500`.  
**Dependencies**: request-ID accessor and controlled message catalog.  
**Traceability**: NFR-003, NFR-004, NFR-008; SECURITY-03, SECURITY-05, SECURITY-09, SECURITY-15.

The mapper never serializes an exception or rejected field value. Unknown failures are logged once, while
the client receives a generic message and request ID.

### B-N03 Transaction Coordinator

**Owns**: application-service transaction boundaries for Task writes and append-only audit.  
**Inputs**: validated typed commands and persistence ports.  
**Outputs**: committed view or typed failure with no partial state.  
**Dependencies**: Spring transaction manager only at the application configuration edge.  
**Traceability**: FR-003, FR-004, FR-006~FR-009, FR-013; NFR-006; SECURITY-13, SECURITY-15.

It does not retry mutations. Integration tests replace or fail the audit/persistence port to prove rollback.

### B-N04 Schema and Query Optimizer

**Owns**: forward-only Flyway migrations, constraints, indexes and repository projection queries.  
**Inputs**: versioned SQL resources and fixed typed query parameters.  
**Outputs**: validated schema and bounded result sets.  
**Dependencies**: Flyway, JPA adapter and H2.  
**Traceability**: NFR-005, NFR-006, NFR-007; SECURITY-05, SECURITY-09, SECURITY-10.

Hibernate schema mutation is disabled in file mode. Migration validation fails startup on drift. Repository
tests verify ordering, overlap candidates, paging caps and query counts without asserting vendor internals.

### B-N05 Data Protection Configuration

**Owns**: isolated memory test datasource, required encrypted file datasource and safe secret handling.  
**Inputs**: profile and runtime environment value.  
**Outputs**: configured datasource or fail-fast startup.  
**Dependencies**: H2 driver and datasource configuration.  
**Traceability**: NFR-003, NFR-006; SECURITY-01, SECURITY-09, SECURITY-12, SECURITY-15.

It contains no fallback file key. It never starts H2 console/TCP and does not log resolved datasource URLs
when they could include sensitive values.

### B-N06 Observability and Health

**Owns**: structured event schema, request-ID correlation, redaction rules and sanitized liveness/readiness.  
**Inputs**: controlled event names, opaque identifiers and safe outcomes.  
**Outputs**: JSON logs and status-only health.  
**Dependencies**: SLF4J/Logback, MDC and constrained Actuator health groups.  
**Traceability**: NFR-008; SECURITY-03, SECURITY-09, SECURITY-14, SECURITY-15.

Task title/description and raw input never cross this component. Only health routes are exposed; all other
Actuator endpoints are unavailable over HTTP.

### B-N07 Verification and Supply-Chain Gate

**Owns**: reproducible build, test topology, property generators, coverage, architecture, contract,
vulnerability, SBOM and security-review evidence.  
**Inputs**: source, tests, Maven model and checklist.  
**Outputs**: PASS/BLOCKED build and review records.  
**Dependencies**: Maven Wrapper, JUnit, jqwik, MockMvc, ArchUnit, JaCoCo, Spotless, dependency scanner and
CycloneDX.  
**Traceability**: NFR-001, NFR-002, NFR-007; SECURITY-10; PBT-02, PBT-03, PBT-07, PBT-08, PBT-09.

An applicable security checklist failure or a coverage/contract/architecture failure blocks U1 completion.

## 3. Runtime Interaction Designs

### Mutation Success

```text
HTTP Platform -> Controller: admitted request + requestId
Controller -> Application: typed validated command
Application -> Persistence: load current task/version
Application -> Domain: decide valid next state
Application -> Persistence: save task + append audit (one transaction)
Persistence -> Application: committed view
Application -> Controller: safe response
Controller -> HTTP Platform: response + requestId + security headers
```

### Scheduling Conflict or Stale Version

```text
Application -> Persistence: bounded candidate/current-version read
Application -> Domain: re-evaluate current state
Domain --> Application: typed Conflict or StaleTaskVersion
Application --> Error Mapper: no mutation, transaction exits
Error Mapper --> Client: 409 + safe metadata + requestId
```

The client must issue a new command to accept a candidate or retry after reload. No server background action
changes the task.

### Persistence/Audit Failure

```text
Persistence --> Transaction Coordinator: failure
Transaction Coordinator -> H2: rollback task and audit
Safe Error Mapper -> Observability: log once with requestId, no content
Safe Error Mapper --> Client: sanitized 500 + requestId
```

## 4. Configuration Boundaries

| Configuration | Default/constraint | Failure behavior |
|---|---|---|
| server address | `127.0.0.1` | invalid/non-loopback production-like config is rejected unless explicitly reviewed |
| allowed origins | exact loopback frontend origins | empty/malformed/wildcard value fails startup |
| request body | fixed conservative byte limit | excess rejected with safe `413` before controller |
| page size | default 25, maximum 100 | invalid value returns field-safe `400` |
| rate limit | bounded capacity/refill/cache entries | exhaustion returns `429`; evaluator error fails closed for mutations |
| file datasource | AES cipher plus runtime secret | missing/malformed secret fails startup |
| schema | Flyway validate/migrate; ORM validate | unknown migration/drift fails startup |
| management exposure | health only | all other management HTTP routes unavailable |

Exact operational numbers not already fixed by requirements are typed configuration with conservative
defaults, documented in code-generation artifacts and boundary-tested. Secrets never have defaults.

## 5. Dependency Rules

1. B-C01/B-C02 domain imports no B-N or Spring/JPA/servlet type.
2. B-C03/B-C04 use B-C05 ports and domain types; transaction annotations/configuration may wrap the
   application boundary but cannot enter domain models.
3. B-C07 maps transport DTOs only and cannot call B-C06 repositories.
4. B-N01/B-N02 can call the web/application boundary but cannot access JPA entities.
5. B-N04/B-N05 implement outbound configuration and never leak entity/datasource types through ports.
6. B-N06 observes controlled metadata; business content is not an input dependency.
7. B-N07 is test/build-only and is absent from runtime production code.

ArchUnit and package tests enforce rules 1~5. Review and logging tests enforce rule 6.

## 6. Infrastructure Applicability

No infrastructure design stage is required for U1. H2 is embedded, and filters, transaction management,
migration, health and verification are in-process framework/tool components. Queue, broker, distributed
cache, circuit breaker, service discovery, external secret manager, proxy and cloud monitor are deliberately
absent. The encrypted database file and runtime environment value are local operational configuration, not
provisioned infrastructure.

## 7. Verification Matrix

| Component | Primary automated evidence |
|---|---|
| B-N01 | filter order, CORS/header/body/media/rate/request-ID MockMvc tests |
| B-N02 | status/schema/redaction and unknown-exception tests |
| B-N03 | transaction rollback, audit atomicity and optimistic-lock H2 tests |
| B-N04 | Flyway startup, constraints, stable order, bounded query and index review |
| B-N05 | isolated memory profile, encrypted file startup/restore and missing-key rejection |
| B-N06 | structured field/redaction/MDC cleanup and sanitized health tests |
| B-N07 | Maven verify, JaCoCo, ArchUnit, OpenAPI diff, jqwik, vulnerability and SBOM reports |
