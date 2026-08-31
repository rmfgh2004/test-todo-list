# U1 Backend Planning Core — Test Summary

`./mvnw verify` is the standard gate: **147 tests, 0 failures**, plus 1,400 generated property checks,
the architecture rule, the OpenAPI drift check, the coverage gates and the format check.

## Suites

| Suite | Tests | Covers |
|---|---|---|
| `domain/TaskTest`, `TaskContentEdgeCaseTest` | 12 | FR-003, FR-004, FR-006, FR-008: content validation, lifecycle, no-op transitions |
| `domain/SchedulePolicyTest`, `SchedulePolicyEdgeCaseTest` | 14 | FR-006, FR-007: window, alignment, half-open overlap, candidate search boundaries |
| `domain/SchedulePolicyPropertiesTest` | 4 properties | NFR-002, PBT-02/03/07/08/09: round trip, overlap symmetry, touching slots, generated valid slots |
| `ArchitectureTest` | 1 rule | NFR-007: the domain imports no Spring, JPA, servlet or SQL type |
| `application/PlanningServiceTest` | 7 | FR-001~FR-010, FR-013: orchestration with fake ports and an injected clock |
| `adapter/out/persistence/*` | 8 | FR-012, NFR-005, NFR-006: mapping, stable order, optimistic locking, audit rollback |
| `adapter/in/web/*ApiTest`, `ApiErrorContractTest`, `ApiErrorHandlerFailureTest` | 65 | the whole HTTP contract including every safe error path |
| `adapter/in/web/OpenApiContractDriftTest`, `ApiDocumentationTest` | 4 | NFR-007: contract matches the routes and the served bytes match the file |
| `platform/SecurityPlatformTest`, `RateLimitTest` | 15 | NFR-003, NFR-008: the full filter chain, headers, CORS, limits, health |
| `platform/PlatformPropertiesTest`, `TokenBucketRateLimiterTest`, `FileDatasourceGuardTest` | 17 | limits, bucket behaviour and encrypted-datasource fail-fast in isolation |

The REST contract tests run with servlet filters disabled so that a controller assertion cannot be
accidentally satisfied by a filter. `SecurityPlatformTest` is the counterpart that runs the chain for
real, so neither layer can hide a gap in the other.

## Gated Suites

Excluded from the ordinary loop because they are slow or environment-dependent:

| Command | Suite | Covers |
|---|---|---|
| `./mvnw -Pcapacity verify` | `CapacityAndLatencyTest` | NFR-005: 1,000-task read p95 against the 300 ms objective over two controlled runs after warm-up, 10,000-task bounded results, readiness |
| `./mvnw -Prestore verify` | `EncryptedFileRestoreTest` | FR-012, SECURITY-01: stop-copy-start restore, and no task title in clear text inside the database file |
| `./mvnw -Psecurity-scan verify` | OWASP Dependency-Check | SECURITY-10: needs vulnerability database access |

## Coverage

Enforced by JaCoCo during `verify`; the build fails rather than reporting a number:

| Scope | Gate | Actual |
|---|---|---|
| Bundle line | 80% | 98% |
| Bundle branch | 80% | 88% |
| `SchedulePolicy` branch | 90% | 93% |

`SchedulePolicy` carries the strictest gate because a wrong branch there double-books a user's day
silently instead of failing loudly.

## Naming

Every test method contains the stable ID it verifies (`FR_007_...`, `NFR_006_...`, `SECURITY_11_...`),
so requirement traceability is greppable from the test source.
