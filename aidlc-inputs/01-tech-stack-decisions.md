# 01. 기술 스택 결정 — 팀 전원 통일

## 1. 언어 / 런타임

| 영역 | 선택 | 버전 고정 | 결정 근거 |
|---|---|---|---|
| 프론트엔드 | TypeScript, React | TypeScript 5.x, React 19.2.x | 정적 타입과 최신 안정 React 사용 |
| 백엔드 | Java, Spring Boot | Java 17, Spring Boot 4.1.1 | 로컬 JDK와 안정 Spring Boot 지원선의 교집합 |
| IaC | 사용하지 않음 | N/A | 로컬 MVP이며 클라우드 배포가 범위 밖임 |

## 2. 프론트엔드

- 프레임워크: React 19.2.x + Vite 8.2.x
- 상태 관리: TanStack Query로 서버 상태 관리, React 지역 상태로 UI 상태 관리
- 스타일링: CSS Modules와 전역 디자인 토큰, Lucide React 아이콘
- 드래그 앤 드롭: dnd-kit
- 빌드/패키지 매니저: Node.js 24 LTS, npm 11, `package-lock.json`으로 정확한 버전 고정

## 3. 백엔드

- 실행 형태: 내장 서버 기반 로컬 상시 프로세스
- API 스타일: JSON REST API, `/api/v1` 버전 경로
- 인증·인가: 인증 없음. 모든 엔드포인트는 로컬 단일 사용자용 공개 경로로 명시
- 데이터 저장소: H2 2.4.240, 개발은 암호화된 파일 모드, 테스트는 인메모리 모드
- 스키마·마이그레이션 관리: Flyway SQL 마이그레이션
- 데이터 접근: Spring Data JPA
- 입력 검증: Jakarta Bean Validation과 도메인 검증

## 4. 인프라 서비스

| 용도 | 서비스 | 환경 | 비고 |
|---|---|---|---|
| 컴퓨트 | 로컬 JVM | 개발/데모 | loopback 인터페이스 바인딩 |
| 정적 프론트엔드 | Vite 개발 서버 및 빌드 산출물 | 개발/데모 | 외부 CDN 사용 없음 |
| 데이터베이스 | H2 embedded | 개발/테스트 | 파일/인메모리 프로필 분리 |
| 인증 | 사용하지 않음 | N/A | 단일 사용자 범위 |
| CI/CD | GitHub Actions | 검증 | 빌드, 테스트, 감사, SBOM |
| 관측 | 구조화 콘솔 로그 | 로컬 | 민감정보 기록 금지 |

## 5. AI / 모델

- 사용 모델: 사용하지 않음
- 호출 방식: N/A
- 프롬프트·컨텍스트 관리: N/A
- 비용 상한과 폴백 전략: N/A
- 평가 방법: N/A

## 6. 개발 도구

- AI 코딩 도구: ChatGPT Codex, 공통 AI-DLC 규칙 적용
- 프론트 테스트: Vitest 4.1.x, React Testing Library, Playwright 1.62.x, fast-check
- 백엔드 테스트: JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc, jqwik
- 린터·포매터: ESLint, Prettier, Maven Checkstyle, Spotless
- 보안 검사: npm audit, OWASP Dependency-Check, 보안 리뷰 체크리스트
- 로컬 개발 환경: macOS, Node.js 24 LTS 기준, Java 17

## 7. 대안과 기각 사유

| 항목 | 검토한 대안 | 기각 사유 |
|---|---|---|
| 사용자 범위 | 로그인 또는 팀 협업 | 첫 릴리스의 핵심 경험과 풀스택 품질을 약화함 |
| 백엔드 | Jakarta REST + JDBC | 검증·트랜잭션·테스트를 직접 조립할 비용이 큼 |
| 데이터베이스 | PostgreSQL | 사용자 지정 H2와 로컬 무료 실행 제약에 맞지 않음 |
| 상태 관리 | 전역 상태 라이브러리 | 서버 상태 외 복잡한 전역 상태가 없어 불필요함 |
| 보드 보기 | 칸반 보드 포함 | 첫 릴리스 핵심 여정이 아니며 범위 확장을 유발함 |

