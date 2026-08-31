# Application Services and Orchestration

## Backend Services

### S-B01 Task Command Service

- **Components**: B-C01, B-C03, B-C05.
- **Responsibilities**: create/update/delete, completion state, validation and audit append.
- **Transaction boundary**: one command, one transaction including FR-013 audit event.
- **Failure policy**: validation before write; not-found is typed; DB failure rolls back state and audit together.
- **Traceability**: FR-003, FR-004, FR-009, FR-013; SECURITY-05, SECURITY-13, SECURITY-15.

### S-B02 Scheduling Service

- **Components**: B-C01, B-C02, B-C03, B-C05.
- **Responsibilities**: schedule/move/unschedule, final overlap query, conflict result and next candidate.
- **Transaction boundary**: load task → validate proposed slot → query overlapping rows → resolve outcome → save and audit.
- **Concurrency policy**: optimistic version check and transaction-time overlap revalidation; no last-write-wins.
- **Failure policy**: conflict returns typed 409 without mutation; persistence error rolls back all changes.
- **Traceability**: FR-006~FR-008, FR-013, NFR-002, NFR-006; SECURITY-11, SECURITY-13, SECURITY-15.

### S-B03 Planning Query Service

- **Components**: B-C04, B-C05.
- **Responsibilities**: weekly plan, backlog and paged list projections.
- **Read policy**: mandatory week/date bounds and page-size maximum; allowlisted sort fields.
- **Performance policy**: range query and projection DTOs avoid loading unrelated history.
- **Traceability**: FR-001, FR-002, FR-005, FR-010, NFR-005; SECURITY-05.

### S-B04 Web Security and Error Service

- **Components**: B-C07, B-C08.
- **Responsibilities**: request ID, body/content-type limits, validation mapping, CORS, rate limit, headers, safe errors and structured logs.
- **Ordering**: request ID → rate limit/body guard → validation/controller → safe exception mapping → security headers.
- **Trust policy**: all local endpoints are explicitly public but only allow configured loopback origins.
- **Traceability**: NFR-003, NFR-008; SECURITY-03~05, SECURITY-07~11, SECURITY-15.

## Frontend Services

### S-F01 Planning Query Coordinator

- **Components**: F-C01, F-C02, F-C03, F-C08, F-C09.
- **Responsibilities**: week route parsing, weekly/backlog query, skeleton/empty/error composition and retry.
- **Cache policy**: week-scoped keys; mutations invalidate only affected week and list keys.
- **Traceability**: FR-001, FR-002, FR-005, NFR-005, NFR-008.

### S-F02 Task Mutation Coordinator

- **Components**: F-C04, F-C07, F-C08, F-C09.
- **Responsibilities**: form validation, create/update/delete/completion mutations, pending state and safe feedback.
- **Optimistic policy**: completion may update cache optimistically with snapshot rollback; destructive delete waits for confirmation.
- **Traceability**: FR-003, FR-004, FR-009, NFR-003, NFR-004.

### S-F03 Schedule Interaction Coordinator

- **Components**: F-C02, F-C03, F-C05, F-C06, F-C08, F-C09.
- **Responsibilities**: drag/keyboard proposal, schedule mutation, 409 conflict transition, resolution retry and rollback.
- **State sequence**: idle → proposing → saving → scheduled | conflict | failed → idle.
- **Trust policy**: client slot calculation is preview only; server response replaces local proposal.
- **Accessibility policy**: drag has form alternative; every transition announces a concise aria-live status.
- **Traceability**: FR-006~FR-008, FR-011, NFR-002, NFR-004, NFR-006.

### S-F04 List Query Coordinator

- **Components**: F-C01, F-C07, F-C08, F-C09.
- **Responsibilities**: URL filter parsing, allowlisted query serialization, pagination and completion mutation integration.
- **Traceability**: FR-009, FR-010, NFR-004, NFR-005.

## End-to-End Orchestration Examples

### Create and Schedule

1. F-C04 validates and S-F02 sends create command.
2. B-C07 validates the DTO and S-B01 persists task plus audit.
3. S-F01 updates backlog and list caches from server result.
4. F-C05 proposes a slot by drag or form and S-F03 sends schedule command.
5. S-B02 validates time and overlap inside a transaction.
6. Success replaces cache data; conflict opens F-C06 without changing stored data.

### Resolve Conflict

1. B-C07 maps the S-B02 typed conflict to 409 with safe schedule summaries.
2. S-F03 rolls back optimistic placement and opens F-C06 with focus containment.
3. User selects keep, move candidate or cancel.
4. Move sends a new explicit schedule command; S-B02 revalidates current data before save.
5. F-C09 announces the final server result and focus returns to the initiating task.

### Complete and Recover From Failure

1. S-F02 snapshots affected caches and applies a pending completion state.
2. S-B01 updates task and appends audit in one transaction.
3. Success confirms server state across weekly/list queries.
4. Failure restores snapshots and shows safe error with request ID.

## Service-Level Rules

- No frontend service writes durable business state without the backend API.
- No backend service bypasses validation, domain rules or audit for mutations.
- No cross-feature cache mutation occurs without a captured rollback snapshot.
- No unexpected exception crosses the REST boundary with internal details.
- Application code cannot update or delete audit records.

