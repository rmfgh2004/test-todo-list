# 02. 개발 정책 — 코딩 규칙 / TDD / 커밋 / 안정 ID

## 1. 안정 ID 규칙

모든 요구사항은 `FR-XXX` 또는 `NFR-XXX` 안정 ID를 가지며 requirements → design →
tasks → code docstring/Javadoc → test 이름까지 관통한다.

- 한 번 부여한 ID는 재사용하거나 재번호를 부여하지 않는다.
- 폐기된 요구사항은 ID와 폐기 사유를 보존한다.
- 분할 시 `FR-003a`, `FR-003b`처럼 하위 ID를 사용한다.
- 코드의 공개 클래스·핵심 함수 Javadoc 또는 TSDoc 첫 줄에 관련 ID를 기록한다.
- 테스트 이름에는 `FR_003`처럼 하이픈을 밑줄로 변환해 포함한다.
- 각 단계에서 `rg 'FR-[0-9]{3}|NFR-[0-9]{3}'` 추적성을 검사한다.

## 2. TDD 및 테스트 정책

- 순서: 실패하는 테스트 → 최소 구현 → 리팩터링. 테스트 없는 기능 코드는 머지하지 않는다.
- 프론트 단위·컴포넌트: Vitest + React Testing Library.
- 프론트 E2E: Playwright로 데스크톱·모바일 핵심 사용자 여정 검증.
- 백엔드 단위: JUnit 5 + AssertJ + Mockito.
- 백엔드 통합: Spring Boot Test + MockMvc + H2 인메모리 DB.
- 속성 기반 테스트: 시간 범위, 15분 정렬, 충돌 판정에 fast-check와 jqwik 부분 적용.
- 계약 테스트: 프론트 API 타입과 백엔드 OpenAPI 응답 스키마 일치 검증.
- 커버리지: 프론트 statements/functions/lines 80%, branches 75% 이상.
- 커버리지: 백엔드 line/branch 80% 이상, 시간 충돌 도메인 branch 90% 이상.
- E2E 필수 흐름: 생성, 배치, 충돌 해결, 완료, 주 이동, 목록 전환, 모바일 조작.
- 외부 의존성은 없으며 DB 통합 테스트는 매 테스트 격리된 인메모리 H2를 사용한다.
- PBT 실패 시 seed와 축소된 입력을 보존하고 회귀 예제 테스트를 추가한다.

## 3. 코딩 규칙

- 네이밍: Java는 표준 Java 관례, TypeScript는 컴포넌트 PascalCase와 변수 camelCase.
- 구조: `frontend/`, `backend/`를 독립 빌드 단위로 두고 도메인별 내부 구조를 사용한다.
- 주석: 코드가 무엇을 하는지 반복하지 않고 제약, 의도, 안정 ID와 보안 결정을 기록한다.
- 에러 처리: 예외를 삼키지 않으며 전역 처리기에서 안전한 오류 응답으로 변환한다.
- 로깅: 구조화 로그에 timestamp, level, request ID, message를 포함하고 사용자 입력 원문,
  DB 비밀번호, 토큰, 스택 트레이스를 사용자 응답이나 일반 로그에 남기지 않는다.
- 포맷: Prettier/ESLint와 Spotless/Checkstyle을 CI에서 강제한다.
- 금지: 비밀값 하드코딩, SQL 문자열 결합, `any` 남용, 비검증 타입 단언, 비활성 코드를
  주석으로 보존, `console.log`와 `System.out`을 운영 로깅으로 사용.
- 날짜·시간: 백엔드는 `LocalDate`, `LocalTime`과 명시적 `Asia/Seoul` 정책을 사용하고,
  프론트엔드는 ISO-8601 문자열을 경계에서 파싱한다.

## 4. 코드별 보안 리뷰 규칙

- 모든 코드 작성 또는 수정 묶음 직후 `04-security-review-checklist.md`를 수행한다.
- 관련 없는 항목은 N/A와 근거를 기록한다.
- 적용 가능한 미충족 항목이 하나라도 있으면 해당 코드 단계는 완료로 처리하지 않는다.
- 리뷰 결과는 `aidlc-docs/construction/{unit}/code/security-review.md`에 append한다.
- SECURITY-01~15 적용 상태와 자체 체크리스트 결과를 함께 기록한다.

## 5. 커밋 규칙

- Conventional Commits: `<type>(<scope>): <subject>`.
- type은 `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`를 사용한다.
- 본문에 `Refs: FR-003, NFR-002` 형식으로 안정 ID를 명시한다.
- 한 커밋은 한 가지 목적만 가지며 산출물과 코드 변경은 분리한다.

## 6. 브랜치 / PR

- 브랜치: `feat/FR-XXX-short-description`.
- PR 단위: unit 또는 서로 분리할 수 없는 안정 ID 묶음.
- 리뷰 기준: 요구사항 추적성, 테스트 통과, 커버리지, 보안 체크리스트, 접근성,
  API 호환성, 사용자 입력과 기존 변경 보존.
- PR 설명: 관련 안정 ID, 산출물 경로, 테스트·커버리지·보안 검사 결과.

## 7. AI 협업 규칙

- 모호한 요구사항은 구현 전에 다지선다 질문 파일로 확인한다.
- 각 phase는 사람의 명시적 승인과 판단 근거를 받은 뒤 진행한다.
- 모든 상호작용은 사용자 원문 그대로 `aidlc-docs/audit.md`에 append한다.
- 계획 단계 완료 즉시 해당 체크박스를 같은 상호작용에서 갱신한다.

