# Unit of Work Plan

## Planning Progress

- [x] Requirements, Stories, Application Design과 Execution Plan 로드
- [x] 필수 6개 분해 범주의 질문 작성
- [x] 사용자 기본 A 위임으로 답변·근거 기록
- [x] 답변의 모호성·충돌 없음 검증
- [x] Unit of Work 계획 승인 및 근거 기록

## Decomposition Questions

### Question 1 — Story Grouping
스토리를 개발 유닛에 어떻게 배치할까요?

A) frontend/backend 실행 경계로 두 유닛을 만들고, 모든 사용자 스토리는 양쪽 유닛에 primary/supporting 책임을 명시해 세로 슬라이스를 유지한다. (권장)

B) 스토리를 서로 겹치지 않게 한 유닛에만 독점 배정한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 독립 빌드 경계를 유지하면서도 UI와 API가 분리된 납품물이 되지 않게 한다.

### Question 2 — Dependencies
두 유닛의 계약과 통합 의존성을 어떻게 관리할까요?

A) backend 유닛이 OpenAPI를 소유하고 frontend는 생성 타입을 소비하며, contract test와 실제 E2E를 통합 게이트로 둔다. (권장)

B) 양쪽이 타입을 수동 관리하고 최종 E2E에서만 통합한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 계약 이탈을 구현 초기에 발견하고 독립 테스트와 실제 통합을 모두 유지한다.

### Question 3 — Team Alignment
현재 한 명의 AI 개발 에이전트 기준 작업 순서를 어떻게 할까요?

A) backend domain과 계약을 먼저 확정한 뒤 frontend mock 연동을 시작하고, 세로 슬라이스마다 실제 backend 통합으로 닫는다. (권장)

B) frontend 전체를 먼저 완성한 뒤 backend를 한 번에 연결한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 재작업을 줄이고 각 사용자 가치가 실제 저장까지 동작하는 시점을 앞당긴다.

### Question 4 — Technical and Deployment Boundary
배포와 확장 관점의 유닛 경계는 어떻게 둘까요?

A) frontend와 backend를 독립 빌드 유닛으로 유지하되 첫 릴리스는 하나의 로컬 애플리케이션으로 실행한다. (권장)

B) 처음부터 각각 독립 배포 서비스로 운영한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 로컬 실행 단순성을 유지하면서 빌드·테스트 책임은 명확히 분리한다.

### Question 5 — Business Domain Boundary
backend 내부 업무 경계는 어떻게 둘까요?

A) task aggregate와 schedule policy를 같은 Planning Core 유닛 안의 독립 domain module로 두고 하나의 transaction에서 조정한다. (권장)

B) task와 schedule을 별도 서비스와 DB로 분리한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 충돌 판정과 저장에 필요한 강한 일관성을 단순하게 보장한다.

### Question 6 — Greenfield Code Organization
workspace 코드 디렉터리는 어떻게 구성할까요?

A) root 아래 `backend/` Maven project와 `frontend/` npm/Vite project, 통합 `scripts/`와 root CI 설정을 둔다. (권장)

B) 한 빌드 도구 아래 frontend와 backend를 중첩한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 양쪽 빌드와 테스트를 독립적으로 실행하고 root에서 통합 검증할 수 있다.

## Approved Generation Checklist

- [x] 계획과 Application Design을 다시 로드한다.
- [x] `unit-of-work.md`에 두 유닛의 책임, code organization과 완료 조건을 생성한다.
- [x] `unit-of-work-dependency.md`에 dependency matrix, 계약과 순서를 생성한다.
- [x] `unit-of-work-story-map.md`에 US-001~US-010과 FR/NFR의 primary/supporting 책임을 매핑한다.
- [x] 모든 스토리와 FR/NFR이 최소 한 유닛에 할당되었는지 검증한다.
- [x] 순환 의존성, 공유 DB 직접 접근과 범위 중복이 없는지 검증한다.
- [x] Security Baseline과 PBT partial 책임을 유닛에 할당한다.
- [x] Markdown, 안정 ID와 계획 체크박스 완전성을 검증한다.

## Mandatory Artifacts

- `aidlc-docs/inception/application-design/unit-of-work.md`
- `aidlc-docs/inception/application-design/unit-of-work-dependency.md`
- `aidlc-docs/inception/application-design/unit-of-work-story-map.md`

## Plan Approval Gate

Unit of work plan complete. Review this plan before generation. The recommended A rationale
is that two runtime/build units preserve clear ownership while OpenAPI and vertical-slice tests
prevent frontend/backend fragmentation.
