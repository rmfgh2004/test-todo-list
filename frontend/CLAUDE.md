# Timetable Todo Frontend — Agent and SDLC Pod Contract

이 파일은 frontend 저장소가 단독으로 분리된 뒤에도 Claude Code와 SDLC runner가 환경을 추측하지
않도록 하는 실행 계약이다. 버전의 최종 진실 원천은 `package.json`과 `package-lock.json`이다.

## 프로젝트 성격과 Backend 관계

- React/Vite 기반 주간 타임테이블 UI
- U1 Backend가 검증, 충돌, 버전과 저장의 유일한 권위 원천
- Backend OpenAPI로 TypeScript wire type을 생성하며 수기 DTO를 금지
- frontend 자체 빌드와 단위 테스트는 Node만 필요
- contract check와 실제 U1 Playwright는 **backend checkout도 필요**

저장소를 분리한 뒤에도 통합 gate를 그대로 실행하려면 다음 sibling layout을 유지한다. 현재 npm
script와 Playwright 설정은 `../backend`를 참조한다.

```text
/workspace/
├── backend/   # backend repository checkout
└── frontend/  # frontend repository checkout; npm commands run here
```

frontend 저장소만 checkout하면 `npm test`, `npm run build`, `npm run lint`는 가능하지만,
`npm run verify`의 `contract:check`, `contract:generate`, `test:e2e`, `test:e2e:journey`는 sibling
backend가 없으면 실패한다.

## 필수 런타임과 설치 도구

| 실행 범위               | 필요한 도구                                                                |
| ----------------------- | -------------------------------------------------------------------------- |
| build/unit/component    | Node.js 20.19+, npm, Bash, Git, CA certificates                            |
| contract check/generate | 위 도구 + `../backend/openapi/planning-api.yaml`                           |
| real U1 Playwright      | 위 도구 + JDK 17, curl 또는 wget, unzip, Chromium, Linux browser libraries |

Node `20.19.x`를 재현 가능한 baseline으로 권장한다. 검증된 로컬 환경은 Node 26.8.1/npm 11.19.0,
Java 17.0.18이다. lockfile은 npm lockfile v3이므로 clean Pod에서는 반드시 `npm ci`를 사용한다.
Cold start에는 npm registry와 Maven Central 접근, 또는 각각의 사전 캐시가 필요하다.

Build/unit Pod는 Node 20.19.x Linux image를 조직 manifest에서 digest로 고정한다. Full E2E Pod는
그 image에 JDK 17을 더하고 local Playwright CLI로 Chromium/OS dependency를 설치한다. Vite,
TypeScript, Vitest와 Playwright를 global npm package로 설치하지 않는다.

## Frontend 기술 스택

### Production dependencies

| 패키지                  | 버전    |
| ----------------------- | ------- |
| `react`                 | 19.2.8  |
| `react-dom`             | 19.2.8  |
| `@tanstack/react-query` | 5.102.8 |
| `@dnd-kit/core`         | 6.3.1   |
| `@dnd-kit/utilities`    | 3.2.2   |
| `lucide-react`          | 1.38.0  |

### Build, test and quality dependencies

| 패키지                        | 버전    |
| ----------------------------- | ------- |
| `@axe-core/playwright`        | 4.13.0  |
| `@cyclonedx/cyclonedx-npm`    | 6.0.1   |
| `@eslint/js`                  | 9.39.5  |
| `@playwright/test`            | 1.62.1  |
| `@testing-library/jest-dom`   | 7.0.1   |
| `@testing-library/react`      | 16.3.3  |
| `@testing-library/user-event` | 14.6.6  |
| `@types/node`                 | 26.4.0  |
| `@types/react`                | 19.2.18 |
| `@types/react-dom`            | 19.2.5  |
| `@vitejs/plugin-react`        | 6.1.1   |
| `@vitest/coverage-v8`         | 4.1.11  |
| `axe-core`                    | 4.13.0  |
| `eslint`                      | 9.39.5  |
| `eslint-plugin-jsx-a11y`      | 6.10.2  |
| `eslint-plugin-react-hooks`   | 7.1.1   |
| `fast-check`                  | 4.9.0   |
| `globals`                     | 17.11.0 |
| `jsdom`                       | 30.0.1  |
| `msw`                         | 2.15.0  |
| `openapi-typescript`          | 7.13.0  |
| `prettier`                    | 3.9.6   |
| `typescript`                  | 5.9.3   |
| `typescript-eslint`           | 8.68.0  |
| `vite`                        | 8.2.2   |
| `vitest`                      | 4.1.11  |

