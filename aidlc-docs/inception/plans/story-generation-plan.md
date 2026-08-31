# User Story Generation Plan

## 1. 목적과 원칙

요구사항을 UI 컴포넌트나 기술 계층이 아니라 사용자가 달성하는 결과 중심의 스토리로
변환한다. 각 스토리는 INVEST 기준, 명시적 수용 기준, 안정 ID 추적성과 테스트 가능성을
갖는다.

## 2. 계획 진행 상태

- [x] Requirements Analysis와 디자인 레퍼런스 컨텍스트 로드
- [x] User Stories 실행 필요성 평가 및 문서화
- [x] 적용 가능한 분해 방법과 선택 질문 작성
- [x] 모든 `[Answer]` 수집
- [x] 답변의 모호성·충돌 검증
- [x] 스토리 생성 계획의 명시적 승인 및 근거 기록

## 3. 분해 방법 대안

| 접근 | 장점 | 위험 | 이 프로젝트 판단 |
|---|---|---|---|
| 사용자 여정 기반 | 생성→배치→충돌 해결→완료의 가치 흐름 보존 | 일부 도메인 규칙이 여러 스토리에 언급될 수 있음 | 권장 |
| 기능 기반 | 타임테이블, 목록, CRUD 경계가 명확 | 컴포넌트 납품으로 흐르고 E2E 가치가 끊길 수 있음 | 보조 분류로만 사용 |
| 페르소나 기반 | 사용자별 요구 차이를 드러냄 | 단일 사용자 제품이라 중복이 큼 | 비권장 |
| 도메인 기반 | task와 schedule 규칙 분리가 명확 | 사용자 경험 순서를 잃기 쉬움 | 설계 단계에서 활용 |
| Epic 기반 | 전체 범위를 계층적으로 조망 | MVP 규모에 비해 문서 계층이 과도함 | 비권장 |

## 4. 계획 질문

### Question 1
스토리를 어떤 방식으로 분해할까요?

A) 사용자 여정 기반의 세로 슬라이스로 분해하고 기능 영역 태그를 보조로 사용한다. (권장)

B) 타임테이블, 백로그, 목록, API 등 기능 영역별로 분해한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 생성부터 주간 검토까지 사용자 가치가 이어지는 세로 슬라이스가 단순 UI 컴포넌트 납품을 방지하고 E2E 수용 기준과 직접 연결된다.

### Question 2
페르소나는 어느 깊이로 작성할까요?

A) 개인 지식 근로자 1명을 핵심 페르소나로 두고 키보드·모바일 이용 상황을 컨텍스트 변형으로 포함한다. (권장)

B) 데스크톱 집중 사용자와 모바일 이동 사용자를 별도 페르소나로 만든다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: 인증 없는 단일 사용자 범위이므로 페르소나를 인위적으로 나누지 않고 실제 기기와 접근성 사용 상황의 차이를 한 페르소나 안에서 다룬다.

### Question 3
수용 기준과 테스트 추적 형식은 무엇으로 할까요?

A) Given/When/Then 시나리오와 비즈니스 규칙 체크리스트를 함께 사용하고 각 항목에 FR/NFR 및 테스트 계층을 연결한다. (권장)

B) 간결한 체크리스트 수용 기준과 FR/NFR 연결만 사용한다.

X) Other (please describe after [Answer]: tag below)

[Answer]: A

[Rationale]: Given/When/Then은 핵심 흐름을 실행 가능한 예로 만들고 규칙 체크리스트와 안정 ID 연결은 경계 조건과 테스트 누락을 방지한다.

## 5. 승인 후 생성 체크리스트

- [x] 승인된 질문 답변과 요구사항을 다시 로드한다.
- [x] `personas.md`에 핵심 페르소나, 목표, 좌절, 사용 환경과 접근성 요구를 생성한다.
- [x] `stories.md`에 사용자 여정 순서의 스토리와 Given/When/Then 수용 기준을 생성한다.
- [x] 각 스토리에 관련 FR/NFR, 페르소나, 우선순위와 필수 테스트 계층을 연결한다.
- [x] 생성된 모든 스토리를 Independent, Negotiable, Valuable, Estimable, Small, Testable 기준으로 검토한다.
- [x] 충돌 덮어쓰기, 과대 입력, API 남용 등 보안·오용 시나리오를 관련 스토리 수용 기준에 포함한다.
- [x] 범위 밖 항목이 유입되지 않았는지 검증한다.
- [x] Security Baseline 적용 상태와 PBT 부분 적용 관련 추적성을 검증한다.
- [x] `stories.md`와 `personas.md`의 Markdown 및 안정 ID 추적성을 검증한다.

## 6. 필수 산출물

- `aidlc-docs/inception/user-stories/stories.md`
- `aidlc-docs/inception/user-stories/personas.md`

## 7. 완료 조건

- 모든 질문 답변과 판단 근거가 명확하다.
- 계획이 사람의 명시적 승인을 받았다.
- 생성 체크리스트가 모두 `[x]`다.
- 모든 스토리가 INVEST와 수용 기준 검토를 통과한다.
- 페르소나, 스토리, FR/NFR와 테스트 계층의 연결이 완전하다.
