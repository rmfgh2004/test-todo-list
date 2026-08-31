# User Stories

## 구성 원칙

- 모든 스토리는 P-001의 사용자 가치가 프론트엔드, API와 데이터 저장까지 이어지는 세로 슬라이스다.
- 우선순위는 Must로 통일하되 구현 순서는 사용자 여정과 의존성을 따른다.
- Given/When/Then은 E2E 수용 기준이며 규칙 체크리스트는 단위·통합 테스트 경계를 보완한다.

## US-001 이번 주 계획 확인

**As a** P-001 개인 지식 근로자  
**I want** 이번 주의 배치된 일정과 미배치 할 일을 한 화면에서 확인하고 주를 이동하고 싶다.  
**So that** 실제 사용 가능한 시간과 아직 계획하지 않은 업무를 함께 판단할 수 있다.

**Priority**: Must  
**Requirements**: FR-001, FR-002, FR-005, NFR-004, NFR-005

### Acceptance Scenarios

**Scenario: 주간 계획 표시**

- Given 월요일부터 일요일 사이에 배치 일정과 미배치 할 일이 있다.
- When 사용자가 주간 화면을 연다.
- Then 08:00~22:00 타임그리드에 일정이 정확한 위치와 길이로 표시된다.
- And 백로그에는 미완료·미배치 할 일만 표시된다.

**Scenario: 주 이동과 복원**

- Given 사용자가 현재 주간 화면에 있다.
- When 다음 주로 이동하고 페이지를 새로고침한다.
- Then URL 기준 날짜와 해당 주 데이터가 동일하게 복원된다.

### Business Rules

- 월요일 시작, Asia/Seoul, 15분 간격을 사용한다.
- 오늘, 주말과 우선순위는 색상 외 텍스트 또는 아이콘으로 구분한다.
- 로딩, 전체 빈 상태, 필터 빈 상태와 오류를 서로 다르게 표시한다.

### Required Tests

- Frontend component: 주간 위치 계산, 백로그 상태, 로딩·빈·오류 상태
- Backend integration: 기간 필터와 미배치 조건
- Playwright desktop/mobile: 주 이동, URL 복원과 초기 화면

## US-002 할 일 빠르게 기록

**As a** P-001 개인 지식 근로자  
**I want** 제목과 예상 시간 중심으로 새 할 일을 빠르게 만들고 싶다.  
**So that** 계획 전에 해야 할 일을 잊지 않고 백로그에 모을 수 있다.

**Priority**: Must  
**Requirements**: FR-003, FR-005, NFR-001, NFR-003

### Acceptance Scenarios

**Scenario: 유효한 할 일 생성**

- Given 사용자가 새 할 일 폼을 열었다.
- When 제목, 우선순위, 15분 단위 예상 시간을 입력해 제출한다.
- Then 서버에 할 일이 저장되고 백로그와 목록에 즉시 나타난다.
- And 성공 상태가 보조 기술에도 전달된다.

**Scenario: 잘못된 입력 거부**

- Given 제목이 비었거나 너무 길고 예상 시간이 허용 범위를 벗어난다.
- When 사용자가 제출한다.
- Then 프론트와 서버가 모두 저장을 거부하고 필드별 안전한 오류를 표시한다.

### Business Rules

- 제목 1~120자, 설명 최대 2,000자, 예상 시간 15~840분이다.
- 우선순위는 LOW, MEDIUM, HIGH만 허용한다.
- HTML과 script 입력을 실행하거나 raw HTML로 렌더링하지 않는다.

### Required Tests

- Backend unit/API: 경계값, enum allowlist, 과대 payload, 안전한 오류
- Frontend component: 폼 검증, 중복 제출 방지, 성공·오류 피드백
- Playwright: 생성 후 백로그 반영

## US-003 할 일 내용 유지보수

**As a** P-001 개인 지식 근로자  
**I want** 할 일의 상세를 수정하거나 더 이상 필요 없는 항목을 삭제하고 싶다.  
**So that** 계획이 현재 해야 할 일을 정확히 반영한다.

**Priority**: Must  
**Requirements**: FR-004, FR-013, NFR-003, NFR-006

### Acceptance Scenarios

**Scenario: 할 일 수정**

- Given 기존 할 일이 백로그 또는 타임테이블에 있다.
- When 사용자가 제목, 우선순위, 예상 시간 또는 마감일을 수정한다.
- Then 모든 보기와 영속 데이터에 변경이 일관되게 반영된다.

**Scenario: 확인 후 삭제**

- Given 사용자가 할 일 상세에서 삭제를 선택한다.
- When 삭제 확인을 완료한다.
- Then 본 데이터는 제거되고 변경 감사 기록은 남는다.