TypeScript는 strict, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, ES2022, ESM으로
컴파일한다. 모든 직접 버전은 exact pin이고 전이 트리는 `package-lock.json`으로 고정된다.

## 새 Pod 부트스트랩

### Build/unit runner

```bash
set -eu
node --version
npm --version
npm ci
npm run typecheck
npm test
npm run build
```

### Full SDLC/E2E runner

같은 컨테이너에 Node 20.19+, JDK 17과 Chromium을 설치하고 backend를 sibling으로 checkout한다.
Debian/Ubuntu-compatible runner에서 OS package 설치 권한이 있을 때:

```bash
set -eu
node --version
java -version
npm ci
./node_modules/.bin/playwright install --with-deps chromium
npm run verify
npm run test:capacity
npm run test:e2e
npm run test:e2e:journey
```

조직이 사전 제작한 browser image를 쓰면 image 안의 Playwright browser revision이 package의
Playwright 1.62.1과 일치해야 한다. `playwright install --with-deps chromium`은 root/apt 권한이
필요할 수 있으므로 image build 단계에서 실행하는 것이 안전하다.

## 실행 방법

### 실제 Backend 사용

```bash
# sibling backend 터미널/컨테이너
cd ../backend
./mvnw spring-boot:run

# frontend 터미널/컨테이너
cd ../frontend
npm ci
npm run dev
```

- Frontend dev: `http://127.0.0.1:5173`
- Backend default: `http://127.0.0.1:8080`
- Production preview: `npm run build && npm run preview` → `http://127.0.0.1:4173`
- 종료: 각 프로세스에 `SIGTERM` 또는 `Ctrl-C`

### 개발 전용 MSW

```bash
VITE_USE_MOCK=1 npm run dev
```

MSW transport는 dev에서만 허용되며 production bundle에 들어가면 build gate가 실패한다.

## 환경변수와 네트워크

| 변수                       | 기본값                  | 적용 시점/용도                                      |
| -------------------------- | ----------------------- | --------------------------------------------------- |
| `VITE_API_BASE_URL`        | `http://127.0.0.1:8080` | Vite 시작/빌드 시 browser API base URL 결정         |
| `VITE_USE_MOCK`            | 미설정                  | `1`일 때 dev-only MSW 활성화                        |
| `PLAYWRIGHT_BROWSERS_PATH` | Playwright 기본 cache   | browser 설치/조회 경로를 PVC/cache로 지정할 때 사용 |

Vite dev server는 기본 `127.0.0.1:5173`, preview는 `127.0.0.1:4173`에 strict-port로 bind한다.
Playwright는 실제 U1을 `127.0.0.1:18080`, U2를 `127.0.0.1:5180`에 자동 기동하고 종료한다.
따라서 E2E 실행 전에 이 두 포트를 점유하지 않는다.

가장 단순한 SDLC 구성은 backend/frontend/test process가 한 Pod 네트워크를 공유하고 외부 확인 시
5173과 8080을 함께 port-forward하는 방식이다. 서로 다른 Pod를 사용하면:

```bash
VITE_API_BASE_URL=http://backend.example.test npm run dev -- --host 0.0.0.0
```

이 URL은 frontend Pod뿐 아니라 실제 browser에서도 resolve/접근 가능해야 한다. Backend는
`0.0.0.0` bind와 정확한 frontend Origin CORS 설정이 필요하다. 인증 없는 local-only 도구이므로
격리된 테스트 namespace 밖으로 공개하지 않는다.

