#!/bin/bash

set -euo pipefail

ROOT_DIR=$(git rev-parse --show-toplevel)
cd "${ROOT_DIR}"

COPILOT_BIN="${COPILOT_BIN:-copilot}"
FORCE=0

usage() {
  cat <<'EOF'
Usage: tools/scripts/run-docs-reinforcement-overhaul.sh [options]

Options:
  --force       Run even when working tree dirty.
  -h, --help    Show help.

This script:
1) audits static docs + live docs surfaces
2) updates adversary markdown prompts/rules from findings
3) writes reinforcement summary in .copilot/docs-reinforcement/<timestamp>/
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --force)
      FORCE=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

if ! command -v "${COPILOT_BIN}" >/dev/null 2>&1; then
  echo "Copilot CLI not found." >&2
  exit 1
fi

if [[ "${FORCE}" -ne 1 && -n "$(git status --porcelain)" ]]; then
  echo "Working tree not clean. Commit/stash first or use --force." >&2
  exit 1
fi

RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="${ROOT_DIR}/.copilot/docs-reinforcement/${RUN_ID}"
mkdir -p "${RUN_DIR}"

SUMMARY_FILE="${RUN_DIR}/summary.md"
RAW_LOG_FILE="${RUN_DIR}/reinforcement.raw.log"

echo "Running docs reinforcement + adversary markdown overhaul..."
"${COPILOT_BIN}" -p "
You are documentation reinforcement + adversary-rules maintainer.

Scope:
- static docs: README.md + docs/**/*.md
- live docs surfaces: backend OpenAPI/controller contracts and runtime-facing docs references
- adversary markdown used by guards:
  - .agents/rules/test-adversary-rules.md
  - .agents/rules/auditor-rules.md
  - .agents/prompts/local-prepush-copilot-adversary.md
  - .agents/prompts/local-prepush-copilot-auditor.md

Tasks:
1) detect stale/inconsistent docs vs code behavior.
2) reinforce docs wording to reduce ambiguity.
3) overhaul adversary markdown so checks align with real project invariants and current architecture.
4) keep sections structured, deterministic, and fail-closed.

Write rollout summary to: ${SUMMARY_FILE}
Summary sections:
- ## Updated Files
- ## Key Reinforcements
- ## New Adversary Rules
- ## Follow-up Gaps
" --add-dir "${ROOT_DIR}" --add-dir "${RUN_DIR}" --allow-tool="write" --allow-tool="shell" --no-ask-user 2>&1 | tee "${RAW_LOG_FILE}"

echo "Docs reinforcement summary:"
echo "  ${SUMMARY_FILE}"
echo "Done."
