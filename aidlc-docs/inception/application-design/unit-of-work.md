# Units of Work

## Decomposition Strategy

- **Unit type**: independently buildable modules within one local application.
- **Boundary driver**: runtime/toolchain ownership (Java backend vs React frontend).
- **Integration driver**: backend-owned OpenAPI, generated frontend transport types and real E2E.
- **Business consistency**: task and schedule remain inside one backend transaction boundary.

## U1 Backend Planning Core

### Mission

사용자 계획 데이터의 신뢰 가능한 단일 소스로서 task, schedule, conflict, persistence와 REST
계약을 구현한다.

### Owns

- B-C01~B-C08, S-B01~S-B04.
- Task aggregate, schedule policy, application commands/queries.
- `/api/v1` OpenAPI and safe error/conflict contracts.
- H2 file/memory profiles, Flyway schema, append-only audit.
- Validation, rate limit, security headers, CORS, request ID and structured logging.
- JUnit, MockMvc, H2 integration, jqwik, architecture and security tests.

### Does Not Own

- Browser layout, drag sensors, responsive behavior and frontend cache.
- User-facing copy and component composition.
- Cloud deployment or identity management.

### Primary Requirements

- FR-003, FR-004, FR-006~FR-009, FR-012, FR-013.
- NFR-002, NFR-003, NFR-005~NFR-008.

### Supporting Requirements

- FR-001, FR-002, FR-005, FR-010 through query APIs.
- FR-011 through safe/semantic API states consumed by accessible UI.
- NFR-001 through backend test and traceability gates.

### Completion Criteria

- OpenAPI and all backend tests pass with required coverage.
- Conflict calculation and 15-minute invariants pass example and jqwik tests.
- File and memory H2 profiles, migrations, rollback and audit tests pass.
- Applicable Security Baseline and code security checklist items pass.
- U2 can generate client types and complete contract tests from U1 output.

## U2 Frontend Planning Experience

### Mission

미배치 할 일을 주간 실행 계획으로 전환하는 완결된 데스크톱·모바일·키보드 사용자 경험을
구현한다.

### Owns

- F-C01~F-C09, S-F01~S-F04.
- App shell, weekly/day timetable, backlog, task editor, list and conflict dialog.
- Drag/drop and direct date/time alternative, optimistic cache rollback.
- Responsive layout, accessibility, explicit loading/empty/error/success states.
- Generated OpenAPI client adapter and query/mutation cache policy.
- Vitest, React Testing Library, fast-check and Playwright desktop/mobile tests.

### Does Not Own

- Authoritative conflict decision, durable state or database schema.
- Manual transport type definitions that duplicate OpenAPI.
- Authentication, team collaboration, board, recurrence or attachments.

### Primary Requirements

- FR-001~FR-006, FR-008~FR-011.
- NFR-001, NFR-004, NFR-005, NFR-007, NFR-008.

### Supporting Requirements

- FR-007 by rendering and resolving U1 conflict responses.
- FR-012 and FR-013 through truthful persistence/error feedback.
- NFR-002 and NFR-003 through time helper PBT and frontend security controls.

### Completion Criteria

- Desktop and 320px mobile user journeys pass Playwright against real U1.
- All key actions are keyboard operable with correct focus and aria-live behavior.
- API types are generated and contract drift check passes.
- Frontend unit/component/PBT coverage thresholds pass.
- Applicable Security Baseline and code security checklist items pass.

## Greenfield Code Organization

```text
workspace-root/
  backend/
    pom.xml
    src/main/java/.../planning/
      domain/
      application/
      adapter/in/web/
      adapter/out/persistence/
      platform/
    src/main/resources/
      db/migration/
    src/test/java/.../planning/
  frontend/
    package.json
    package-lock.json
    src/
      app/
      features/timetable/
      features/backlog/
      features/task-editor/
      features/conflict-resolution/
      features/task-list/
      shared/api/
      shared/ui/
    tests/e2e/
  scripts/
  .github/workflows/
  aidlc-inputs/
  aidlc-docs/
```

## Shared Rules Without Shared Runtime Code

- Stable ID format, time policy and error schema are documented contracts.
- OpenAPI artifact is generated from U1 and consumed by U2.
- No shared source directory contains mutable business logic across Java and TypeScript.
- Frontend time helpers may mirror display geometry but cannot authorize persistence.

## Unit Delivery Order

1. U1 domain types, policy tests and OpenAPI skeleton.
2. U1 persistence/security/API vertical slices.
3. U2 shell and typed mock client from approved OpenAPI.
4. U2 feature slices, replacing mocks with real U1 per story.
5. Integrated E2E, security, dependency and SBOM verification.

