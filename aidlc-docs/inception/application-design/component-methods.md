# Component Method Contracts

상세 비즈니스 규칙과 알고리즘은 유닛별 Functional Design에서 정의한다. 이 문서는
컴포넌트 간 고수준 계약과 타입 경계만 고정한다.

## Shared Contract Types

```text
TaskId = UUID
TaskStatus = TODO | COMPLETED
Priority = LOW | MEDIUM | HIGH
LocalDateIso = YYYY-MM-DD
LocalTimeIso = HH:mm
RequestId = opaque string
```

## Backend Domain Methods

### B-C01 Task Domain

```java
// FR-003, FR-004
Task create(TaskId id, TaskDraft draft, Instant now);
Task update(TaskPatch patch, Instant now);

// FR-006, FR-008
Task schedule(ScheduleSlot slot, Instant now);
Task unschedule(Instant now);

// FR-009
Task setCompletion(TaskStatus status, Instant now);
```

- `TaskDraft` and `TaskPatch`: title, description, priority, estimateMinutes, optional dueDate.
- Domain validation failure returns a typed failure, not framework exceptions.

### B-C02 Schedule Policy

```java
// FR-006, NFR-006
ValidationResult validate(ScheduleSlot slot, int estimateMinutes);
LocalDateTime calculateEnd(LocalDateTime start, int estimateMinutes);

// FR-007
boolean overlaps(ScheduleSlot left, ScheduleSlot right);
Optional<ScheduleSlot> nextAvailable(ScheduleSlot proposed, List<ScheduleSlot> occupied);
```

- Methods are pure and deterministic under the fixed Asia/Seoul/15-minute policy.
- `overlaps` uses half-open intervals and is a PBT target.

## Backend Application Methods

### B-C03 Planning Application Service

```java
// FR-003
TaskView createTask(CreateTaskCommand command, RequestContext context);

// FR-004
TaskView updateTask(TaskId id, UpdateTaskCommand command, RequestContext context);
void deleteTask(TaskId id, RequestContext context);

// FR-006, FR-007
ScheduleResult scheduleTask(TaskId id, ScheduleCommand command, RequestContext context);
ScheduleResult moveTask(TaskId id, ScheduleCommand command, RequestContext context);

// FR-008
TaskView unscheduleTask(TaskId id, RequestContext context);

// FR-009
TaskView setTaskCompletion(TaskId id, TaskStatus status, RequestContext context);
```

- `ScheduleResult` is either scheduled or conflict with existing/proposed slots and a next-slot candidate.
- All mutations wrap state change and FR-013 audit append in one transaction.

### B-C04 Query Application Service

```java
// FR-001, FR-005
WeeklyPlanView getWeeklyPlan(LocalDate weekStart, RequestContext context);

// FR-010
Page<TaskListItem> listTasks(TaskListQuery query, RequestContext context);

// FR-004
TaskView getTask(TaskId id, RequestContext context);
```

- `TaskListQuery` accepts allowlisted status, scheduled, priority, sort and bounded page size.

## Backend Port Methods

### B-C05 Persistence Ports

```java
Optional<Task> findTask(TaskId id);
Task saveTask(Task task);
void deleteTask(TaskId id);
List<Task> findTasksInRange(LocalDateTime start, LocalDateTime end);
Page<Task> findTasks(TaskQuerySpec spec);
void appendAudit(AuditEvent event);
```

- No audit update/delete method exists.
- Repository inputs are typed; no raw query string crosses the port.

### B-C07 REST Web Adapter

```text
GET    /api/v1/tasks/{id}
POST   /api/v1/tasks
PATCH  /api/v1/tasks/{id}
DELETE /api/v1/tasks/{id}
GET    /api/v1/tasks
GET    /api/v1/planning/weeks/{weekStart}
PUT    /api/v1/tasks/{id}/schedule
DELETE /api/v1/tasks/{id}/schedule
PUT    /api/v1/tasks/{id}/completion
GET    /api/v1/health
```

- Mutation request DTOs use Bean Validation and explicit maximum sizes.
- `409 Conflict` returns existing/proposed/candidate slot data without internal entity details.
- Errors use `{code, message, requestId, fieldErrors}`.

## Frontend Hook and Component Methods

### F-C02 Weekly Planner Feature

```typescript
// FR-001, FR-002
useWeeklyPlan(weekStart: DateIso): WeeklyPlanQuery;
toGridPosition(slot: ScheduleSlot): GridPosition;
selectWeek(current: DateIso, direction: 'previous' | 'next' | 'today'): DateIso;
```

### F-C03 Backlog Feature

```typescript
// FR-005
useBacklog(weekStart: DateIso): BacklogQuery;
sortBacklog(items: TaskListItem[]): TaskListItem[];
```

### F-C04 Task Editor Feature

```typescript
// FR-003, FR-004
useCreateTask(): TaskMutation<CreateTaskInput>;
useUpdateTask(taskId: TaskId): TaskMutation<UpdateTaskInput>;
useDeleteTask(taskId: TaskId): TaskMutation<void>;
validateTaskForm(input: TaskFormValues): FieldErrors;
```

### F-C05 Scheduling Interaction Feature

```typescript
// FR-006, FR-008
useScheduleTask(taskId: TaskId): ScheduleMutation;
useUnscheduleTask(taskId: TaskId): TaskMutation<void>;
toSlotProposal(drop: DropPosition, estimateMinutes: number): ScheduleInput;
rollbackSchedule(previous: WeeklyPlan): void;
```

### F-C06 Conflict Resolution Feature

```typescript
// FR-007
openConflict(result: ConflictResult): void;
resolveConflict(choice: 'keep-existing' | 'move-proposed' | 'cancel'): Promise<void>;
closeConflict(): void;
```

### F-C07 Task List Feature

```typescript
// FR-009, FR-010
useTaskList(query: TaskListQuery): TaskListQueryResult;
useSetCompletion(taskId: TaskId): TaskMutation<TaskStatus>;
parseTaskListQuery(search: URLSearchParams): TaskListQuery;
serializeTaskListQuery(query: TaskListQuery): URLSearchParams;
```

### F-C08 API Client and Contract Adapter

```typescript
request<TResponse, TBody = never>(contract: ApiContract<TResponse, TBody>): Promise<TResponse>;
normalizeApiError(error: unknown): SafeApiError;
```

- Request and response types are generated from the backend OpenAPI document.
- Unknown responses are runtime-validated before entering the query cache.

## Cross-Cutting Error Contract

- Domain/application methods return typed business failures for validation, not-found and conflict.
- Web adapter maps typed failures to 400/404/409 and unexpected failures to safe 500.
- Frontend treats 409 as a conflict state, field errors as form state, and other safe errors as retryable feedback.
- Every HTTP response carries a request ID that is displayed on failures and logged server-side.

