#!/bin/bash

set -euo pipefail

# Simple test runner for run-copilot-prepush-guards.sh
# Mocks Copilot and Git to verify hook logic paths.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
HOOK_SCRIPT="${ROOT_DIR}/tools/hooks/run-copilot-prepush-guards.sh"

TEST_TEMP_DIR=$(mktemp -d)
trap 'rm -rf "${TEST_TEMP_DIR}"' EXIT

# Mock setup
MOCK_BIN="${TEST_TEMP_DIR}/bin"
mkdir -p "${MOCK_BIN}"
export PATH="${MOCK_BIN}:${PATH}"

# Mock Git
cat > "${MOCK_BIN}/git" <<EOF
#!/bin/bash
# Log for debugging
# echo "GIT CALL: \$*" >> "${TEST_TEMP_DIR}/git_log.txt"

ARGS=("\$@")
# Handle -C
if [ "\${ARGS[0]}" == "-C" ]; then
  ARGS=("\${ARGS[@]:2}")
fi

if [ "\${ARGS[0]}" == "rev-parse" ] && [ "\${ARGS[1]}" == "--show-toplevel" ]; then
  echo "${ROOT_DIR}"
elif [ "\${ARGS[0]}" == "diff" ]; then
  echo "mock-diff-content-for-diff \${ARGS[@]:1}"
elif [ "\${ARGS[0]}" == "rev-parse" ] && [[ "\${ARGS[1]}" == "--verify" ]]; then
  # Mock no parent for SHA starting with 3
  if [[ "\${ARGS[2]}" == "3"* ]]; then exit 1; fi
  exit 0
elif [ "\${ARGS[0]}" == "hash-object" ]; then
  echo "empty-tree-sha"
else
  exit 0
fi
EOF
chmod +x "${MOCK_BIN}/git"

# Mock Copilot
cat > "${MOCK_BIN}/copilot" <<EOF
#!/bin/bash
# Extract status file path from prompt
STATUS_FILE=\$(echo "\$@" | grep -oE 'Status output path: [^ ]+' | cut -d' ' -f4)
REPORT_FILE=\$(echo "\$@" | grep -oE 'Report output path: [^ ]+' | cut -d' ' -f4)

if [ -n "\${STATUS_FILE}" ]; then
  echo "PASS" > "\${STATUS_FILE}"
fi
if [ -n "\${REPORT_FILE}" ]; then
  echo -e "## Mock Report\nPASS" > "\${REPORT_FILE}"
fi
EOF
chmod +x "${MOCK_BIN}/copilot"

assert_fail() {
  if "$@"; then
    echo "FAILED: Expected command to fail: $*"
    exit 1
  fi
}

assert_pass() {
  if ! "$@"; then
    echo "FAILED: Expected command to pass: $*"
    exit 1
  fi
}

assert_file_contains() {
  local file=$1
  local pattern=$2
  if ! grep -q "${pattern}" "${file}"; then
    echo "FAILED: File ${file} does not contain ${pattern}"
    # echo "Actual content:"
    # cat "${file}"
    exit 1
  fi
}

echo "Running tests for run-copilot-prepush-guards.sh..."

# 1. Test missing refs file
echo "• Test missing refs file"
assert_fail "${HOOK_SCRIPT}" ""

# 2. Test invalid SHA
echo "• Test invalid SHA"
REFS_FILE="${TEST_TEMP_DIR}/refs_invalid"
echo "refs/heads/main invalid-sha refs/heads/main 0000000000000000000000000000000000000000" > "${REFS_FILE}"
assert_fail "${HOOK_SCRIPT}" "${REFS_FILE}"

# 3. Test empty push
echo "• Test empty push"
REFS_FILE="${TEST_TEMP_DIR}/refs_empty"
echo "refs/heads/main 0000000000000000000000000000000000000000 refs/heads/main 0000000000000000000000000000000000000000" > "${REFS_FILE}"
assert_pass "${HOOK_SCRIPT}" "${REFS_FILE}"

# 4. Test normal push with artifact verification
echo "• Test normal push"
REFS_FILE="${TEST_TEMP_DIR}/refs_normal"
SHA1=$(printf '1%.0s' {1..40})
SHA2=$(printf '2%.0s' {1..40})
echo "refs/heads/main ${SHA1} refs/heads/main ${SHA2}" > "${REFS_FILE}"
assert_pass "${HOOK_SCRIPT}" "${REFS_FILE}"

# Verify latest artifacts
LATEST_POINTER="${ROOT_DIR}/.copilot/local-guards/latest-pre-push-run.txt"
LATEST_RUN=$(cat "${LATEST_POINTER}")
assert_file_contains "${LATEST_RUN}/push.diff" "mock-diff-content-for-diff ${SHA2}..${SHA1}"

# 5. Test root commit (no parent)
echo "• Test root commit"
REFS_FILE="${TEST_TEMP_DIR}/refs_root"
SHA_ROOT=$(printf '3%.0s' {1..40})
echo "refs/heads/new ${SHA_ROOT} refs/heads/new 0000000000000000000000000000000000000000" > "${REFS_FILE}"
assert_pass "${HOOK_SCRIPT}" "${REFS_FILE}"

LATEST_RUN=$(cat "${LATEST_POINTER}")
assert_file_contains "${LATEST_RUN}/push.diff" "mock-diff-content-for-diff empty-tree-sha..${SHA_ROOT}"

echo "✅ All tests passed!"

# 6. Test invalid caveman mode
echo "• Test invalid caveman mode"
REFS_FILE="${TEST_TEMP_DIR}/refs_caveman"
echo "refs/heads/main ${SHA1} refs/heads/main ${SHA2}" > "${REFS_FILE}"
export COPILOT_CAVEMAN_MODE="invalid"
assert_fail "${HOOK_SCRIPT}" "${REFS_FILE}"
unset COPILOT_CAVEMAN_MODE

echo "✅ All pre-push tests passed (including caveman validation)!"
