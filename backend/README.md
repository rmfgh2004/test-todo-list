# Timetable Todo — Backend

Local planning API for the timetable-based todo application. One Spring Boot process with an embedded
H2 database, no remote dependency and no user accounts: it is a single-user tool bound to loopback.

- Java 17, Spring Boot 4.1.1, Maven Wrapper
- Hexagonal layering: framework-free `planning.domain`, `planning.application` use cases, inbound
  `adapter.in.web`, outbound `adapter.out.persistence`, cross-cutting `platform`
- All wall times are Asia/Seoul, on a 15-minute grid, inside the 08:00~22:00 planning window

## Run

### In-memory (default)

```bash
./mvnw spring-boot:run
```

Serves `http://127.0.0.1:8080`. Data lives only for the life of the process. Use a different port
with `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`.

### Encrypted file database (development persistence)

The `file` profile stores data in an AES-encrypted local H2 file. The key is **never** stored in this
repository and has no default; startup fails without it.

```bash
export PLANNING_DB_PASSWORD='<file-encryption-key> <database-user-password>'
./mvnw spring-boot:run -Dspring-boot.run.profiles=file
```

The value is H2's composite password: the file encryption key, one space, then the user password.
Choose both yourself, keep them out of shell history and out of version control. `FileDatasourceGuard`
rejects startup when the key is missing, when the URL is not a local `jdbc:h2:file:` URL, or when
`CIPHER=AES` is absent, so an unencrypted database can never be created by accident. The H2 console
and TCP server are disabled in every profile.

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/planning/weeks/{weekStart}` | one Monday-anchored week plus the backlog |
| GET | `/api/v1/tasks` | filtered, bounded task page |
| POST | `/api/v1/tasks` | create a task in the backlog |
| GET | `/api/v1/tasks/{id}` | task detail |
| PATCH | `/api/v1/tasks/{id}` | replace the full content set |
| DELETE | `/api/v1/tasks/{id}` | delete after explicit confirmation |
| PUT | `/api/v1/tasks/{id}/schedule` | place or move on the timetable |
| DELETE | `/api/v1/tasks/{id}/schedule` | unschedule, keeping all content |
| PUT | `/api/v1/tasks/{id}/completion` | set the desired completion state |

The contract is `openapi/planning-api.yaml`. `OpenApiContractDriftTest` fails the build if the routing
table and the document disagree, so the file is always accurate.

Browse it while the application runs at **http://127.0.0.1:8080/docs/index.html**. Swagger UI is served
from this application's own assets, so the page works offline and needs no CDN.

### Contract notes

- Every mutation carries `expectedVersion`; a mismatch returns `409 STALE_TASK` with the current
  version and writes nothing.
- `PATCH` replaces the whole content set. An omitted `description` or `dueDate` clears that value.
  Completion and placement are changed only through their own endpoints.
- A changed estimate keeps an existing placement's start time and recalculates its end. If the longer
  block overlaps, the response is `409 SCHEDULE_CONFLICT` with the next free candidate and nothing is
  stored.
- Overlap uses half-open intervals, so touching slots (`09:30` end, `09:30` start) never conflict.
- Errors always use `{code, message, requestId}` plus `fieldErrors`, `currentVersion` or `conflict`
  where relevant. Rejected values, task content, SQL and stack traces are never returned.
- Every response carries an `X-Request-Id` header. A client-supplied value is honoured only when it
  matches `^[A-Za-z0-9._-]{8,64}$`.

## Database migrations

Flyway owns the schema (`src/main/resources/db/migration`). Migrations are forward-only; Hibernate is
restricted to `validate`, so schema drift fails startup instead of being silently repaired.

## Backup and restore

The encrypted file database is recovered with an offline stop-copy-start:

1. Stop the application.
2. Copy `data/planning.mv.db` to your backup location.
3. To restore, stop the application, put the file back, start with the same `PLANNING_DB_PASSWORD`.

`EncryptedFileRestoreTest` exercises exactly this flow and also asserts that task titles do not appear
in clear text inside the database file.

## Verification

```bash
./mvnw verify              # unit, integration, property, architecture, contract, coverage, format
./mvnw -Pcapacity verify   # 1,000-task read p95 and 10,000-task bounded-result fixtures
./mvnw -Prestore verify    # encrypted backup and restore smoke
./mvnw -Psecurity-scan verify   # OWASP Dependency-Check (needs vulnerability database access)
```

`./mvnw verify` is the standard gate: it must pass before any commit. It enforces 80% line and branch
coverage overall and 90% branch coverage on `SchedulePolicy`, because a wrong branch in the collision
rules silently double-books a day. Capacity, restore and the vulnerability scan run behind their own
profiles because they are far too slow or too environment-dependent for a TDD loop.

A CycloneDX SBOM is generated into `target/` during `verify`.

## Security posture

This service is deliberately unauthenticated and loopback-only. Before it is ever exposed beyond a
local machine, at minimum: add authentication, review the public route list in
`PlatformSecurityConfiguration`, remove or gate the `/docs` assets, enable HTTPS with HSTS, and revisit
the rate-limit policy in `planning.platform.rate-limit`.

Every code change batch appends a checklist result to
`../aidlc-docs/construction/u1-backend-planning-core/code/security-review.md`.
