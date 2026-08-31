# AI-DLC 워크플로 규칙 (Claude Code 진입점)

이 파일은 Claude Code가 세션 시작 시 자동으로 읽는 진입점이다.
CLAUDE.md / AGENTS.md / .kiro/steering/aidlc.md 세 진입점의 규칙 내용은 동일하며, 모두 같은 규칙 엔진을 가리킨다.
어떤 도구로 작업하든 아래 규칙을 동일하게 적용한다.

## [블록 A — 규칙 로딩 순서]

작업 시작 시 아래 경로를 순서대로 확인해 **처음 존재하는 것**을 규칙 디렉터리로 사용한다:

1. `.aidlc-rule-details/` (Claude Code / Codex / Cursor / Copilot)
2. `.kiro/aws-aidlc-rule-details/` (Kiro)

이후 모든 규칙 파일 참조(예: `common/process-overview.md`, `inception/workspace-detection.md`)는
위에서 결정된 규칙 디렉터리를 기준으로 하는 상대 경로다.
두 경로의 내용은 항상 동일해야 하며, `./scripts/verify-aidlc-rules.sh` 로 검증한다.

## [블록 B — 상시 운영 규칙 (매 작업에 항상 적용)]

1. 작업 시작 시 위 규칙 디렉터리의 AI-DLC 규칙을 먼저 읽고 숙지한다.
2. 개발 작업은 항상 `aidlc-inputs/` 를 입력 원천으로 삼아 Inception 단계부터 시작한다.
3. 요구사항이 조금이라도 모호하면, 구현 전에 반드시 역질문(다지선다)을 먼저 던진다.
   과신하지 말고, 애매하면 질문하는 쪽을 기본값으로 한다.
4. 각 phase(단계)는 사람의 명시적 승인을 받은 뒤에만 다음으로 진행한다.
5. AI 제안을 "승인합니다"로만 넘기지 않고, 판단 근거를 함께 남긴다.
6. 진행 상태는 `aidlc-docs/aidlc-state.md` 로 관리하며, 세션이 바뀌어도 이어서 재개한다.

## [블록 C — audit.md 기록 규칙 (엄격)]

1. 모든 상호작용을 `aidlc-docs/audit.md` 에 기록하며, 내 입력은 요약·바꿔쓰기 없이
   **"원문 그대로"** 남긴다.
2. 각 기록은 아래 5개 필드를 반드시 포함한다:
   `[타임스탬프]` / `[사용자 지시 원문]` / `[AI 제안 요약]`
   / `[사용자 판단·승인 근거]` / `[최종 결정]`
3. 각 phase 완료 시점마다 audit.md에 누락 없이 기록됐는지 스스로 점검하고,
   빠진 것이 있으면 즉시 보완한 뒤 다음 단계로 넘어간다.

### audit.md 기록 형식 (필수 템플릿)

```markdown
## [Stage Name 또는 Interaction Type]

- **[타임스탬프]**: YYYY-MM-DD HH:MM:SS (KST)
- **[사용자 지시 원문]**:
  > (사용자가 입력한 텍스트를 한 글자도 바꾸지 않고 그대로 인용)
- **[AI 제안 요약]**: (제안한 내용과 선택지, 근거를 요약)
- **[사용자 판단·승인 근거]**: (왜 그렇게 판단했는지 — "승인합니다"만으로는 불충분)
- **[최종 결정]**: (실제로 확정된 결론과 다음 단계)
```

기록은 항상 **append(추가)** 한다. 기존 항목을 덮어쓰거나 삭제하지 않는다.

## MANDATORY: 워크플로 전문 로드

**이 파일은 진입점(entry point)일 뿐이며, 워크플로의 단일 진실 원천이 아니다.**
작업을 시작하면 블록 A에서 결정한 규칙 디렉터리의 **`core-workflow.md` 전문을 반드시 먼저 읽고**,
그 지시에 따라 `common/` 규칙(process-overview, session-continuity, content-validation,
question-format-guide, welcome-message)과 각 단계별 규칙 파일을 로드한다.
이 진입점과 `core-workflow.md` 가 충돌하면 **`core-workflow.md` 가 우선**한다.

## 워크플로 개요 (요약 — 상세는 core-workflow.md)

각 단계는 계획 → 사람 승인 → 실행 → 산출물 → audit 기록 순서를 지키며,
승인 게이트를 건너뛰고 다음 단계로 넘어가지 않는다.

| Phase | Stage | 실행 조건 |
|---|---|---|
| INCEPTION | Workspace Detection | 항상 |
| INCEPTION | Reverse Engineering | 조건부 (브라운필드) |
| INCEPTION | Requirements Analysis | 항상 (깊이 적응) |
| INCEPTION | User Stories | 조건부 |
| INCEPTION | Workflow Planning | 항상 |
| INCEPTION | Application Design | 조건부 |
| INCEPTION | Units Generation | 조건부 |
| CONSTRUCTION | Functional Design (unit별) | 조건부 |
| CONSTRUCTION | NFR Requirements (unit별) | 조건부 |
| CONSTRUCTION | NFR Design (unit별) | 조건부 |
| CONSTRUCTION | Infrastructure Design (unit별) | 조건부 |
| CONSTRUCTION | Code Generation (unit별) | 항상 |
| CONSTRUCTION | Build and Test | 항상 |
| OPERATIONS | Operations | 조건부 |

## 산출물 위치

- 입력(사람이 관리): `aidlc-inputs/` — AI-DLC 실행 중에는 변경하지 않는다.
- 산출물(AI-DLC가 생성): `aidlc-docs/` — 진행 상태는 `aidlc-docs/aidlc-state.md`,
  감사 기록은 `aidlc-docs/audit.md`.
- 요구사항 안정 ID(`FR-XXX`)는 requirements → design → tasks → code(docstring) →
  test(함수명)까지 관통한다. 상세 규칙은 `aidlc-inputs/02-development-policy.md` 참조.
