#!/bin/bash

set -euo pipefail

ROOT_DIR=$(git rev-parse --show-toplevel)
SCRIPT="${ROOT_DIR}/tools/hooks/run-copilot-recursive-guards.sh"
TMP_DIR=$(mktemp -d)
trap 'rm -rf "${TMP_DIR}"' EXIT

make_mock_copilot() {
  cat > "${TMP_DIR}/copilot" <<'EOF'
#!/bin/bash
set -euo pipefail

PROMPT="${2:-}"
STATUS_FILE=$(printf '%s' "${PROMPT}" | sed -n 's/^-[[:space:]]*Status output path: //p' | head -n 1)
REPORT_FILE=$(printf '%s' "${PROMPT}" | sed -n 's/^-[[:space:]]*Report output path: //p' | head -n 1)
ATTEMPT_FILE="${MOCK_ATTEMPT_FILE}"

count=0
if [ -f "${ATTEMPT_FILE}" ]; then
  count=$(cat "${ATTEMPT_FILE}")
fi
count=$((count + 1))
echo "${count}" > "${ATTEMPT_FILE}"

case "${MOCK_MODE:-always-pass}" in
  always-pass)
    verdict="PASS"
    ;;
  always-fail)
    verdict="FAIL"
    ;;
  fail-then-pass)
    if [ "${count}" -le 2 ]; then
      verdict="FAIL"
    else
      verdict="PASS"
    fi
    ;;
  malformed)
    verdict="MAYBE"
    ;;
  *)
    verdict="FAIL"
    ;;
esac

if [ -n "${STATUS_FILE}" ]; then
  printf '%s\n' "${verdict}" > "${STATUS_FILE}"
fi

if [ -n "${REPORT_FILE}" ]; then
  cat > "${REPORT_FILE}" <<EOF2
## Mock Report

Verdict: ${verdict}
EOF2
fi

echo "mock copilot => ${verdict}"
EOF
  chmod +x "${TMP_DIR}/copilot"
}

assert_contains() {
  local needle="$1"
  local file="$2"
  grep -q -- "${needle}" "${file}"
}

run_case() {
  local name="$1"
  shift
  echo "case: ${name}"
  "$@"
}

make_mock_copilot

export PATH="${TMP_DIR}:$PATH"
export COPILOT_BIN="copilot"

# case 1: missing prompt file
if COPILOT_RECURSIVE_ADVERSARY_PROMPT_FILE="${TMP_DIR}/missing-adversary.md" \
  COPILOT_RECURSIVE_AUDITOR_PROMPT_FILE="${TMP_DIR}/missing-auditor.md" \
  MOCK_ATTEMPT_FILE="${TMP_DIR}/attempts-1" \
  MOCK_MODE="always-pass" \
  "${SCRIPT}" 1 >/dev/null 2>"${TMP_DIR}/missing.err"; then
  echo "missing prompt case should fail"
  exit 1
fi
missing_run_dir="$(cat "${ROOT_DIR}/.copilot/recursive-guards/latest-run.txt")"
assert_contains "Prompt file not found" "${missing_run_dir}/attempt-1/adversary/report.md"

# case 2: malformed verdict normalized to FAIL
cat > "${TMP_DIR}/adversary.md" <<EOF
adversary prompt
EOF
cat > "${TMP_DIR}/auditor.md" <<EOF
auditor prompt
EOF
if COPILOT_RECURSIVE_ADVERSARY_PROMPT_FILE="${TMP_DIR}/adversary.md" \
  COPILOT_RECURSIVE_AUDITOR_PROMPT_FILE="${TMP_DIR}/auditor.md" \
  MOCK_ATTEMPT_FILE="${TMP_DIR}/attempts-2" \
  MOCK_MODE="malformed" \
  "${SCRIPT}" 1 >/dev/null 2>"${TMP_DIR}/malformed.err"; then
  echo "malformed case should fail"
  exit 1
fi
latest_summary="$(cat "${ROOT_DIR}/.copilot/recursive-guards/latest-summary.md")"
printf '%s' "${latest_summary}" | grep -q "FAIL"

# case 3: retry until pass
if COPILOT_RECURSIVE_ADVERSARY_PROMPT_FILE="${TMP_DIR}/adversary.md" \
  COPILOT_RECURSIVE_AUDITOR_PROMPT_FILE="${TMP_DIR}/auditor.md" \
  MOCK_ATTEMPT_FILE="${TMP_DIR}/attempts-3" \
  MOCK_MODE="fail-then-pass" \
  "${SCRIPT}" 2 >/dev/null 2>"${TMP_DIR}/retry.err"; then
  :
else
  echo "retry case should pass"
  exit 1
fi
latest_summary="$(cat "${ROOT_DIR}/.copilot/recursive-guards/latest-summary.md")"
printf '%s' "${latest_summary}" | grep -q "Adversary: PASS"
printf '%s' "${latest_summary}" | grep -q "Auditor: PASS"

# case 4: max attempts fail closed
if COPILOT_RECURSIVE_ADVERSARY_PROMPT_FILE="${TMP_DIR}/adversary.md" \
  COPILOT_RECURSIVE_AUDITOR_PROMPT_FILE="${TMP_DIR}/auditor.md" \
  MOCK_ATTEMPT_FILE="${TMP_DIR}/attempts-4" \
  MOCK_MODE="always-fail" \
  "${SCRIPT}" 2 >"${TMP_DIR}/fail.out" 2>&1; then
  echo "max attempts case should fail"
  exit 1
fi
assert_contains "failed after 2 attempts" "${TMP_DIR}/fail.out"

echo "all recursive guard tests passed"
