# U2 Frontend Planning Experience — 인수인계서

- **작성 시각**: 2026-08-31 22:05 (KST)
- **현재 단계**: CONSTRUCTION / U2 Code Generation Part 2 실행 중
- **진행률**: 계획서 14단계 중 **Step 1~4 완료**, Step 5부터 남음
- **차단 요소**: 없음. 설계 게이트는 모두 닫혔고 코드 생성 계획도 승인됐다.

---

## 1. 지금 무엇이 끝났나

### 승인 완료된 게이트

| 게이트                           | 상태                                           |
| -------------------------------- | ---------------------------------------------- |
| U1 전체 (백엔드)                 | 승인 완료, `origin/main`(2cd6c4a)에 반영       |
| U2 Functional Design             | 승인 완료                                      |
| U2 NFR Requirements              | 승인 완료                                      |
| U2 NFR Design                    | **이번 세션에서 승인 완료**                    |
| U2 Code Generation Part 1 (계획) | **이번 세션에서 승인 완료** (Q1/Q2/Q3 = A/A/A) |

### 이번 세션에서 확정된 결정 3건

계획서 `aidlc-docs/construction/plans/u2-frontend-planning-experience-code-generation-plan.md`의
`[Answer]` 태그에 원문이 있다.

1. **시안 충실도 = 구조 충실 + 토큰 도출**. `aidlc-inputs/design/`의 스크린샷 15장에서 레이아웃·
   계층·밀도·컴포넌트 해부를 그대로 가져오되, 팔레트는 시안에서 뽑고 **WCAG AA 대비에 실패하는
   조합만 조정**한다. 범위 밖 화면(칸반·태그·반복 등)은 비활성이 아니라 제거한다.
2. **재연결 폴링 = 5초 간격, 2분(24회) 상한**. 상한 도달 시 폴링을 멈추고 수동 재시도 컨트롤을
   남긴다. 변경 요청은 어떤 경우에도 자동 재시도하지 않는다. 간격과 상한은 주입 가능한 상수로
   두어 테스트를 결정론적으로 만든다.
3. **E2E의 U1 확보 = Playwright가 직접 기동·종료**. `webServer`가 Maven wrapper를 in-memory
   프로필로 띄우고 `/actuator/health`를 기다린 뒤 종료한다. 로컬·CI 모두 `npm run test:e2e` 한 줄.

### Step 1 — 프론트엔드 스캐폴드와 검증 게이트 (완료)

생성 위치: `frontend/` (신규)

- `package.json` — 승인된 스택을 **정확한 버전으로 고정**(캐럿 없음). React 19.2.8, Vite 8.2.2,
  Vitest 4.1.11, Playwright 1.62.1, TanStack Query 5.102.8, dnd-kit 6.3.1, MSW 2.15.0,
  fast-check 4.9.0, openapi-typescript 7.13.0, TypeScript **5.9.3**
- `tsconfig.json` — `strict` + `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`,
  `verbatimModuleSyntax`, `@/*` → `src/*` 경로 별칭
- `vite.config.ts` — 개발 서버가 **127.0.0.1에만 바인딩**(NFR-001), 소스맵 없음
- `vitest.config.ts` — jsdom, 커버리지 게이트 **차단**(statements/functions/lines 80%, branches 75%),
  생성 파일과 목 핸들러는 커버리지 제외
- `vitest.capacity.config.ts` — 1,000건 렌더 측정 전용 프로필(U1의 `-Pcapacity`와 같은 분리)
- `eslint.config.js` — 아래 "린트가 강제하는 것" 참조
- `scripts/check-bundle-budget.mjs` — 250KB gzip 상한 + **목 모듈이 번들에서 도달 가능하면 실패**
  (마커 문자열 검사 + 금지 파일명 검사 2중). 커밋 직전 실제 구멍 1건을 잡아 고쳤다: MSW postinstall이
  만든 `public/mockServiceWorker.js`를 Vite가 `dist/`로 그대로 복사해 프로덕션 산출물에 실렸는데,
  워커 파일 안에 마커 문자열이 없어 마커 검사만으로는 통과했다. `vite.config.ts`에
  `excludeMockWorkerFromBuild` 플러그인(빌드 시 `dist/mockServiceWorker.js` 제거)을 추가하고,
  게이트에 금지 파일명 검사를 넣어 플러그인이 사라져도 빌드가 실패하도록 했다. 음성 검증 완료.
