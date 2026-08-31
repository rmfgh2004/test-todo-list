# ai-dlc-sample

TODO: 프로젝트 한 줄 소개. (`aidlc-inputs/00-business-brief.md` 확정 후 채운다)

이 저장소는 **AI-DLC(AI-Driven Development Life Cycle)** 방식으로 개발한다.
팀원이 Claude Code를 쓰든 ChatGPT Codex를 쓰든 Kiro를 쓰든, **같은 규칙 엔진을 읽고 같은 절차로**
일하며, 그 과정이 `aidlc-docs/` 에 산출물과 감사 기록으로 남는다.

---

## 목차

1. [핵심 아이디어](#1-핵심-아이디어)
2. [저장소 구조와 파일별 역할](#2-저장소-구조와-파일별-역할)
3. [규칙이 로딩되는 흐름](#3-규칙이-로딩되는-흐름)
4. [전체 개발 흐름](#4-전체-개발-흐름)
5. [한 단계가 굴러가는 방식](#5-한-단계가-굴러가는-방식)
6. [도구별 시작 방법](#6-도구별-시작-방법)
7. [승인하는 법](#7-승인하는-법)
8. [추적성 — 안정 ID](#8-추적성--안정-id)
9. [상태 관리와 세션 재개](#9-상태-관리와-세션-재개)
10. [입력 변경 절차](#10-입력-변경-절차)
11. [검증](#11-검증)
12. [AI-DLC 준수 증거 매핑표](#12-ai-dlc-준수-증거-매핑표)
13. [자주 겪는 상황](#13-자주-겪는-상황)

---

## 1. 핵심 아이디어

AI에게 "알아서 만들어줘"라고 하면 빠르게 무언가가 나오지만, **왜 그렇게 만들었는지**가 남지 않는다.
AI-DLC는 그 사이에 절차를 넣는다.

세 가지 원칙이 전부다.

### ① 단계별 사람 승인 (Human-in-the-Loop)

AI는 각 단계에서 **먼저 계획을 제시**하고, 사람이 명시적으로 승인한 뒤에만 다음으로 넘어간다.
요구사항이 조금이라도 모호하면 구현 전에 **다지선다 역질문**을 던진다.
추측으로 채워 넣은 요구사항은 나중에 통째로 되돌려야 하므로, 묻는 쪽이 항상 싸다.

### ② 산출물의 일관성과 추적성

모든 요구사항은 안정 ID(`FR-001`)를 갖고, 그 ID가
**requirements → design → tasks → code → test** 를 관통한다.
`FR-018` 하나를 grep하면 "무엇을 요구했고 → 어떻게 설계했고 → 어떤 코드가 구현했고 →
어떤 테스트가 그것을 보장하는지"가 한 줄로 이어진다.

### ③ 판단 근거를 남기기

승인은 "승인합니다"가 아니라 **왜 그렇게 판단했는지**와 함께 기록한다.
AI 제안을 그대로 통과시킨 것인지, 검토 끝에 선택한 것인지는 근거의 유무로만 구별된다.
근거가 없는 승인 기록은 [aidlc-docs/audit.md](aidlc-docs/audit.md)에서 **불완전한 기록**으로 취급한다.

---

## 2. 저장소 구조와 파일별 역할

```
ai-dlc-sample/
│
├── .aidlc-rule-details/          ★ 규칙 엔진 (단일 진실 원천)
│   ├── core-workflow.md            워크플로 전문 — 모든 절차의 원본
│   ├── VERSION                     엔진 버전 (1.0.1)
│   ├── common/                     항상 로드되는 공통 규칙 (11)
│   ├── inception/                  Inception 단계별 규칙 (7)
│   ├── construction/               Construction 단계별 규칙 (6)
│   ├── operations/                 Operations 규칙 (1)
│   └── extensions/                 선택 적용 규칙 — security / resiliency / testing
│
├── CLAUDE.md                     ← Claude Code 진입점
├── AGENTS.md                     ← ChatGPT Codex / Cursor / Copilot 진입점
├── .kiro/
│   ├── steering/aidlc.md         ← Kiro 진입점
│   └── aws-aidlc-rule-details/     규칙 엔진 사본 (Kiro가 찾는 경로)
│
├── aidlc-inputs/                 ★ 입력 — 사람이 쓴다
│   ├── README.md                   입력↔산출물 분리 원칙, 변경 절차
│   ├── 00-business-brief.md        무엇을 / 왜 만드는가
│   ├── 01-tech-stack-decisions.md  기술 스택 (팀 전원 통일)
│   ├── 02-development-policy.md    코딩·TDD·커밋·안정 ID 규칙
│   └── 03-architecture-policy.md   아키텍처 방침과 제약
│
├── aidlc-docs/                   ★ 산출물 — AI-DLC가 쓴다
│   ├── audit.md                    모든 상호작용의 원문 기록
│   ├── aidlc-state.md              단계별 진행·승인 상태
│   ├── inception/                  요구사항·스토리·설계
│   ├── construction/               유닛별 설계·코드 계획·테스트
│   └── operations/                 운영 산출물
│
├── scripts/verify-aidlc-rules.sh  세 도구가 같은 엔진을 보는지 검증
└── README.md                      이 문서
```

### 세 종류의 파일을 구분하는 것이 중요하다

| 종류 | 경로 | 누가 쓰는가 | 언제 바뀌는가 |
|---|---|---|---|
| **규칙** | `.aidlc-rule-details/`, `CLAUDE.md`, `AGENTS.md`, `.kiro/` | 팀이 한 번 세팅 | 거의 안 바뀜 (엔진 업그레이드 시) |
| **입력** | `aidlc-inputs/` | **사람** | AI-DLC 실행 **밖에서**, PR로만 |
| **산출물** | `aidlc-docs/` | **AI-DLC 워크플로** | 각 단계 실행 시, 사람 승인 후 |

AI는 입력을 **읽기 전용**으로 취급하고, 사람은 산출물을 손으로 고치지 않는다.
이 경계가 무너지면 "입력이 결과에 맞춰 사후 수정됐는지" 알 수 없게 되고, 추적성이 사라진다.

---

## 3. 규칙이 로딩되는 흐름

세 도구는 서로 다른 파일을 진입점으로 읽지만, **결국 같은 규칙 엔진에 도달한다.**

```
  Claude Code          Codex / Cursor / Copilot          Kiro
       │                        │                          │
       ▼                        ▼                          ▼
   CLAUDE.md               AGENTS.md            .kiro/steering/aidlc.md
       │                        │                          │
       └────────────┬───────────┴──────────────┬───────────┘
                    │  세 파일의 규칙 본문은 동일 │
                    │  (블록 A / B / C)          │
                    ▼                            ▼
        [블록 A] 규칙 디렉터리 탐색 — 처음 존재하는 것 사용
             1. .aidlc-rule-details/           ← Claude / Codex / Cursor / Copilot
             2. .kiro/aws-aidlc-rule-details/  ← Kiro
                    │
                    ▼
            core-workflow.md (워크플로 전문) 로드
                    │
                    ├── common/process-overview.md
                    ├── common/session-continuity.md
                    ├── common/content-validation.md
                    ├── common/question-format-guide.md
                    ├── common/welcome-message.md
                    └── extensions/**/*.opt-in.md  (opt-in 프롬프트만 우선 로드)
                    │
                    ▼
            단계 진입 시 해당 규칙 파일을 그때 로드
             예) inception/requirements-analysis.md
                 construction/code-generation.md
```

### 진입점 3종에 들어 있는 것

각 진입점은 얇다(약 90줄). 워크플로 본문을 복제하지 않고 **엔진을 가리키기만** 한다.
그래서 엔진을 업데이트하면 세 도구가 동시에 새 규칙을 따른다.

| 블록 | 내용 |
|---|---|
| **[블록 A]** | 규칙 디렉터리 탐색 순서 |
| **[블록 B]** | 상시 운영 규칙 6가지 — 입력 원천, 역질문, 단계 승인, 판단 근거, 상태 관리 |
| **[블록 C]** | audit.md 기록 규칙 — 5개 필드, 원문 그대로, phase마다 누락 점검 |

그 아래에 워크플로 단계 요약표와 **"`core-workflow.md` 전문을 반드시 먼저 읽어라"** 는 지시가 있다.
진입점과 `core-workflow.md` 가 충돌하면 **`core-workflow.md` 가 우선**한다.

> `.kiro/aws-aidlc-rule-details/` 는 `.aidlc-rule-details/` 의 사본이다.
> Kiro가 그 경로만 탐색하기 때문에 둘을 함께 둔다. 두 디렉터리가 어긋나면
> 도구마다 다른 규칙으로 일하게 되므로, [11. 검증](#11-검증)의 스크립트로 확인한다.

---

## 4. 전체 개발 흐름

AI-DLC는 3개 Phase, 14개 Stage로 구성된다.
**모든 stage를 항상 실행하지는 않는다.** 작업 성격에 따라 필요한 stage만 적응적으로 고른다
(그 판단 자체도 계획으로 제시되고 승인을 받는다).

```
 ┌─ 🔵 INCEPTION ─────────── 무엇을, 왜 만드는가 ────────────────────┐
 │                                                                  │
 │  Workspace Detection      (항상)   그린필드/브라운필드 판정        │
 │        │                                                         │
 │  Reverse Engineering      (조건부) 기존 코드가 있을 때만          │
 │        │                                                         │
 │  Requirements Analysis    (항상)   ★ 요구사항 확정 + FR-XXX 부여  │
 │        │                                                         │
 │  User Stories             (조건부) 페르소나 · 스토리              │
 │        │                                                         │
 │  Workflow Planning        (항상)   실행 계획 수립                 │
 │        │                                                         │
 │  Application Design       (조건부) 컴포넌트 · 서비스 · 의존성     │
 │        │                                                         │
 │  Units Generation         (조건부) 작업 단위(unit) 분할           │
 └────────┼─────────────────────────────────────────────────────────┘
          ▼
 ┌─ 🟢 CONSTRUCTION ──────── 어떻게 만드는가 ────────────────────────┐
 │                                                                  │
 │   ┌── 유닛별 반복 (unit 1, unit 2, …) ──────────────────────┐    │
 │   │  Functional Design      (조건부) 도메인 · 비즈니스 규칙 │    │
 │   │  NFR Requirements       (조건부) 성능 · 보안 · 스택     │    │
 │   │  NFR Design             (조건부) 논리 컴포넌트 · 패턴   │    │
 │   │  Infrastructure Design  (조건부) 인프라 · 배포 구조     │    │
 │   │  Code Generation        (항상)   ★ 실제 코드 생성       │    │
 │   └─────────────────────────────────────────────────────────┘    │
 │                                                                  │
 │  Build and Test           (항상)   빌드 · 테스트 지침과 결과      │
 └────────┼─────────────────────────────────────────────────────────┘
          ▼
 ┌─ 🟡 OPERATIONS ────────── 어떻게 운영하는가 ──────────────────────┐
 │  Operations               (조건부) 운영 산출물                    │
 └──────────────────────────────────────────────────────────────────┘

 ── 모든 stage 경계에 사람의 명시적 승인 게이트가 있다 ──
```

### Phase별로 답하는 질문

| Phase | 답하는 질문 | 끝났을 때 손에 남는 것 |
|---|---|---|
| **Inception** | 무엇을, 왜 만드는가 | ID가 붙은 요구사항, 설계, 작업 단위 분할 |
| **Construction** | 어떻게 만드는가 | 유닛별 설계, 동작하는 코드, 테스트 |
| **Operations** | 어떻게 운영하는가 | 운영·배포 산출물 |

### 유닛(Unit of Work)이란

Inception의 Units Generation에서 시스템을 **독립적으로 만들고 테스트할 수 있는 덩어리**로 나눈다.
Construction은 그 유닛을 하나씩 완주한다. 유닛 하나가 끝나면 그 부분은 실제로 동작한다.
전체를 한 번에 설계하고 한 번에 구현하는 방식보다, 잘못된 방향을 일찍 발견할 수 있다.

---

## 5. 한 단계가 굴러가는 방식

Stage가 무엇이든 내부 사이클은 같다. **이 6단계가 AI-DLC의 실체다.**

```
 ┌─────────────────────────────────────────────────────────────┐
 │ 1. 계획 제시                                                 │
 │    AI가 이 단계에서 무엇을 할지 체크박스 계획으로 보여준다.   │
 │    aidlc-docs/**/plans/*.md 에 파일로 남는다.                │
 └───────────────────────┬─────────────────────────────────────┘
                         ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 2. 역질문 (모호하면 반드시)                                  │
 │    A/B/C/D 다지선다로 묻는다. 추측해서 진행하지 않는다.       │
 │    → 사람이 답한다. "모르겠다 / 나중에 정하자"도 유효한 답.   │
 └───────────────────────┬─────────────────────────────────────┘
                         ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 3. 사람 승인 ★ 게이트                                        │
 │    판단 근거와 함께 승인한다. 승인 없이는 다음으로 못 간다.   │
 └───────────────────────┬─────────────────────────────────────┘
                         ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 4. 실행 + 산출물 생성                                        │
 │    aidlc-docs/ 아래 정해진 경로에 문서를, 코드는 저장소       │
 │    루트에 만든다. (코드는 절대 aidlc-docs/ 안에 두지 않는다)  │
 └───────────────────────┬─────────────────────────────────────┘
                         ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 5. 체크박스 갱신 (2단계 추적)                                │
 │    계획 파일의 항목 [x] + aidlc-state.md 의 stage 상태        │
 │    → 작업을 끝낸 그 자리에서 즉시 갱신한다.                   │
 └───────────────────────┬─────────────────────────────────────┘
                         ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 6. audit.md 기록 + 누락 자체 점검                            │
 │    5개 필드로 append. phase 완료 시점마다 빠진 기록이 없는지  │
 │    스스로 점검하고, 있으면 보완한 뒤 다음 단계로 넘어간다.     │
 └───────────────────────┬─────────────────────────────────────┘
                         ▼
                   다음 stage 로
```

### 왜 계획을 파일로 남기는가

머릿속 계획은 검증할 수 없다. 체크박스 계획이 파일로 남으면
"어디까지 했고, 무엇을 건너뛰었는지"를 나중에 누구나 확인할 수 있다.
세션이 끊겨도 그 파일을 읽고 이어서 시작할 수 있다.

---

## 6. 도구별 시작 방법

먼저 **[aidlc-inputs/](aidlc-inputs/) 의 `TODO` 를 모두 채운다.**
TODO가 남은 채로 시작하면 AI가 그 빈칸을 추측으로 채우고, 그 추측이 요구사항으로 굳어진다.

| 도구 | 시작 방법 | 자동으로 읽는 파일 |
|---|---|---|
| **Claude Code** | 저장소 루트에서 `claude` 실행 후 요청 | `CLAUDE.md` |
| **ChatGPT Codex / Cursor / Copilot** | 저장소를 연 뒤 요청 | `AGENTS.md` |
| **Kiro** | 저장소를 연 뒤 요청 | `.kiro/steering/aidlc.md` |

요청은 평소처럼 하면 된다. 예: *"aidlc-inputs 기준으로 개발 시작해줘"*

그러면 도구는 진입점 → 규칙 엔진 순으로 읽고, **Inception의 Workspace Detection부터** 시작한다.
바로 코드를 쓰기 시작한다면 규칙을 읽지 않은 것이므로, 진입점을 읽었는지 되물어라.

> 어떤 도구로 시작했든 **다른 도구가 이어받을 수 있다.**
> 진행 상태는 `aidlc-docs/aidlc-state.md` 에, 맥락은 `audit.md` 와 산출물에 남기 때문이다.
> 도구를 바꿔도 사람이 같은 내용을 다시 설명할 필요는 없다.

---

## 7. 승인하는 법

승인은 이 방법론에서 **사람이 하는 거의 유일한 일**이며, 평가 대상이기도 하다.

### 좋은 승인

> Q2는 C로 가자. 자동 병합은 사용자가 결과를 예측할 수 없어서 신뢰를 잃는다.
> "나중에 저장한 쪽이 이긴다"는 조용한 데이터 유실이라 배제한다.
> 충돌 UI가 추가 비용이지만 데이터 유실보다 싸다고 봄.

- 선택지 중 무엇을 골랐는지 명확하다
- **왜** 그렇게 판단했는지가 있다
- 기각한 대안과 그 이유가 있다 → 나중에 같은 논의를 반복하지 않는다

### 나쁜 승인

> 승인합니다

무엇을 검토했는지, AI 제안을 읽기는 했는지 알 수 없다.
`audit.md` 의 `[사용자 판단·승인 근거]` 필드가 이 한 줄이면 **불완전한 기록**으로 본다.

### 승인 말고도 할 수 있는 것

| 상황 | 이렇게 말한다 |
|---|---|
| 제안이 과하다 | "3번은 이번 범위에서 빼자. 이유는 …" |
| 방향이 틀렸다 | "다시 제안해줘. 전제가 틀렸는데 우리는 …" |
| 지금 결정 못 한다 | "이건 보류. 미정으로 기록하고 다음으로 가자" |
| 모르겠다 | "선택지를 더 설명해줘" — 모른 채 승인하는 것보다 낫다 |

거절·수정 요청·보류도 **전부 audit.md에 기록된다.** 기록되는 것은 승인만이 아니다.

---

## 8. 추적성 — 안정 ID

모든 요구사항은 안정 ID(`FR-XXX`)를 갖고, 그 ID는
**requirements → design → tasks → code(docstring) → test(함수명)** 까지 관통한다.

```
  requirements.md          ## FR-018 동시 편집 충돌 처리
        │                       "두 사용자가 같은 문서를 편집하면 …"
        ▼
  functional-design/       ### 충돌 감지 규칙   구현: FR-018
        │
        ▼
  plans/*.md               - [x] 충돌 감지 서비스 구현 (FR-018)
        │
        ▼
  src/…/conflict.py        def detect_conflict(...):
        │                      """FR-018: 동시 편집 충돌을 감지한다."""
        ▼
  tests/…/test_conflict.py def test_FR_018_동시편집시_충돌을_사용자에게_노출한다():
```

확인 방법은 단순하다.

```bash
grep -rn "FR-018" aidlc-docs/ src/ tests/
```

5개 계층 전부에서 잡혀야 한다.

- **구현·테스트가 없는 요구사항** → 만들다 만 것
- **요구사항이 없는 코드** → 아무도 요청하지 않은 것

둘 다 결함으로 본다. 한 번 부여한 ID는 재사용하지 않으며, 폐기된 요구사항도
번호를 비우지 않고 `FR-007 (폐기, 사유: …)` 로 남긴다.
상세 규칙: [aidlc-inputs/02-development-policy.md](aidlc-inputs/02-development-policy.md)

---

## 9. 상태 관리와 세션 재개

| 파일 | 역할 |
|---|---|
| `aidlc-docs/aidlc-state.md` | 지금 어느 stage인가, 무엇을 끝냈고 무엇을 건너뛰었나 |
| `aidlc-docs/audit.md` | 어떤 대화와 판단으로 여기까지 왔나 (원문) |
| `aidlc-docs/**/plans/*.md` | 현재 stage 안에서 어디까지 했나 (체크박스) |

세션이 끊기거나 다음 날 다시 시작하면, 도구는 `aidlc-state.md` 를 먼저 읽고
**중단 지점부터 이어서** 진행한다. 사람이 맥락을 다시 설명할 필요가 없다.
새 대화를 시작할 때 진행이 리셋된 것처럼 보이면, `aidlc-state.md` 를 읽었는지 확인하라.

---

## 10. 입력 변경 절차

**입력 변경은 반드시 PR 리뷰를 거치며, AI-DLC 실행 중에는 입력을 변경하지 않는다.**

실행 도중 입력의 결함을 발견했다면:

```
 1. 진행을 멈춘다
 2. 발견 사실을 aidlc-docs/audit.md 에 기록한다
 3. aidlc-inputs/ 변경 PR을 올려 리뷰·머지한다
 4. 해당 stage를 다시 실행한다
```

실행 중에 입력을 슬쩍 고치면 "결과에 맞춰 요구사항을 사후 수정한 것"과 구별되지 않는다.
멈추고 고치고 재실행하는 편이 느려 보여도, 산출물의 신뢰를 지킨다.

---

## 11. 검증

세 도구가 정말 같은 엔진을 보고 있는지 언제든 확인할 수 있다.

```bash
./scripts/verify-aidlc-rules.sh
```

검사 항목:

1. `.aidlc-rule-details/` 와 `.kiro/aws-aidlc-rule-details/` 의 내용이 동일한가
2. 규칙 하위 디렉터리(common / inception / construction / operations / extensions)가 모두 있는가
3. 진입점 3종이 블록 A · B · C 를 모두 포함하는가
4. 진입점 3종의 규칙 본문 해시가 일치하는가
5. 세 진입점이 `core-workflow.md` 를 참조하는가
6. **`aidlc-docs/audit.md` 가 gitignore되고 있지 않은가**

불일치가 있으면 non-zero로 종료한다.

### 사람이 눈으로 확인할 것

- [ ] `aidlc-docs/audit.md` 의 모든 항목에 5개 필드가 있는가
- [ ] `[사용자 지시 원문]` 이 요약되지 않고 원문 그대로인가
- [ ] `[사용자 판단·승인 근거]` 가 "승인합니다"로만 채워진 항목이 없는가
- [ ] 각 stage 산출물이 [12번 표](#12-ai-dlc-준수-증거-매핑표)의 경로에 있는가
- [ ] 임의의 `FR-XXX` 를 grep했을 때 5개 계층 전부에서 잡히는가

---

## 12. AI-DLC 준수 증거 매핑표

| Phase | Stage | 산출물 | 경로 |
|---|---|---|---|
| — | 진행 상태 | 단계별 진행/승인 상태 | `aidlc-docs/aidlc-state.md` |
| — | 감사 기록 | 모든 상호작용의 원문 기록 | `aidlc-docs/audit.md` |
| INCEPTION | Workspace Detection | 워크스페이스 판정(그린/브라운필드) | `aidlc-docs/audit.md` |
| INCEPTION | Reverse Engineering | 기존 코드 분석(브라운필드 한정) | `aidlc-docs/inception/reverse-engineering/` |
| INCEPTION | Requirements Analysis | 요구사항, 확인 질문 | `aidlc-docs/inception/requirements/requirements.md`<br>`aidlc-docs/inception/requirements/requirement-verification-questions.md` |
| INCEPTION | User Stories | 페르소나, 사용자 스토리 | `aidlc-docs/inception/user-stories/` |
| INCEPTION | Workflow Planning | 실행 계획 | `aidlc-docs/inception/plans/execution-plan.md` |
| INCEPTION | Application Design | 컴포넌트·서비스·의존성 설계 | `aidlc-docs/inception/application-design/` |
| INCEPTION | Units Generation | 작업 단위(unit of work) 분할 | `aidlc-docs/inception/application-design/unit-of-work.md` |
| CONSTRUCTION | Functional Design | 도메인 엔터티, 비즈니스 규칙·로직 모델 | `aidlc-docs/construction/{unit}/functional-design/` |
| CONSTRUCTION | NFR Requirements | 비기능 요구사항, 기술 스택 결정 | `aidlc-docs/construction/{unit}/nfr-requirements/` |
| CONSTRUCTION | NFR Design | 논리 컴포넌트, NFR 설계 패턴 | `aidlc-docs/construction/{unit}/nfr-design/` |
| CONSTRUCTION | Infrastructure Design | 인프라·배포 아키텍처 | `aidlc-docs/construction/{unit}/infrastructure-design/` |
| CONSTRUCTION | Code Generation | 구현 코드와 생성 계획 | `aidlc-docs/construction/plans/{unit}-code-generation-plan.md` |
| CONSTRUCTION | Build and Test | 빌드·테스트 지침과 결과 요약 | `aidlc-docs/construction/build-and-test/` |
| OPERATIONS | Operations | 운영 산출물 | `aidlc-docs/operations/` |

> **애플리케이션 코드는 저장소 루트에 만든다.** `aidlc-docs/` 에는 문서만 들어간다.
> `aidlc-docs/` 와 `audit.md` 는 어떤 경우에도 gitignore하지 않는다.

---

## 13. 자주 겪는 상황

**AI가 규칙을 읽지 않고 바로 코드를 쓰기 시작한다**
→ 진입점을 읽었는지 되묻고, `.aidlc-rule-details/core-workflow.md` 를 먼저 읽게 한다.
새 대화에서 종종 발생한다. Workspace Detection부터 다시 시작시키면 된다.

**요구사항을 묻지 않고 추측으로 진행한다**
→ 멈추고 역질문을 요구한다. [블록 B]의 3번(모호하면 역질문)과 4번(단계 승인)이 지켜지지 않은 것이다.
이미 만든 산출물은 폐기하고 해당 stage를 재실행하는 편이 낫다.

**audit.md에 기록이 빠졌다**
→ 발견 즉시 보완해서 append하고, 보완했다는 사실 자체도 기록한다.
사후 보완은 은폐가 아니라 절차의 일부다. ([aidlc-docs/audit.md](aidlc-docs/audit.md)의 두 번째 예시 참조)

**팀원마다 AI가 다르게 행동한다**
→ `./scripts/verify-aidlc-rules.sh` 로 규칙 엔진이 어긋났는지 먼저 확인한다.
엔진이 같다면 입력(`aidlc-inputs/`)이 서로 다른 브랜치인지 확인한다.

**중간에 요구사항이 바뀌었다**
→ 실행을 멈추고 → audit.md에 기록 → `aidlc-inputs/` 변경 PR → 머지 후 재실행. ([10번](#10-입력-변경-절차))

**도구를 바꾸고 싶다**
→ 그냥 바꾸면 된다. 새 도구가 `aidlc-state.md` 를 읽고 중단 지점부터 이어간다.

---

## 시작 체크리스트

- [ ] `aidlc-inputs/` 의 `TODO` 를 전부 채웠다 (PR 리뷰 완료)
- [ ] `./scripts/verify-aidlc-rules.sh` 가 통과한다
- [ ] 팀원 전원이 [7. 승인하는 법](#7-승인하는-법)을 읽었다
- [ ] 각자 자기 도구로 저장소를 열고 Inception을 시작한다
