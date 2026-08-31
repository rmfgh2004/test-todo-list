# Application Components

## Backend Planning Core

### B-C01 Task Domain

- **Purpose**: 할 일의 내용, 우선순위, 예상 시간, 완료와 배치 상태를 표현한다.
- **Responsibilities**: domain invariant, 상태 전이, 15분 기간 검증, 배치 정보 보존·제거.
- **Interfaces**: 순수 Java domain types와 command/result objects.
- **Implements**: FR-003, FR-004, FR-008, FR-009, NFR-006.

### B-C02 Schedule Policy

- **Purpose**: 시간 범위와 충돌을 framework-independent 방식으로 판정한다.
- **Responsibilities**: 반개구간 overlap, 다음 빈 슬롯 후보, Asia/Seoul·15분 경계 검증.
- **Interfaces**: `SchedulePolicy` domain service.
- **Implements**: FR-006, FR-007, NFR-002, NFR-006.

### B-C03 Planning Application Service

- **Purpose**: task와 schedule use case를 transaction 단위로 orchestration한다.
- **Responsibilities**: CRUD, 배치·재배치·해제, 충돌 409 결과, 완료, 감사 이벤트.
- **Interfaces**: command/query use-case ports.
- **Implements**: FR-003~FR-009, FR-013.

### B-C04 Query Application Service

- **Purpose**: 주간·백로그·목록 조회 모델을 제공한다.
- **Responsibilities**: 기간·상태·우선순위 filter, pagination/정렬 allowlist, view DTO 조립.
- **Interfaces**: weekly plan and task list query ports.
- **Implements**: FR-001, FR-002, FR-005, FR-010, NFR-005.

### B-C05 Persistence Ports

- **Purpose**: domain/application을 JPA와 H2에서 분리한다.
- **Responsibilities**: task 저장·조회, 기간 충돌 조회, append-only audit 저장.
- **Interfaces**: `TaskRepositoryPort`, `ScheduleRepositoryPort`, `AuditRepositoryPort`.
- **Implements**: FR-012, FR-013, NFR-006, NFR-007.

### B-C06 JPA/H2 Adapter

- **Purpose**: persistence ports를 Spring Data JPA, Flyway와 H2로 구현한다.
- **Responsibilities**: entity mapping, query, optimistic locking, file/in-memory profile.
- **Interfaces**: persistence port implementations only.
- **Implements**: FR-012, NFR-003, NFR-006.

### B-C07 REST Web Adapter

- **Purpose**: `/api/v1` HTTP 계약을 use case ports에 연결한다.
- **Responsibilities**: Bean Validation, DTO mapping, status code, OpenAPI, request ID.
- **Interfaces**: task, weekly plan, schedule and health controllers.
- **Implements**: FR-001~FR-010, NFR-003, NFR-008.

### B-C08 Security and Error Platform

- **Purpose**: 모든 endpoint에 공통 보안·실패 정책을 적용한다.
- **Responsibilities**: loopback CORS, rate limit, security headers, body limits, safe global error, structured logging.
- **Interfaces**: filters, interceptors, exception handler and configuration.
- **Implements**: NFR-003, NFR-008, SECURITY-03~05, SECURITY-07~11, SECURITY-15.

## Frontend Planning Experience

### F-C01 Application Shell

- **Purpose**: 주간·목록 route, 공통 navigation, query/error provider를 제공한다.
- **Responsibilities**: route state, top navigation, theme tokens, global feedback boundary.
- **Interfaces**: React route composition.
- **Implements**: FR-002, FR-010, FR-011.

### F-C02 Weekly Planner Feature

- **Purpose**: 주간 또는 모바일 일 타임라인을 렌더링한다.
- **Responsibilities**: time geometry, week/day navigation, schedule blocks, today/weekend semantics.
- **Interfaces**: weekly query hook and scheduling callbacks.
- **Implements**: FR-001, FR-002, FR-006, FR-011.

### F-C03 Backlog Feature

- **Purpose**: 미배치 할 일을 계획 대상으로 제공한다.
- **Responsibilities**: stable ordering, task cards, drag source, empty/error state.
- **Interfaces**: backlog query and task selection callbacks.
- **Implements**: FR-005, FR-006, FR-011.

### F-C04 Task Editor Feature

- **Purpose**: 생성·수정·삭제와 직접 시간 배치 폼을 제공한다.
- **Responsibilities**: accessible dialog, client validation, mutation state, delete confirmation.
- **Interfaces**: task commands and schedule command hooks.
- **Implements**: FR-003, FR-004, FR-006, FR-011.

### F-C05 Scheduling Interaction Feature

- **Purpose**: drag/drop, keyboard alternative, optimistic preview와 rollback을 조정한다.
- **Responsibilities**: dnd sensor, slot proposal, server mutation, pending/rollback feedback.
- **Interfaces**: timetable/backlog adapters and schedule API client.
- **Implements**: FR-006, FR-008, NFR-004.

### F-C06 Conflict Resolution Feature

- **Purpose**: 서버 충돌 결과를 비교 가능한 선택 UI로 표현한다.
- **Responsibilities**: existing/proposed schedule summary, keep/move/cancel commands, focus trap and aria-live.
- **Interfaces**: conflict result and resolution callbacks.
- **Implements**: FR-007, FR-011, NFR-003, NFR-004.

### F-C07 Task List Feature

- **Purpose**: 전체 할 일의 필터·정렬·완료 상태 관리를 제공한다.
- **Responsibilities**: URL query filters, pagination, completion mutation, empty states.
- **Interfaces**: list query and task command hooks.
- **Implements**: FR-009, FR-010, FR-011.

### F-C08 API Client and Contract Adapter

- **Purpose**: generated OpenAPI types로 REST 통신을 캡슐화한다.
- **Responsibilities**: request ID propagation, error normalization, query keys, mutation invalidation.
- **Interfaces**: typed task, planning and health clients.
- **Implements**: NFR-003, NFR-007, NFR-008.

### F-C09 Accessible Feedback System

- **Purpose**: loading, success, empty, conflict와 failure를 일관되게 전달한다.
- **Responsibilities**: aria-live, toast/status, skeleton, retry and request ID display.
- **Interfaces**: status primitives and error boundary.
- **Implements**: FR-001, FR-003, FR-006, FR-011, NFR-004, NFR-008.

## Boundary Rules

- UI components는 API client를 직접 호출하지 않고 feature query/mutation hooks를 사용한다.
- REST controller는 persistence adapter를 직접 호출하지 않고 application port만 호출한다.
- domain은 Spring, JPA, HTTP, React와 OpenAPI generated type에 의존하지 않는다.
- 프론트의 시간 미리보기는 UX 보조이며 충돌·저장 최종 판정은 backend가 수행한다.
- audit records는 append-only port 외 update/delete interface를 제공하지 않는다.

