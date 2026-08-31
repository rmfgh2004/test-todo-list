# Application Design

## 1. Design Objective

타임테이블 기반 투두 리스트를 두 개의 독립 빌드 유닛으로 구성하되, 사용자의 생성→배치→
충돌 해결→완료 여정이 React UI, REST orchestration, domain rule과 H2 transaction을 끊김 없이
통과하도록 설계한다.

## 2. Architecture Decisions

| Decision | Selected | Rationale |
|---|---|---|
| Backend structure | domain/application/adapter | 충돌·상태 규칙을 Spring/JPA에서 격리하고 PBT 가능하게 함 |
| Frontend structure | app shell + feature modules | 화면 컴포넌트가 아닌 사용자 기능과 흐름 중심으로 응집 |
| API contract | backend OpenAPI source + generated frontend types | frontend/backend 계약 이탈을 CI에서 차단 |
| Client state | TanStack Query server state + local React UI state | 서버 단일 출처와 mutation rollback 경계 유지 |
| Persistence | ports + JPA/H2 adapter | domain/application을 DB 기술에서 분리 |
| Conflict authority | backend transaction-time validation | 조용한 덮어쓰기와 stale client 판정을 방지 |

## 3. Component Summary

### Backend Planning Core

- **Domain**: B-C01 Task Domain, B-C02 Schedule Policy.
- **Application**: B-C03 Planning Application Service, B-C04 Query Application Service.
- **Ports/Adapters**: B-C05 Persistence Ports, B-C06 JPA/H2 Adapter, B-C07 REST Web Adapter.
- **Cross-cutting**: B-C08 Security and Error Platform.

### Frontend Planning Experience

- **Shell**: F-C01 routes, navigation, providers and global boundary.
- **Planning**: F-C02 Weekly Planner, F-C03 Backlog, F-C05 Scheduling Interaction.
- **Task flows**: F-C04 Task Editor, F-C06 Conflict Resolution, F-C07 Task List.
- **Cross-cutting**: F-C08 typed API client and F-C09 accessible feedback.

Detailed responsibilities and traceability are in [components.md](components.md).

## 4. Contract Summary

- Task commands: create, update, delete, completion.
- Schedule commands: schedule/move, conflict response and unschedule.
- Queries: task detail, bounded/filterable list and week-scoped plan.
- Safe error: `code`, `message`, `requestId`, optional `fieldErrors`.
- Conflict: HTTP 409 with existing, proposed and optional next candidate slots.
- Contract source: backend OpenAPI with generated frontend transport types.

Method signatures are in [component-methods.md](component-methods.md).

## 5. Service Orchestration

### Mutation Boundary

Every backend mutation validates request shape, loads current state, applies domain rules, writes state
and appends an audit event inside one transaction. Validation, conflict or persistence failure produces
no partial state.

### Frontend Mutation Boundary

Feature coordinators snapshot affected query caches before an optimistic change. Server success replaces
the snapshot; conflict or failure rolls back and opens accessible resolution/error feedback.

### Conflict Boundary

Client-side slot geometry provides immediate preview only. Backend schedule policy and current H2 data
are the final authority. A conflict never mutates stored state until the user submits a new explicit choice.

Detailed orchestration is in [services.md](services.md).

## 6. Dependency Direction

```text
Frontend route -> feature -> typed API adapter -> HTTP
HTTP -> security/error adapter -> application -> domain
application -> persistence port -> JPA/H2 adapter -> encrypted H2
```

- Domain has no Spring/JPA/HTTP dependency.
- Controllers cannot call concrete repositories.
- React views cannot use raw fetch or persistence concepts.
- Audit port exposes append only.

Matrices and diagrams are in [component-dependency.md](component-dependency.md).

## 7. User Experience Architecture

### Desktop

- Restrained app shell with week navigation and view tabs.
- Backlog rail and weekly grid share the primary workspace without nested decorative cards.
- Task details, conflict choices and destructive confirmation use accessible dialogs.
- Layout dimensions remain stable while loading, dragging and showing dynamic labels.

### Mobile

- Selected-day timeline replaces a compressed seven-column grid.
- Backlog opens as a dedicated panel; create, time entry and completion remain first-class.
- Drag has direct date/time form alternatives and every status change has aria-live feedback.

### Interaction States

- Loading skeleton, true empty, filtered empty, pending mutation, conflict, safe error, success and rollback
  are explicit states owned by feature coordinators and the feedback system.

## 8. Data and Consistency

- Task aggregate owns content, completion and optional schedule slot.
- Schedule uses Asia/Seoul, LocalDate/LocalTime transport and 15-minute invariant.
- Conflict queries compare half-open intervals `[start, end)` and exclude the moving task itself.
- Optimistic versioning and transaction-time revalidation prevent stale writes.
- Flyway manages schema; local file H2 uses AES and tests use isolated memory DB.

## 9. Alternatives Rejected

| Alternative | Reason Rejected |
|---|---|
| Technical-layer-only frontend folders | Encourages isolated components without complete user workflows |
| Controller/service/repository-only backend | Leaks persistence/framework details into collision business rules |
| Duplicated manual API types | Contract drift and runtime errors across independent units |
| One global frontend store | Duplicates server data and complicates rollback/invalidation |
| Client-authoritative conflict checks | Cannot protect against stale data or concurrent schedule mutations |

## 10. Security Baseline Compliance

| Rules | Status | Design Evidence |
|---|---|---|
| SECURITY-01 | Compliant | encrypted H2 adapter, environment-provided key; embedded transport N/A |
| SECURITY-02, SECURITY-06 | N/A | no intermediary or cloud IAM |
| SECURITY-03~05 | Compliant | B-C08 structured log/header/validation and safe DTO boundary |
| SECURITY-07~11 | Compliant | loopback CORS, public route declaration, hardening, supply chain and rate limit boundaries |
| SECURITY-12 | N/A | no authentication; database key still externalized |
| SECURITY-13 | Compliant | append-only audit port and generated contract integrity |
| SECURITY-14 | N/A | no cloud monitoring; request-ID error observability remains applicable |
| SECURITY-15 | Compliant | global error mapping, transaction rollback and cache rollback |

**Blocking findings**: 없음.

## 11. PBT Partial Compliance

- **PBT-02**: date/time parse-format transport round trip in backend/frontend boundaries.
- **PBT-03**: end-time, 15-minute and overlap invariants in B-C02 and F-C02/F-C05 helpers.
- **PBT-07**: reusable valid Task/ScheduleSlot generators per unit.
- **PBT-08**: jqwik/fast-check shrinking and seed logging in CI.
- **PBT-09**: jqwik and fast-check selected and assigned to Backend/Frontend units.

**Blocking findings**: 없음.

## 12. Handoff to Units Generation

- U1 receives B-C01~B-C08, S-B01~S-B04 and backend contract/testing responsibilities.
- U2 receives F-C01~F-C09, S-F01~S-F04 and responsive/accessibility/testing responsibilities.
- U1 owns OpenAPI; U2 consumes the generated client contract.
- Integration stories US-001~US-010 remain end-to-end and may span both units.

