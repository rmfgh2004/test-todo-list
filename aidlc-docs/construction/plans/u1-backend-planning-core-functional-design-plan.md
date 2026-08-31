# U1 Backend Planning Core Functional Design Plan

## Progress

- [x] Unit, story map and Application Design context loaded
- [x] Functional questions generated across all applicable categories
- [x] Default-A answers and rationale recorded
- [x] Answers checked for ambiguity and contradiction
- [x] `business-logic-model.md` generated
- [x] `business-rules.md` generated
- [x] `domain-entities.md` generated
- [x] FR/NFR, Security and PBT traceability validated

## Questions and Decisions

### Question 1 — Domain Model

A) Task aggregate owns an optional ScheduleSlot value object and completion state. (selected)

B) Task and Schedule are separate aggregates with independent lifecycle.

X) Other.

[Answer]: A

[Rationale]: task 배치·해제·완료와 audit을 한 transaction에서 다루고 별도 서비스 일관성 비용을 피한다.

### Question 2 — Deletion

A) Task row is physically deleted after confirmation; append-only audit metadata remains without title/description. (selected)

B) Soft-delete task rows indefinitely.

X) Other.

[Answer]: A

[Rationale]: 승인된 보존 정책과 개인정보 최소화를 따르면서 변경 증거는 유지한다.

### Question 3 — Next Available Slot

A) Search from proposed time in 15-minute increments through 08:00~22:00 of the remaining selected week; return none when unavailable. (selected)

B) Search indefinitely into future weeks.

X) Other.

[Answer]: A

[Rationale]: 사용자가 보고 있는 주간 맥락을 벗어난 조용한 자동 이동을 방지한다.

### Question 4 — Concurrency

A) Optimistic task version plus transaction-time overlap revalidation; stale writes return typed conflict. (selected)

B) Last-write-wins.

X) Other.

[Answer]: A

[Rationale]: 동시 변경에서 조용한 데이터 덮어쓰기를 차단한다.

### Question 5 — Audit Data

A) Store actor, action, task ID, timestamp, request ID and structural changed fields; omit title and description values. (selected)

B) Store full before/after task payloads.

X) Other.

[Answer]: A

[Rationale]: 추적 가능성과 사용자 입력 최소 보관을 균형 있게 만족한다.

### Question 6 — Time Contract

A) Use LocalDate and LocalTime under fixed Asia/Seoul policy; reject offsets/timezones in API input. (selected)

B) Accept arbitrary client timezones.

X) Other.

[Answer]: A

[Rationale]: 첫 릴리스의 단일 시간대 요구와 DST가 없는 한국 환경에서 모호성을 제거한다.

### Question 7 — Error Model

A) Typed validation/not-found/conflict/stale/rate-limit failures mapped to safe HTTP errors. (selected)

B) Throw generic runtime exceptions and map all failures to 500.

X) Other.

[Answer]: A

[Rationale]: client recovery와 보안상 안전한 오류 경계를 명확히 한다.

### Question 8 — Integration Points

A) No external systems; only OpenAPI HTTP and embedded persistence ports. (selected)

B) Add calendar or notification integration extension points now.

X) Other.

[Answer]: A

[Rationale]: 승인된 MVP 범위를 유지하고 사용하지 않는 추상화를 만들지 않는다.