### Business Rules

- 수정에도 US-002 입력 검증을 동일하게 적용한다.
- 존재하지 않는 ID는 안전한 404를 반환한다.
- 삭제 실패 시 UI는 기존 항목을 복원하고 request ID를 표시한다.

### Required Tests

- Backend unit/integration: 트랜잭션, 404, 감사 레코드와 롤백
- Frontend component: 확인 모달, 낙관적 상태 복원
- Playwright: 수정과 삭제 전체 흐름

## US-004 할 일을 실제 시간에 배치

**As a** P-001 개인 지식 근로자  
**I want** 백로그 할 일을 주간 타임테이블에 드래그하거나 날짜·시간을 입력해 배치하고 싶다.  
**So that** 해야 할 일 목록을 실행 가능한 일정으로 바꿀 수 있다.

**Priority**: Must  
**Requirements**: FR-006, NFR-002, NFR-004, NFR-006

### Acceptance Scenarios

**Scenario: 드래그 배치**

- Given 미배치 할 일이 있고 목표 시간이 비어 있다.
- When 사용자가 카드를 원하는 날짜와 시간 슬롯에 놓는다.
- Then 시작 시각과 예상 시간으로 종료 시각을 계산해 저장하고 일정 블록을 표시한다.

**Scenario: 키보드 대체 배치**

- Given 사용자가 드래그를 사용할 수 없다.
- When 날짜와 시작 시간을 폼으로 선택해 제출한다.
- Then 드래그와 동일한 서버 규칙과 결과로 배치된다.

### Business Rules

- 시작 시각과 예상 시간은 15분 경계를 유지한다.
- 종료 시각은 시작보다 늦고 같은 날짜의 표시 범위를 벗어나지 않는다.
- 서버가 클라이언트 계산을 신뢰하지 않고 다시 검증한다.

### Required Tests

- Java jqwik/TypeScript fast-check: parse/format round-trip, 종료 시각과 15분 불변식
- Backend integration: 배치 저장과 잘못된 시간 거부
- Frontend interaction/Playwright: drag, 키보드 대체, 실패 rollback

## US-005 충돌을 이해하고 해결

**As a** P-001 개인 지식 근로자  
**I want** 새 일정이 기존 일정과 겹칠 때 두 일정을 비교하고 해결 방법을 직접 선택하고 싶다.  
**So that** 시스템이 모르게 내 계획을 덮어쓰거나 이동하지 않는다.

**Priority**: Must  
**Requirements**: FR-007, FR-013, NFR-002, NFR-003, NFR-006

### Acceptance Scenarios

**Scenario: 충돌 전 저장 중단**

- Given 기존 일정과 겹치는 새 배치가 제안되었다.
- When 서버가 충돌을 판정한다.
- Then 409 응답과 기존·제안 일정의 안전한 비교 정보를 반환한다.
- And 사용자의 선택 전에는 어느 일정도 변경하지 않는다.

**Scenario: 다음 빈 슬롯으로 이동**

- Given 충돌 화면이 열려 있다.
- When 사용자가 새 일정을 다음 빈 15분 슬롯으로 이동하도록 선택한다.
- Then 서버가 후보를 다시 검증하고 충돌이 없을 때 하나의 트랜잭션으로 저장한다.

**Scenario: 취소 또는 기존 일정 유지**

- Given 충돌 화면이 열려 있다.
- When 사용자가 취소하거나 기존 일정 유지를 선택한다.
- Then 기존 일정과 데이터가 변경되지 않는다.

### Business Rules

- 반개구간 `[start, end)` 기준으로 맞닿는 경계는 충돌이 아니다.
- 충돌 관계는 대칭이며 동일 할 일의 현재 배치는 자기 자신과 충돌하지 않는다.
- 반복 요청이나 경합에서도 최종 서버 판정 없이 덮어쓰지 않는다.

### Required Tests

- Java jqwik/fast-check: 충돌 대칭성, 경계 비충돌, 유효 후보 불변식
- Backend unit/integration: 409 계약, 트랜잭션 경합과 감사 기록
- Frontend component/Playwright: 세 해결 선택과 초점 관리

## US-006 계획에서 할 일을 다시 빼기

**As a** P-001 개인 지식 근로자  
**I want** 배치된 할 일을 내용 손실 없이 미배치 상태로 되돌리고 싶다.  
**So that** 아직 시간을 정하지 못한 업무를 다시 계획할 수 있다.

**Priority**: Must  
**Requirements**: FR-008, FR-013, NFR-006

### Acceptance Scenarios

**Scenario: 배치 해제**

