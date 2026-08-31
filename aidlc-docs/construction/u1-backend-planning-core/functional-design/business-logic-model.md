# U1 Business Logic Model

## 1. Command Processing Template

모든 mutation은 동일한 순서를 따른다.

1. Transport DTO의 type, size, format과 allowlist를 검증한다. (NFR-003, SECURITY-05)
2. DTO를 typed command로 변환하고 request ID를 유지한다.
3. 필요한 Task aggregate와 현재 주간 schedule snapshot을 읽는다.
4. domain invariant와 command별 precondition을 평가한다.
5. 성공 결과 또는 typed failure를 결정한다.
6. 성공이면 task 변경과 append-only audit event를 하나의 transaction으로 commit한다.
7. failure면 durable state를 변경하지 않고 안전한 오류로 반환한다. (SECURITY-15)

## 2. Task Lifecycle

| Current | Command | Next | Rule |
|---|---|---|---|
| none | Create | TODO + unscheduled | FR-003 validation required |
| TODO | Update | TODO | schedule/completion preserved unless explicit command |
| COMPLETED | Update | COMPLETED | content may change; completion preserved |
| TODO | Set COMPLETED | COMPLETED | idempotent |
| COMPLETED | Set TODO | TODO | idempotent |
| any | Delete | none | task removed, audit remains |
| unscheduled | Schedule | scheduled | conflict-free server validation |
| scheduled | Move | scheduled new slot | exclude self, revalidate conflict |
| scheduled | Unschedule | unscheduled | content preserved |
| unscheduled | Unschedule | unscheduled | idempotent |

## 3. Create Task Flow (FR-003, FR-005, FR-013)

### Input

`CreateTaskCommand(title, description?, priority, estimateMinutes, dueDate?)`.

### Transformation

- trim outer whitespace without altering internal or Unicode content.
- reject blank title, over-limit values and unsupported priority.
- construct Task with generated UUID, TODO, no ScheduleSlot, version 0 and timestamps.
- append CREATED audit event containing task ID and structural field names only.

### Output

`TaskView` with canonical persisted values and version.

## 4. Update/Delete Flow (FR-004, FR-013)

### Update

- load by TaskId or return `TaskNotFound`.
- validate patch fields and expected version.
- preserve schedule and status unless a dedicated command changes them.
- changing estimate on a scheduled task must keep its new computed end within 22:00 and recheck overlap.
- append UPDATED event with changed field names; do not store title/description values in audit.

### Delete

- require expected version and an explicit confirmed request contract.
- physically delete task and schedule data in one operation.
- append DELETED event without content payload in the same transaction.
- return 204; repeated delete after completion returns safe 404 rather than recreating audit noise.

## 5. Schedule/Move Flow (FR-006, FR-007, FR-013)

### Input

`ScheduleCommand(date, startTime, expectedVersion, resolutionMode)` where resolutionMode is
`PROPOSE_ONLY` or `ACCEPT_CANDIDATE`.

### Algorithm

1. Load task and ensure it is not deleted.
2. Construct proposed half-open slot `[start, start + estimate)`.
3. Validate 15-minute alignment, 08:00 inclusive lower bound and 22:00 inclusive end bound.
4. Query incomplete scheduled tasks overlapping the proposed date range, excluding the moving task ID.
5. Re-evaluate overlap in domain policy for every candidate.
6. If no overlap, save proposed slot with optimistic version and append SCHEDULED or MOVED audit.
7. If overlap, calculate the next available candidate and return typed `ScheduleConflict` without mutation.
8. `ACCEPT_CANDIDATE` is a new command and must repeat steps 1~6 against current data.

### Conflict Predicate (NFR-002, NFR-006)

For slots `a` and `b`:

```text
overlap(a, b) = a.start < b.end AND b.start < a.end
```

- predicate is symmetric.
- equal end/start boundary is not overlap.
- invalid or zero/negative slots never reach the predicate.

## 6. Next Available Slot Search (FR-007)

### Inputs

- proposed slot and task duration.
- occupied slots for the selected week, excluding the current task.
- week start Monday and fixed daily window 08:00~22:00.

### Procedure

1. Round proposed start up to the next 15-minute boundary only if necessary; normal commands are already aligned.
2. For each 15-minute candidate from proposed date/time through Sunday:
3. On a new day start at 08:00; stop candidate starts whose end would exceed 22:00.
4. Accept the first candidate that overlaps no occupied incomplete slot.
5. Return none when no candidate remains in the selected week.

### Properties

- returned slot is within the selected week and daily window.
- returned start is not earlier than the proposed start.
- returned duration equals task estimate.
- returned slot overlaps no occupied slot.
- with identical inputs the result is deterministic.

## 7. Unschedule Flow (FR-008, FR-013)

- load task or return `TaskNotFound`.
- if no slot exists, return current view without new audit event.
- remove ScheduleSlot only; preserve all other fields.
- append UNSCHEDULED event and commit atomically.

## 8. Completion Flow (FR-009, FR-013)

- accept desired final status and expected version.
- if current status already equals desired status, return current view without mutation/audit duplication.
- otherwise update status, append COMPLETED or REOPENED event and commit atomically.
- completed scheduled tasks remain visible on the timetable and are excluded from default backlog/conflict queries.

## 9. Query Models (FR-001, FR-002, FR-005, FR-010)

### Weekly Plan

- input must be a Monday `weekStart`; normalize is not silent, invalid date returns validation error.
- query scheduled tasks whose start date is within `[weekStart, weekStart + 7 days)`.
- query incomplete unscheduled tasks for backlog with due-date/priority stable order.
- output scheduled items, backlog items and week metadata.

### Task List

- filters: status, scheduled boolean, priority.
- sort allowlist: createdAt, dueDate, priority, title; stable secondary TaskId.
- page size 1~100, default 25.
- invalid filter/sort/page returns validation error, never raw query execution.

## 10. Typed Failures

| Failure | HTTP | Mutation | Safe Data |
|---|---|---|---|
| ValidationFailure | 400 | none | code, message, field errors, request ID |
| TaskNotFound | 404 | none | generic resource message, request ID |
| ScheduleConflict | 409 | none | task IDs, dates/times, optional candidate, request ID |
| StaleTaskVersion | 409 | none | generic refresh message, current version, request ID |
| RateLimitExceeded | 429 | none | retry-after and request ID |
| UnexpectedFailure | 500 | rollback | generic message and request ID only |

## 11. Transaction and Audit Model

- Create/update/delete/schedule/move/unschedule/completion each define one transaction boundary.
- audit append occurs after domain decision but before commit in the same transaction.
- audit data: event ID, task ID, action, actor=`local-user`, occurredAt, request ID, changed field names and allowlisted structural values.
- never audit title, description, DB key, stack trace or raw request body.
- application ports expose only `appendAudit`, never update/delete.

## 12. PBT Targets

- PBT-02: LocalDate/LocalTime API format round trip.
- PBT-03: overlap symmetry and boundary, end calculation, 15-minute/window invariants, candidate non-overlap.
- PBT-07: generators for valid/invalid TaskDraft, ScheduleSlot and occupied-week scenarios.
- PBT-08: shrinking and logged seed.
- PBT-09: jqwik integrated with JUnit 5.