## npm 명령 전체 목록

```bash
npm run dev                 # Vite dev, 127.0.0.1:5173
npm run build               # typecheck + Vite build + 250KB gzip gate
npm run preview             # built dist, 127.0.0.1:4173
npm run typecheck           # tsc --noEmit
npm run lint                # ESLint
npm run format:check        # Prettier check
npm run format              # Prettier write
npm test                    # Vitest once
npm run test:watch          # Vitest watch
npm run test:coverage       # Vitest + blocking coverage
npm run test:capacity       # 1,000-task/300ms fixture
npm run test:e2e            # real U1, desktop + 320px, 10 cases
npm run test:e2e:journey    # captioned A–Z + WebM
npm run contract:generate   # generated TS contract; approved U1 change only
npm run contract:check      # compare against sibling backend OpenAPI
npm run audit:deps          # fail for high npm advisories
npm run sbom                # CycloneDX JSON to sbom.json
npm run verify              # type/lint/format/contract/coverage/build aggregate
```

Coverage gates: statements/functions/lines 80%, branches 75%. Production bundle gate: 250KB gzip.
E2E stores videos under `test-results/`; failures retain trace and screenshot. A–Z evidence is under
`test-results/a-z-journey/`.

## Pod writable 경로, 캐시, artifact

| 경로                         | 용도                     | 처리                                         |
| ---------------------------- | ------------------------ | -------------------------------------------- |
| `node_modules/`              | `npm ci` result          | writable ephemeral; artifact로 보존하지 않음 |
| `dist/`                      | production static output | CI artifact로 선택 보존                      |
| `coverage/`                  | V8 coverage              | CI artifact로 선택 보존                      |
| `test-results/`              | WebM, trace, screenshot  | 실패 분석/A–Z evidence로 보존                |
| `sbom.json`                  | CycloneDX BOM            | CI artifact로 선택 보존                      |
| npm cache                    | downloaded packages      | writable reusable cache                      |
| Playwright browser path      | Chromium binaries        | writable reusable cache/PVC 가능             |
| sibling `../backend/target/` | E2E Maven output         | writable ephemeral                           |

Full E2E runner 시작 권장치는 2 vCPU, 4GiB RAM, 8GiB ephemeral storage다. Build/unit-only runner는
1 vCPU, 2GiB RAM, 4GiB부터 시작할 수 있다. 이는 보장값이 아니라 SDLC 환경별 튜닝 시작점이다.
Chromium 안정성을 위해 충분한 `/dev/shm`을 제공한다. 같은 checkout에서 병렬 job이 `dist/`,
`coverage/`, `test-results/`를 공유하지 않게 한다.

## 에이전트 변경 규칙

- UI → feature hook → coordinator/client → generated OpenAPI type 방향을 유지한다.
- `src/shared/`는 feature/app을 import하지 않고 feature끼리 내부 구현을 import하지 않는다.
- wire type을 손으로 만들지 말고 승인된 계약 변경 후에만 `contract:generate`를 실행한다.
- mutation 자동 retry를 추가하지 않는다. snapshot → optimistic write → server replacement/rollback
  순서를 유지한다.
- 사용자 내용은 text node로 렌더링하고 HTML/eval sink를 추가하지 않는다.
- 변경 후 최소 `npm run verify`; 상호작용 변경이면 real U1 E2E와 A–Z도 실행한다.

## 분리 저장소 체크리스트

1. `package.json`, `package-lock.json`, `src/`, `tests/`, `scripts/`, Vite/Vitest/Playwright/TS/ESLint
   config와 `public/`을 함께 이동한다.
2. Full gate runner는 backend repo를 정확히 `../backend`에 checkout한다.
3. 두 repo의 OpenAPI와 생성 선언을 같은 revision 조합으로 고정한다.
4. npm/Maven registry, CA trust, JDK, Chromium과 browser libraries를 runner image에 제공한다.
5. browser가 접근할 Backend URL과 Backend CORS Origin이 일치하는지 확인한다.
