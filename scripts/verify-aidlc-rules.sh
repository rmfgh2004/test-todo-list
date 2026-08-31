#!/usr/bin/env bash
# 세 도구(Claude Code / Codex·Cursor·Copilot / Kiro)가 동일한 AI-DLC 규칙 엔진을
# 가리키는지 검증한다. 불일치가 있으면 non-zero 로 종료한다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PRIMARY="$ROOT/.aidlc-rule-details"
KIRO="$ROOT/.kiro/aws-aidlc-rule-details"
ENTRIES=("$ROOT/CLAUDE.md" "$ROOT/AGENTS.md" "$ROOT/.kiro/steering/aidlc.md")
FAIL=0

fail() { echo "[FAIL] $*"; FAIL=1; }

# 1. 규칙 디렉터리 존재 확인
for d in "$PRIMARY" "$KIRO"; do
  [ -d "$d" ] || fail "규칙 디렉터리 없음: ${d#$ROOT/}"
done
[ "$FAIL" -eq 0 ] || exit 1

# 2. 두 규칙 디렉터리 내용 일치 확인
if diff -r "$PRIMARY" "$KIRO" > /dev/null; then
  COUNT=$(find "$PRIMARY" -type f | wc -l | tr -d ' ')
  VERSION=$(cat "$PRIMARY/VERSION" 2>/dev/null || echo "unknown")
  echo "[OK]  .aidlc-rule-details/ <-> .kiro/aws-aidlc-rule-details/"
  echo "      $COUNT files identical (engine v$VERSION)"
else
  fail "두 규칙 디렉터리의 내용이 다릅니다:"
  diff -rq "$PRIMARY" "$KIRO" || true
fi

# 3. 필수 하위 디렉터리 확인
for sub in common inception construction operations extensions; do
  [ -d "$PRIMARY/$sub" ] || fail "규칙 하위 디렉터리 없음: $sub"
done

# 4. 진입점 3종이 블록 A/B/C 를 모두 포함하는지 확인
for f in "${ENTRIES[@]}"; do
  [ -f "$f" ] || { fail "진입점 없음: ${f#$ROOT/}"; continue; }
  for block in "[블록 A" "[블록 B" "[블록 C"; do
    grep -qF "$block" "$f" || fail "${f#$ROOT/} 에 $block] 누락"
  done
  grep -qF "core-workflow.md" "$f" || fail "${f#$ROOT/} 가 core-workflow.md 를 참조하지 않음"
done

# 5. 진입점 3종의 규칙 본문이 동일한지 확인 (파일별 머리말 4줄 제외)
HASHES=$(for f in "${ENTRIES[@]}"; do tail -n +6 "$f" | shasum | cut -d' ' -f1; done | sort -u | wc -l)
if [ "$HASHES" -eq 1 ]; then
  echo "[OK]  entry points identical (blocks A/B/C + workflow body):"
  echo "      CLAUDE.md, AGENTS.md, .kiro/steering/aidlc.md"
else
  fail "진입점 3종의 규칙 본문이 서로 다릅니다"
fi

# 6. audit.md 가 gitignore 되지 않았는지 확인
if git -C "$ROOT" rev-parse --git-dir > /dev/null 2>&1; then
  if git -C "$ROOT" check-ignore -q aidlc-docs/audit.md 2>/dev/null; then
    fail "aidlc-docs/audit.md 가 gitignore 되고 있습니다 (절대 금지)"
  else
    echo "[OK]  aidlc-docs/ and audit.md are tracked (not ignored)"
  fi
fi

exit "$FAIL"