- `src/app/App.tsx`, `src/main.tsx`, `index.html` — 랜드마크만 있는 최소 셸(Step 10에서 교체)
- `src/app/App.test.tsx` — 스모크 테스트. TDD 순서대로 **먼저 실패시킨 뒤** 구현했다.

검증 결과: `npm run verify` 전체 통과. 번들 61.2KB gzip / 250KB.

### Step 2 — 계약 타입, 이월 결함 수정, drift 게이트 (완료)

**이월 항목이었던 U1 계약 문서 결함을 해소했다.** `RateLimitFilter`는 429 + `RATE_LIMITED` +
`Retry-After`를 반환하는데 `planning-api.yaml`에는 셋 다 없었고, 기존 drift 테스트가 path/method
집합만 비교해 이를 놓쳤다.

- `backend/openapi/planning-api.yaml` (문서 전용 수정, U1 동작 변경 없음)
  - `components/responses/TooManyRequests` 추가 — `Retry-After` 헤더를 `required: true`로 문서화
  - `ApiError.code` enum에 `RATE_LIMITED` 추가
  - **9개 오퍼레이션 전부**에 `'429'` 응답 추가 (레이트 리미터가 라우팅 이전 필터라 전역 도달)
- `OpenApiContractDriftTest`에 검사 3건 추가 → 총 4건. 모두 통과.
  - 전 오퍼레이션의 429 문서화 여부
  - 플랫폼 필터가 내는 코드(`RATE_LIMITED` 등)의 enum 포함 여부
  - `TooManyRequests`의 `Retry-After` 헤더 문서화 여부
- **음성 검증 완료**: 429를 한 곳에서 제거하니 새 테스트가 실제로 실패했고, 계약 파일은 바이트 단위로
  복원됐다(`npm run contract:check`로 확인). 가드가 형식만 갖춘 게 아니라 실제로 회귀를 잡는다.
- `frontend/src/shared/api/generated/planning-api.d.ts` 생성·커밋 대상. 429가 9곳, 코드 유니온에
  `RATE_LIMITED` 포함 확인.
- `frontend/scripts/check-contract-drift.mjs` — 임시 디렉터리에 재생성 후 diff, 차이 나면 실패.
  `npm run verify`에 포함돼 있다.

---

## 2. 다음 사람이 바로 할 일

계획서 **Step 5**부터 순서대로 이어가면 된다. 각 단계는 test-first이며, 완료 즉시 계획서
체크박스를 `[x]`로 갱신하는 것이 규칙이다(AI-DLC 규칙: 같은 상호작용 안에서 갱신).

| 다음       | 내용                                                                                    |
| ---------- | --------------------------------------------------------------------------------------- |
| Step 3~4   | **완료** — 순수 시간·그리드 코어, fast-check PBT, seed `20260901`                       |
| Step 5~6   | MSW 핸들러 → fetch 래퍼, 오류 정규화기(`SafeApiError` 7종), 연결 모니터(5초/2분)        |
| Step 7~8   | 캐시 키·범위 무효화, 스냅샷 롤백, single-flight, S-F03 상태기계                         |
| Step 9~10  | **시안 15장을 열어 토큰 추출** → 라이트/다크 토큰, 셸, 오류 경계, live region 2개, 배너 |
| Step 11~12 | 기능 슬라이스 F-C02~F-C07                                                               |
| Step 13    | Playwright(webServer로 U1 기동) + axe 차단 + 번들·용량·npm audit·SBOM                   |
| Step 14    | `frontend/README.md` + code-summary/test-summary/traceability/security-review           |

**Step 9~10 진입 시 반드시 할 것**: `aidlc-inputs/design/`의 PNG 15장을 실제로 열어 팔레트·간격·
타이포를 추출한다. 지금까지는 이미지를 직접 열지 않고 Inception 단계의 요약(주간 타임테이블,
백로그, 드래그 배치, 충돌, 리스트, 라이트/다크)만 사용했다.

---

## 3. 반드시 알고 있어야 할 제약

### 린트가 강제하는 것 (`frontend/eslint.config.js`)

- `innerHTML`, `outerHTML`, `insertAdjacentHTML`, `dangerouslySetInnerHTML`, `eval`,
  `new Function` — **예외 없는 에러**(UR-007)