- Given 배치된 미완료 할 일이 있다.
- When 사용자가 배치 해제를 확인한다.
- Then 시간 정보만 제거되고 할 일이 백로그에 다시 나타난다.
- And 제목, 설명, 우선순위, 예상 시간과 마감일은 유지된다.

### Business Rules

- 배치 해제는 하나의 트랜잭션과 감사 레코드로 처리한다.
- 이미 미배치인 항목에 반복 호출해도 최종 상태가 동일하다.

### Required Tests

- Backend unit/integration: 필드 보존과 idempotent 상태
- Frontend component/Playwright: 타임테이블 제거와 백로그 복귀

## US-007 실행 완료 기록

**As a** P-001 개인 지식 근로자  
**I want** 할 일을 완료하거나 실수한 완료를 취소하고 싶다.  
**So that** 주간 진행 상황과 남은 업무를 정확히 볼 수 있다.

**Priority**: Must  
**Requirements**: FR-009, FR-013, NFR-006

### Acceptance Scenarios

**Scenario: 완료 처리**

- Given 미완료 할 일이 목록 또는 타임테이블에 있다.
- When 사용자가 완료한다.
- Then 모든 보기에서 완료 상태가 일치하고 기본 백로그에서 제외된다.

**Scenario: 완료 취소와 반복 요청**

- Given 완료된 할 일이 있다.
- When 완료 취소를 요청하거나 같은 상태 요청을 반복한다.
- Then 요청한 최종 상태가 유지되고 중복 부작용이 없다.

### Business Rules

- 상태 변경마다 감사 레코드를 추가한다.
- 완료 항목은 삭제되지 않으며 목록 필터로 조회할 수 있다.

### Required Tests

- Backend unit/integration: 상태 전이, idempotency와 감사
- Frontend component/Playwright: 보기 간 상태 동기화와 실패 복원

## US-008 목록으로 누락 점검

**As a** P-001 개인 지식 근로자  
**I want** 모든 할 일을 목록으로 보고 상태·배치·우선순위에 따라 필터링하고 싶다.  
**So that** 타임테이블에서 놓친 업무와 완료 내역을 빠르게 점검할 수 있다.

**Priority**: Must  
**Requirements**: FR-010, NFR-004, NFR-005

### Acceptance Scenarios

**Scenario: 필터와 URL 복원**

- Given 서로 다른 상태와 우선순위의 할 일이 있다.
- When 사용자가 미완료·미배치·HIGH 필터와 정렬을 선택하고 새로고침한다.
- Then 동일한 필터 결과와 정렬이 URL query에서 복원된다.

### Business Rules

- 페이지 크기에 상한을 두고 허용된 정렬 필드만 받는다.
- 전체 빈 상태와 필터 결과 없음 상태를 구분한다.
- 행은 제목, 우선순위, 예상 시간, 마감일, 배치 시간과 상태를 제공한다.

### Required Tests

- Backend API: 필터 조합, pagination 상한과 정렬 allowlist
- Frontend component/Playwright: 필터, URL 복원과 빈 상태

## US-009 어느 기기와 입력 방식에서도 계획

**As a** P-001 개인 지식 근로자  
**I want** 모바일과 키보드만으로도 핵심 계획 흐름을 완료하고 싶다.  
**So that** 화면 크기나 입력 방식 때문에 계획을 포기하지 않는다.

**Priority**: Must  
**Requirements**: FR-011, NFR-004

### Acceptance Scenarios

**Scenario: 모바일 핵심 여정**

- Given 화면 너비가 320px 이상인 모바일 기기다.
- When 사용자가 날짜를 선택하고 백로그에서 할 일을 생성·배치·완료한다.
- Then 텍스트 겹침이나 가로 페이지 스크롤 없이 흐름을 완료한다.

**Scenario: 키보드와 초점**

- Given 사용자가 포인팅 장치를 사용하지 않는다.
- When Tab, Enter, Space와 Escape로 생성·배치·충돌 해결을 수행한다.
- Then 초점이 논리적으로 이동하고 모달 종료 후 시작 요소로 복원된다.

### Business Rules

- 모바일은 축소된 주간 표 대신 선택 일 타임라인과 별도 백로그를 제공한다.
- 상태와 오류는 `aria-live`로 알리고 색상만으로 의미를 전달하지 않는다.

### Required Tests

- Frontend accessibility: 역할, 이름, 초점, keyboard interaction
- Playwright mobile/desktop: 320px 레이아웃과 전체 키보드 여정

## US-010 재시작 후에도 계획 신뢰

**As a** P-001 개인 지식 근로자  
**I want** 애플리케이션을 다시 실행해도 내 계획이 안전하게 복원되고 오류 원인을 추적하고 싶다.  
**So that** 로컬 도구를 지속적으로 신뢰하고 사용할 수 있다.

