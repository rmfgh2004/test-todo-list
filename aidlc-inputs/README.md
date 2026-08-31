# aidlc-inputs — AI-DLC 입력 원천

이 디렉터리는 **AI-DLC의 유일한 입력 원천(input source of truth)** 이다.
Inception 단계는 언제나 이 디렉터리의 문서를 읽는 것으로 시작한다.

## 입력 ↔ 산출물 분리 원칙

| 구분 | 경로 | 누가 작성하는가 | 언제 바뀌는가 |
|---|---|---|---|
| 입력 | `aidlc-inputs/` | **사람(팀)** 이 직접 작성·수정 | AI-DLC 실행 **밖에서**, PR로만 |
| 산출물 | `aidlc-docs/` | **AI-DLC 워크플로**가 생성 | 각 단계 실행 시, 사람 승인 후 |

- AI는 `aidlc-inputs/` 를 **읽기 전용**으로 취급한다. 실행 중 이 디렉터리의 파일을 고치지 않는다.
- 사람은 `aidlc-docs/` 산출물을 직접 손으로 고치지 않는다. 수정이 필요하면
  해당 단계를 다시 실행하거나, 판단 근거와 함께 `aidlc-docs/audit.md` 에 기록한다.

## 변경 규칙

- **입력 변경은 반드시 PR 리뷰를 거치며, AI-DLC 실행 중에는 입력을 변경하지 않는다.**
- 실행 중 입력의 결함을 발견하면: 진행을 멈추고 → 발견 사실을 `aidlc-docs/audit.md` 에 기록 →
  입력 변경 PR을 올려 리뷰·머지 → 그 뒤에 해당 단계를 재실행한다.
- 팀원 전원(Claude Code / ChatGPT Codex / Kiro 사용자)이 동일한 입력을 본다.
  도구별로 다른 입력을 두지 않는다.

## 파일 구성

| 파일 | 내용 |
|---|---|
| `00-business-brief.md` | 무엇을 / 왜 만드는가 (문제, 사용자, 성공 기준, 범위) |
| `01-tech-stack-decisions.md` | 프론트 / 백엔드 / AWS / AI 스택 — 팀 전원 통일 |
| `02-development-policy.md` | 코딩 규칙, TDD, 커밋 규칙, 안정 ID(FR-XXX) 규칙 |
| `03-architecture-policy.md` | 아키텍처 방침과 제약 |

각 파일의 `TODO:` 표시는 채워야 할 항목이다. **TODO가 남아 있는 채로 Inception을 시작하지 않는다.**
빈칸을 AI의 추측으로 채우게 두면 그 추측이 요구사항으로 굳어진다.