- `.tsx` 파일은 `@/shared/api/client|generated|mocks`를 **직접 import 할 수 없다**. UI는 반드시
  기능 훅을 거친다(NFR-007, U1 ArchUnit의 프론트 대응물)
- `shared/`는 `features/`나 `app/`을 import 할 수 없다
- 각 기능은 다른 기능의 내부를 import 할 수 없다(기능 5개마다 별도 규칙 블록)
- `any`와 non-null 단언(`!`)은 생성 파일 밖에서 에러
- 테스트 파일만 위 경계 규칙이 완화된다

### 절대 하면 안 되는 것

- **수기 전송 타입 작성**. `unit-of-work.md`가 U2에 대해 명시적으로 금지한다. 타입은 오직
  `npm run contract:generate`로만 만든다.
- **변경 요청 자동 재시도**. 승인된 Q4 결정 위반. 조회만 자동 재시도(최대 2회)한다.
- **낙관적 갱신 성공 시 병합**. 서버 payload로 **교체**해야 한다. 스냅샷은 낙관적 쓰기 **이전**에
  뜬다. 삭제는 낙관적 처리하지 않는다.
- **circuit breaker 도입**. NFR-008의 "오류를 숨기지 않는다"에 반해 명시적으로 거부됐다.
  연결 배너는 **전송 계층 실패만** 집계하며, 4xx/5xx는 서버의 답이므로 정상 오류 경로로 간다.
- **범위 밖 기능을 비활성 상태로 남기기**. 제거해야 한다(UR-070~072).
- **품질 게이트 임계값 낮추기**. 실패는 임계값이 아니라 코드로 해결한다.

### 알려진 기술 제약 1건

`eslint-plugin-jsx-a11y` 6.10.2가 ESLint 10 peer 범위를 선언하지 않아 **ESLint 9.39.5로 고정**했다.
npm이 "no longer supported" 경고를 내지만 `npm audit`은 취약점 0건이다. `--legacy-peer-deps`로
우회하면 SECURITY-10의 정확한 의존성 원칙이 흔들리므로 택하지 않았다. jsx-a11y가 ESLint 10을
지원하면 그때 올린다.

---

## 4. 재개 방법

```bash
cd "/Users/parkjunsung/Desktop/Web Project/ai-sample"

# 상태와 계획 확인
cat aidlc-docs/aidlc-state.md
cat aidlc-docs/construction/plans/u2-frontend-planning-experience-code-generation-plan.md

# 프론트 게이트 (통과 상태여야 정상)
cd frontend && npm run verify

# 백엔드 계약 테스트
cd ../backend && ./mvnw -Dtest=OpenApiContractDriftTest test
```

읽어야 할 승인 문서:

- `aidlc-docs/construction/u2-frontend-planning-experience/functional-design/` — 4종
- `aidlc-docs/construction/u2-frontend-planning-experience/nfr-requirements/` — 2종
- `aidlc-docs/construction/u2-frontend-planning-experience/nfr-design/` — 2종
- `aidlc-docs/audit.md` — 모든 결정의 근거(사용자 입력 원문 포함)

---

## 5. 커밋 상태

후속 상태 업데이트(2026-09-01): 아래 변경은 `c94db66`
(`feat(frontend): scaffold U2 with verification gates and close the 429 contract gap`)로 로컬
`main`에 커밋됐다.

- `frontend/` 전체 (`node_modules`·`dist`·`coverage` 제외)
- `backend/openapi/planning-api.yaml`의 429 문서화
- `backend/src/test/.../OpenApiContractDriftTest.java`의 검사 3건
- U2 설계·계획·감사·인수인계 문서

`origin/main`은 여전히 U1 승인 커밋 `2cd6c4a`에 있어 로컬 `main`이 한 커밋 앞선다.

후속 구현 업데이트(2026-09-01): Steps 3~4 순수 시간·그리드 코어와 테스트는 `24f1077`
(`feat(frontend): add pure planning time and grid core`)로 커밋됐다. 이후 AI-DLC 상태·계획·감사·
보안 리뷰 문서는 별도의 문서 커밋으로 관리한다.

