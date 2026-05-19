#!/bin/bash

set -euo pipefail

ROOT_DIR=$(git rev-parse --show-toplevel)
cd "${ROOT_DIR}"

COPILOT_BIN="${COPILOT_BIN:-copilot}"
CREATE_FIX_BRANCH=0
IMPLEMENT_FIXES=0
FORCE=0
BRANCH_NAME=""

usage() {
  cat <<'EOF'
Usage: tools/scripts/run-master-consistency-audit.sh [options]

Options:
  --create-fix-branch [name]  Create fix branch from master after audit.
  --implement-fixes           Let Copilot implement fixes from audit plan.
  --force                     Skip clean-master guard.
  -h, --help                  Show help.

Outputs:
  .copilot/master-consistency-audit/<timestamp>/
    - findings.md
    - fix-plan.md
    - audit.raw.log
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --create-fix-branch)
      CREATE_FIX_BRANCH=1
      if [[ $# -gt 1 && "$2" != --* ]]; then
        BRANCH_NAME="$2"
        shift
      fi
      ;;
    --implement-fixes)
      IMPLEMENT_FIXES=1
      ;;
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

CURRENT_BRANCH=$(git branch --show-current)
if [[ "${FORCE}" -ne 1 ]]; then
  if [[ "${CURRENT_BRANCH}" != "master" ]]; then
    echo "Run on clean master. Current branch: ${CURRENT_BRANCH}" >&2
    exit 1
  fi
  if [[ -n "$(git status --porcelain)" ]]; then
    echo "Working tree not clean. Commit/stash first or use --force." >&2
    exit 1
  fi
fi

RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="${ROOT_DIR}/.copilot/master-consistency-audit/${RUN_ID}"
mkdir -p "${RUN_DIR}"

FINDINGS_FILE="${RUN_DIR}/findings.md"
PLAN_FILE="${RUN_DIR}/fix-plan.md"
RAW_LOG_FILE="${RUN_DIR}/audit.raw.log"

echo "Running master consistency audit..."
"${COPILOT_BIN}" -p "
You are principal consistency auditor for this repository.
Analyze current master branch for:
- cross-module logic inconsistencies
- fragile validations and null-handling pitfalls
- RBAC/audit/UI/domain invariant risks
- missing tests for critical paths

Write outputs to exact paths:
- findings report: ${FINDINGS_FILE}
- prioritized fix plan: ${PLAN_FILE}

Output requirements:
- findings.md sections: ## Verdict, ## Findings, ## High-Risk Pitfalls, ## Suggested Fixes
- fix-plan.md sections: ## Objectives, ## Ordered Fix Tasks, ## Test Plan
- Keep findings concrete with file paths and line references when possible.
- Do not modify repository source files in this step.
" --add-dir "${ROOT_DIR}" --add-dir "${RUN_DIR}" --allow-tool="write" --allow-tool="shell" --no-ask-user 2>&1 | tee "${RAW_LOG_FILE}"

echo "Audit artifacts:"
echo "  ${FINDINGS_FILE}"
echo "  ${PLAN_FILE}"

if [[ "${CREATE_FIX_BRANCH}" -eq 1 ]]; then
  if [[ -z "${BRANCH_NAME}" ]]; then
    BRANCH_NAME="fix/master-audit-${RUN_ID}"
  fi
  git switch -c "${BRANCH_NAME}"
  echo "Created branch: ${BRANCH_NAME}"
fi

if [[ "${IMPLEMENT_FIXES}" -eq 1 ]]; then
  IMPLEMENT_LOG="${RUN_DIR}/implement.raw.log"
  echo "Implementing fixes from plan..."
  "${COPILOT_BIN}" -p "
Read and execute fix plan from: ${PLAN_FILE}
Apply fixes to repository code incrementally.

Rules:
- Fix real code/tests/issues from plan, not guardrail scripts.
- Keep commits atomic by concern.
- After each concern, run relevant tests.
- Stop only when blocking issues from findings are addressed or clearly blocked.
" --add-dir "${ROOT_DIR}" --add-dir "${RUN_DIR}" --allow-tool="write" --allow-tool="shell" --no-ask-user 2>&1 | tee "${IMPLEMENT_LOG}"
fi

echo "Done."
