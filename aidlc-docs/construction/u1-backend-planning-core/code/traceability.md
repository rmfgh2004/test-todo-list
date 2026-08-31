# U1 Backend Planning Core — Traceability Evidence

Stable IDs run from requirements through design, code and tests. Every row below is verifiable by
grepping the ID in `backend/src`.

## Functional Requirements

| ID | Implementation | Primary tests |
|---|---|---|
| FR-001 weekly timetable | `WeeklyPlanController`, `PlanningService.weekPlan`, `TaskJpaRepository.findScheduledInWeek` | `PlanningQueryApiTest.FR_001_*` |
| FR-002 week navigation | Monday-anchored `WeekRange`, no silent normalization | `PlanningQueryApiTest.FR_001_rejects_week_start_that_is_not_monday` |
| FR-003 create task | `TaskContentRequest`, `Task.create`, value objects | `TaskCommandApiTest.FR_003_*`, `TaskTest` |
| FR-004 read, edit, delete | `TaskController`, `Task.update`, `DeleteTaskCommand` | `TaskUpdateApiTest.*`, `TaskCommandApiTest.FR_004_*` |
| FR-005 backlog | `TaskJpaRepository.findBacklog`, `WeeklyPlan` | `PlanningQueryApiTest.FR_005_*` |
| FR-006 place and move | `ScheduleTaskRequest`, `ScheduleSlot`, `Task.schedule` | `TaskScheduleApiTest.FR_006_*` |
| FR-007 conflict handling | `SchedulePolicy`, `PlanningService.detectConflict`, `ScheduleOutcomeResponder` | `TaskScheduleApiTest.FR_007_*`, `SchedulePolicyEdgeCaseTest`, jqwik properties |
| FR-008 unschedule | `PlanningService.unschedule`, `Task.unschedule` | `TaskScheduleApiTest.FR_008_*` |
| FR-009 completion | `SetCompletionRequest`, `Task.setCompleted` | `TaskCommandApiTest.FR_009_*` |
| FR-010 list view | `TaskListQuery`, specification mapping, `TaskPageView` | `PlanningQueryApiTest.FR_010_*` |
| FR-011 responsive flows | frontend responsibility (U2); the API returns semantic outcomes it needs | `ApiErrorContractTest.NFR_004_*` |
| FR-012 persistence | Flyway V1, JPA adapters, encrypted file profile | persistence tests, `EncryptedFileRestoreTest` |
| FR-013 audit | `AuditEvent`, `AuditPort`, `AuditPersistenceAdapter` | `TaskCommandApiTest.FR_013_*`, `PlanningTransactionTest` |

## Non-Functional Requirements

| ID | Implementation | Primary tests |
|---|---|---|
| NFR-001 test discipline | layered suites, stable-ID naming, JaCoCo gates | whole suite; gates enforced in `verify` |
| NFR-002 property-based testing | jqwik generators and properties | `SchedulePolicyPropertiesTest` (1,400 checks) |
| NFR-003 security baseline | ordered filters, typed DTOs, encrypted profile, safe errors | `SecurityPlatformTest`, `ApiErrorContractTest` |
| NFR-004 semantic errors | `ApiError` with field errors, conflict metadata and current version | `ApiErrorContractTest`, `TaskUpdateApiTest` |
| NFR-005 bounded reads | seven-day week, 1~100 page, capped backlog, composite indexes | `PlanningQueryApiTest.NFR_005_*`, `CapacityAndLatencyTest` |
| NFR-006 concurrency and atomicity | `@Version`, `StaleTaskVersionException`, one transaction per command | `TaskPersistenceAdapterTest`, `PlanningTransactionTest` |
| NFR-007 maintainability | hexagonal layering, ArchUnit, OpenAPI drift gate | `ArchitectureTest`, `OpenApiContractDriftTest` |
| NFR-008 observability | request correlation, redacted structured logs, sanitized health | `SecurityPlatformTest.NFR_008_*` |

## Security Baseline

All fifteen entries are evaluated per code batch in `security-review.md`. As of Step 14 every
applicable entry is PASS with no blocking finding and no carried obligation.

## Property-Based Testing Obligations

| ID | Evidence |
|---|---|
| PBT-02 | date and time API round trip properties |
| PBT-03 | overlap symmetry, touching boundaries, end calculation, candidate non-overlap |
| PBT-07 | reusable generators for valid tasks, slots and occupied weeks |
| PBT-08 | jqwik seed output and shrinking on failure |
| PBT-09 | jqwik runs inside the JUnit 5 platform during `verify` |

## User Stories

US-001 through US-010 are all covered by the endpoints and tests above. US-009 (accessible recovery)
and US-011-style responsive behaviour are backend-enabling only: the API returns the semantic status,
field errors and conflict metadata the U2 frontend needs, and the interaction itself is U2 scope.