후속 구현 업데이트(2026-09-01 20:52): Steps 5~6 전송 계층은 구현·검증 완료됐으며 아직 커밋하지
않았다. 생성 계약 기반 응답 검증, 요청 ID, 안전 오류 정규화, allowlist 쿼리, typed MSW와
전송 실패 전용 연결 모니터를 추가했다. `npm run verify`는 62개 테스트, 라인 93.80%, 브랜치
82.96%, 58.0KB gzip으로 통과한다. 다음 승인 계획 항목은 Step 7이다. 별도 Swagger 401 제보는
로컬 네 경로 모두 200이고 사용자가 직접 문서 표시를 확인해 보안 코드 변경 없이 종료했다.

후속 구현 업데이트(2026-09-01 21:07): Steps 7~8 캐시 정책과 mutation coordinator를 test-first로
완료했으며 아직 커밋하지 않았다. 구현 전 두 suite가 모듈 부재로 실패했고, 구현 후 집중 테스트 9개와
전체 71개가 통과한다. 라인 93.05%, 브랜치 81.27%, 번들 58.0KB다. 다음은 실제 디자인 입력을 직접
열어 비교한 뒤 Step 9 테스트와 Step 10 토큰·앱 셸을 구현한다.

후속 구현 업데이트(2026-09-01 21:20): Steps 9~10을 완료했으며 아직 커밋하지 않았다. 15개 PNG를
모두 직접 확인해 공통 시각 언어만 토큰으로 도출했고, 범위 밖 보드·태그·반복·담당자는 제거했다.
라이트/다크, 768px 반응형, 좌측 백로그+7일/1일 그리드, 주 탐색, 가용시간, 연결 배너, toast/live
region, 로딩·빈 상태·오류와 `Retry-After` 버튼을 구현했다. 전체 83개 테스트, 라인 93.29%, 브랜치
82.14%, 번들 64.3KB가 통과한다. Chromium 1440x900과 320x800 실렌더도 확인했다. 다음은 Step 11이다.

후속 구현 업데이트(2026-09-01 21:54): Steps 11~12를 test-first로 완료했으며 아직 커밋하지 않았다.
여섯 feature suite가 모듈 부재로 먼저 실패한 뒤 timetable/backlog/editor/scheduling/conflict/list를
실제 생성 계약 기반 hook과 연결했다. create/update/delete, 충돌 rollback 후 후보 수락, 완료 처리의
live MSW 통합 journey 3개가 통과한다. 이 과정에서 fetch를 모듈 로드 시 캡처하던 테스트 경계,
coordinator 렌더 수명, 주차 범위 캐시 교체, URL 변경 후 재조회 결함을 고쳤다. `npm run verify`는
98개 테스트, 라인 90.51%, 브랜치 81.85%, 함수 85.27%, 번들 84.2KB로 통과하고 U1 150개 테스트도
통과한다. 다음 승인 계획 항목은 Step 13이다.

후속 완료 업데이트(2026-09-02 20:29): Steps 13~14까지 모두 완료했다. Playwright가
실제 U1을 test 프로파일로 기동·종료하며 desktop·320px 10개 여정을 통과한다. 전체
101개 테스트, 라인 89.32%, 브랜치 82.54%, 함수 83.33%, axe serious/critical 0건,
1,000-task 120ms/300ms, gzip 85.0KB/250KB, 취약점 0건, CycloneDX 1.6 SBOM 생성이
통과한다. README와 code/test/traceability/security 증거를 완료했고, 중복 파일·수기
전송 타입·미해결 체크리스트가 없다. 현재는 U2 Code Generation 명시적 승인 대기
상태이며, 승인 전에 Build and Test로 넘어가지 않는다.

후속 Tempo Phase 1 업데이트(2026-09-03 22:58): 승인된 10단계 Code Generation 계획을 모두
구현했다. Tempo 토큰, 공용 우선순위 배지, dnd-kit 포인터/키보드 배치, 가용시간 미리보기,
200ms/10s 스켈레톤, 구조화 롤백 피드백, 목록 마감일 그룹을 반영했다. `npm run verify` 108/108,
실제 U1 desktop·320px E2E 10/10, A–Z 1/1, capacity 1/1, audit 취약점 0건, SBOM 재생성,
OpenAPI drift 없음이 확인됐다. 현재는 Tempo Phase 1 Code Generation 명시적 승인 대기 상태다.
