# Unit of Work Dependencies

## Dependency Matrix

| Consumer | Provider | Dependency | Build-Time | Runtime | Rule |
|---|---|---|---|---|---|
| U2 Frontend | U1 Backend | OpenAPI JSON/YAML contract | Yes | No | generated types only; no manual duplicates |
| U2 Frontend | U1 Backend | `/api/v1` HTTP API | No | Yes | typed client, safe error and request ID |
| U1 Backend | U2 Frontend | None | No | No | backend never imports frontend artifacts |
| Root integration | U1 Backend | backend executable and health | Yes | Yes | start before E2E |
| Root integration | U2 Frontend | built SPA/dev server | Yes | Yes | configured API origin only |

## Direction

```text
U1 OpenAPI contract -> U2 generated transport client
U2 browser request -> U1 REST API -> U1 domain/persistence
U1 safe response -> U2 query cache -> U2 accessible UI
```

There is no source-code or database dependency from U2 to U1. The only cross-unit contracts are
the versioned OpenAPI artifact, HTTP behavior and shared stable requirement IDs.

## Contract Ownership

| Artifact | Owner | Consumer | Validation |
|---|---|---|---|
| OpenAPI document | U1 | U2, integration tests | schema validation and committed diff check |
| Generated TypeScript client | U2 | U2 features | regeneration must produce clean diff |
| Safe error schema | U1 | U2 feedback system | backend examples + frontend runtime parsing |
| Conflict 409 schema | U1 | U2 conflict feature | contract test + Playwright conflict journey |
| Time policy | U1 authoritative | U2 display/proposal | shared documented examples and cross-unit E2E |

## Critical Path

1. U1 domain and initial API/error/OpenAPI contracts.
2. U2 client generation and mock contract adapter.
3. U1 vertical slice implementation and integration tests.
4. U2 feature implementation against typed mock, then real U1.
5. Root integrated E2E and security verification.

## Coordination Checkpoints

### CP-01 Contract Skeleton

- Task, WeeklyPlan, ScheduleResult, SafeApiError schemas validate.
- U2 client generation succeeds before full U1 implementation.

### CP-02 Task CRUD Slice

- U1 create/read/update/delete integration tests pass.
- U2 create/editor/backlog/list component tests pass.
- Playwright create and update flow passes against U1.

### CP-03 Scheduling Slice

- U1 overlap/PBT/transaction tests pass.
- U2 drag/direct-input/rollback/conflict tests pass.
- Playwright schedule and three conflict choices pass.

### CP-04 Completion and Query Slice

- U1 completion idempotency and list filter tests pass.
- U2 weekly/list cache synchronization and URL restore pass.

### CP-05 Final Quality Gate

- Both builds, coverage, contract, PBT and desktop/mobile E2E pass.
- Security checklist, dependency audits and SBOM pass.

## Failure and Rollback Strategy

- OpenAPI breaking change blocks U2 generation before merge.
- Failed backend slice does not advance its dependent frontend real-integration checkpoint.
- Frontend mocks remain contract-shaped and are not used as release evidence.
- DB migration failure blocks U1 startup; existing migration files are never rewritten.
- E2E failure blocks integrated completion even if unit-local tests pass.

## Prohibited Coupling

- U2 cannot access H2 files, database entities or Spring DTO implementation classes.
- U1 cannot serve behavior that exists only in frontend validation.
- Neither unit owns a separate copy of collision business truth.
- Root scripts orchestrate commands but contain no domain logic.
- No circular contract: U1 OpenAPI generation cannot require a U2 build.

