# U1 Business Rules

## Task Content Rules

| Rule | Requirement | Definition | Failure |
|---|---|---|---|
| BR-001 | FR-003 | title is trimmed, nonblank and 1~120 Unicode characters | ValidationFailure `TASK_TITLE_INVALID` |
| BR-002 | FR-003 | description is optional and at most 2,000 Unicode characters | `TASK_DESCRIPTION_TOO_LONG` |
| BR-003 | FR-003 | priority is exactly LOW, MEDIUM or HIGH | `TASK_PRIORITY_INVALID` |
| BR-004 | FR-003 | estimateMinutes is 15~840 and divisible by 15 | `TASK_ESTIMATE_INVALID` |
| BR-005 | FR-003 | dueDate is optional ISO local date under Asia/Seoul policy | `TASK_DUE_DATE_INVALID` |
| BR-006 | FR-004 | update uses the same validation as create and changes only supplied fields | field-specific validation |
| BR-007 | FR-004 | title/description are treated as text and never interpreted as HTML | input rejected or safely represented |

## Time and Schedule Rules

| Rule | Requirement | Definition | Failure |
|---|---|---|---|
| BR-010 | FR-006 | schedule date and time use LocalDate/LocalTime with fixed Asia/Seoul policy | `SCHEDULE_TIME_INVALID` |
| BR-011 | FR-006 | start minute is one of 00, 15, 30, 45 and seconds are absent | `SCHEDULE_ALIGNMENT_INVALID` |
| BR-012 | FR-006 | start >= 08:00 and computed end <= 22:00 on the same date | `SCHEDULE_OUT_OF_WINDOW` |
| BR-013 | FR-006 | end = start + estimateMinutes; client-provided end is ignored/rejected | `SCHEDULE_END_NOT_ACCEPTED` |
| BR-014 | FR-007 | overlap iff `a.start < b.end && b.start < a.end` | typed conflict, no mutation |
| BR-015 | FR-007 | moving task ID is excluded from its own overlap query | internal invariant |
| BR-016 | FR-007 | completed tasks do not block new schedules | query/domain invariant |
| BR-017 | FR-007 | next candidate search stays within remaining selected week and daily window | candidate or none |
| BR-018 | FR-007 | accepting a candidate always revalidates current persisted schedules | conflict or success |
| BR-019 | FR-008 | unschedule removes only ScheduleSlot and is idempotent | current task view |

## State and Concurrency Rules

| Rule | Requirement | Definition | Failure |
|---|---|---|---|
| BR-020 | FR-009 | completion command sets desired final state and is idempotent | current task view |
| BR-021 | FR-004~009 | mutable commands require expectedVersion | `TASK_VERSION_REQUIRED` |
| BR-022 | NFR-006 | version mismatch never overwrites newer state | StaleTaskVersion 409 |
| BR-023 | NFR-006 | domain decision, task write and audit append commit atomically | rollback on failure |
| BR-024 | FR-004 | delete physically removes task after explicit confirmation contract | 204 or safe 404 |
| BR-025 | FR-013 | successful no-op idempotent commands do not create duplicate audit events | no new event |

## Query Rules

| Rule | Requirement | Definition | Failure |
|---|---|---|---|
| BR-030 | FR-001 | weekStart must be Monday; API does not silently normalize | `WEEK_START_INVALID` |
| BR-031 | FR-005 | backlog contains incomplete tasks with no ScheduleSlot | query invariant |
| BR-032 | FR-005 | backlog order: overdue/due date, priority HIGH→LOW, createdAt, TaskId | stable order |
| BR-033 | FR-010 | page size is 1~100, default 25 | `PAGE_SIZE_INVALID` |
| BR-034 | FR-010 | filters and sort fields are allowlisted typed enums | `TASK_QUERY_INVALID` |
| BR-035 | NFR-005 | weekly queries always include bounded seven-day range | `WEEK_RANGE_REQUIRED` |

## Audit Rules

| Rule | Requirement | Definition | Failure |
|---|---|---|---|
| BR-040 | FR-013 | every material mutation appends one audit event in same transaction | rollback mutation |
| BR-041 | FR-013 | action is CREATED, UPDATED, DELETED, SCHEDULED, MOVED, UNSCHEDULED, COMPLETED or REOPENED | internal invariant |
| BR-042 | NFR-003 | audit omits title, description, raw body, key and exception text | security review block |
| BR-043 | FR-013 | audit is append-only; no application update/delete interface | architecture test |
| BR-044 | FR-013 | event includes event/task IDs, actor, request ID, timestamp and changed field names | internal validation |

## API and Security Rules

| Rule | Requirement | Definition | Failure |
|---|---|---|---|
| BR-050 | NFR-003 | JSON content type and configured request-body maximum are mandatory | 415/413 safe error |
| BR-051 | NFR-003 | all DTO fields have explicit type/size/format validation | 400 field error |
| BR-052 | NFR-003 | persistence uses JPA parameter binding; raw input never forms query strings | security review block |
| BR-053 | NFR-003 | allowed origins are explicit configured loopback origins, never wildcard | CORS rejection |
| BR-054 | NFR-003 | public API requests pass rate limiting before application commands | 429 with Retry-After |
| BR-055 | NFR-008 | every response contains request ID; user errors expose no internal details | safe error |
| BR-056 | NFR-008 | unexpected failures are logged once with request ID and rolled back | 500 safe error |
| BR-057 | NFR-003 | H2 console and default credentials are disabled | startup/security test failure |

## Rule Precedence

1. Transport/body/rate limits run first.
2. DTO validation runs before data access.
3. Not-found/version checks run before domain mutation.
4. Domain/time/conflict rules determine business outcome.
5. Atomic persistence and audit run only for a successful material change.
6. Safe error mapping runs for every failure and never converts a failure to success.

## Test Obligation

- Each `BR-XXX` has at least one example-based unit or integration test.
- BR-004, BR-010~018 and BR-030 are evaluated for jqwik properties.
- BR-022~025 and BR-040~044 require H2 integration tests.
- BR-050~057 require MockMvc/security configuration tests.
- Test methods include related `FR_XXX`/`NFR_XXX`; BR IDs appear in display names or documentation.

