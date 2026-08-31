# Application Design Plan

## Plan Progress

- [x] 요구사항, 사용자 스토리와 실행계획 컨텍스트 로드
- [x] 컴포넌트 경계·계약·상태 관리 질문 작성
- [x] 기본 A 위임에 따라 모든 답변과 근거 기록
- [x] 답변 간 모호성·충돌 없음 검증
- [x] `components.md` 생성
- [x] `component-methods.md` 생성
- [x] `services.md` 생성
- [x] `component-dependency.md`와 데이터 흐름 생성
- [x] `application-design.md` 통합 문서 생성
- [x] 설계 완전성, Security Baseline과 PBT 추적성 검증

## Design Questions

### Question 1
백엔드와 프론트엔드 컴포넌트 경계를 어떻게 구성할까요?

A) 백엔드는 domain/application/adapter 의존 방향을 지키고, 프론트는 timetable/task/list 같은 사용자 기능별 모듈과 공통 app shell로 구성한다. (권장)

B) 백엔드는 controller/service/repository 기술 계층, 프론트는 components/hooks/utils 기술 종류로만 구성한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 도메인 규칙을 프레임워크에서 격리하면서 프론트 파일 구조를 사용자 흐름에 맞춰 UI 컴포넌트만 남는 설계를 방지한다.

### Question 2
프론트엔드와 백엔드 API 계약을 어떻게 관리할까요?

A) OpenAPI를 백엔드 계약 산출물로 두고 프론트 API 타입 생성·계약 검증에 사용한다. (권장)

B) 프론트와 백엔드가 요청·응답 타입을 각각 수동으로 관리한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 독립 빌드 유닛 사이의 타입 이탈과 런타임 오류를 CI에서 조기에 차단한다.

### Question 3
프론트엔드 상태 경계를 어떻게 나눌까요?

A) 서버 데이터와 mutation 상태는 TanStack Query, 모달·드래그·필터 입력 같은 일시 UI 상태는 feature-local React state로 관리한다. (권장)

B) 모든 데이터를 하나의 전역 client store에 복제한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 서버 데이터의 단일 출처를 유지하고 낙관적 업데이트·rollback은 query cache 경계에서 통제해 중복 상태를 줄인다.

## Artifact Scope

- Components: frontend/backend 주요 컴포넌트, 책임, port와 interface
- Methods: 고수준 signature와 입력·출력, 상세 비즈니스 규칙은 Functional Design으로 이관
- Services: 사용자 여정별 orchestration, transaction과 error boundary
- Dependencies: 허용 의존 방향, REST 통신과 데이터 흐름
- Consolidation: 결정, 대안, Security/PBT 적용을 하나의 검토 문서로 통합