**Priority**: Must  
**Requirements**: FR-012, FR-013, NFR-003, NFR-007, NFR-008

### Acceptance Scenarios

**Scenario: 암호화된 파일 복원**

- Given 암호화 키와 파일 DB 경로가 외부 설정으로 제공되었다.
- When 할 일을 저장하고 애플리케이션을 재시작한다.
- Then 동일한 계획이 복원되고 DB 키나 파일은 Git에 포함되지 않는다.

**Scenario: 안전한 오류 추적**

- Given DB 또는 API 처리 중 오류가 발생한다.
- When 사용자에게 오류를 표시한다.
- Then 내부 스택·SQL·경로 없이 일반 메시지와 request ID만 제공한다.
- And 구조화 로그에서 같은 request ID로 원인을 찾을 수 있다.

### Business Rules

- H2 콘솔과 기본 credential을 활성화하지 않는다.
- 감사 레코드는 API로 수정·삭제할 수 없다.
- 보안 헤더, loopback origin allowlist와 rate limit을 적용한다.
- 의존성 버전, 취약점 검사와 SBOM을 CI에서 검증한다.

### Required Tests

- Backend integration/security: encrypted profile config, safe error, headers, CORS, rate limit, audit immutability
- CI: dependency scan, lock/version integrity, SBOM
- Playwright: 오류 피드백과 request ID 표시

## Story Traceability Matrix

| Story | Persona | Functional | Non-Functional | Primary User Value |
|---|---|---|---|---|
| US-001 | P-001 | FR-001, FR-002, FR-005 | NFR-004, NFR-005 | 주간 계획 파악 |
| US-002 | P-001 | FR-003 | NFR-001, NFR-003 | 빠른 업무 포착 |
| US-003 | P-001 | FR-004, FR-013 | NFR-003, NFR-006 | 계획 정확성 유지 |
| US-004 | P-001 | FR-006 | NFR-002, NFR-004, NFR-006 | 목록을 실행 시간으로 전환 |
| US-005 | P-001 | FR-007, FR-013 | NFR-002, NFR-003, NFR-006 | 데이터 손실 없는 충돌 해결 |
| US-006 | P-001 | FR-008, FR-013 | NFR-006 | 유연한 재계획 |
| US-007 | P-001 | FR-009, FR-013 | NFR-006 | 진행 상태 확인 |
| US-008 | P-001 | FR-010 | NFR-004, NFR-005 | 누락 업무 점검 |
| US-009 | P-001 | FR-011 | NFR-004 | 기기·입력 방식 독립성 |
| US-010 | P-001 | FR-012, FR-013 | NFR-003, NFR-007, NFR-008 | 지속 가능한 신뢰 |

## INVEST Review

| Story | Independent | Negotiable | Valuable | Estimable | Small | Testable |
|---|---|---|---|---|---|---|
| US-001 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-002 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-003 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-004 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-005 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-006 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-007 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-008 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-009 | Yes | Yes | Yes | Yes | Yes | Yes |
| US-010 | Yes | Yes | Yes | Yes | Yes | Yes |

## Extension Compliance

### Security Baseline

| Rules | Status | Story Evidence |
|---|---|---|
| SECURITY-01, SECURITY-03~05 | Compliant | US-002 입력 보호와 US-010 암호화·로그·보안 헤더 |
| SECURITY-07~11 | Compliant | US-005 서버 재검증, US-008 pagination 상한, US-010 loopback·rate limit·hardening·공급망 |
| SECURITY-13, SECURITY-15 | Compliant | US-003/005/010 감사 무결성, 트랜잭션 rollback과 안전한 오류 |
| SECURITY-02, SECURITY-06 | N/A | 네트워크 intermediary와 IAM이 범위에 없음 |
| SECURITY-12 | N/A | 인증과 사용자 credential이 범위에 없음. DB 키는 US-010에서 보호 |
| SECURITY-14 | N/A | 클라우드 보안 경보가 범위에 없음. request ID 구조화 로그는 US-010 적용 |

**Blocking findings**: 없음.

### Property-Based Testing Partial Mode

| Rules | Status | Story Evidence |
|---|---|---|
| PBT-02, PBT-03 | Compliant | US-004 round-trip·시간 불변식, US-005 충돌 대칭성·경계 불변식 |
| PBT-07 | Compliant | 도메인 유효 범위를 생성하는 jqwik·fast-check 요구 |
| PBT-08 | Compliant | NFR-002 seed 재현과 shrinking 요구를 모든 관련 스토리가 참조 |
| PBT-09 | Compliant | jqwik와 fast-check 프레임워크 선택 |

**Blocking findings**: 없음.
