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
if [ "\$1" == "rev-parse" ] && [ "\$2" == "--show-toplevel" ]; then
  echo "${ROOT_DIR}"
elif [ "\$1" == "diff" ]; then
  echo "mock-diff-content"
elif [ "\$1" == "rev-parse" ]; then
  exit 0
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
  # Default to PASS for mock
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

echo "Running tests for run-copilot-prepush-guards.sh..."

# 1. Test missing refs file
echo "• Test missing refs file"
assert_fail "${HOOK_SCRIPT}" ""

# 2. Test invalid SHA
echo "• Test invalid SHA"
REFS_FILE="${TEST_TEMP_DIR}/refs_invalid"
echo "refs/heads/main invalid-sha refs/heads/main 0000000000000000000000000000000000000000" > "${REFS_FILE}"
assert_fail "${HOOK_SCRIPT}" "${REFS_FILE}"

# 3. Test empty push (all zero SHAs)
echo "• Test empty push"
REFS_FILE="${TEST_TEMP_DIR}/refs_empty"
echo "refs/heads/main 0000000000000000000000000000000000000000 refs/heads/main 0000000000000000000000000000000000000000" > "${REFS_FILE}"
assert_pass "${HOOK_SCRIPT}" "${REFS_FILE}"

# 4. Test normal push (mocked)
echo "• Test normal push"
REFS_FILE="${TEST_TEMP_DIR}/refs_normal"
SHA1=$(printf '1%.0s' {1..40})
SHA2=$(printf '2%.0s' {1..40})
echo "refs/heads/main ${SHA1} refs/heads/main ${SHA2}" > "${REFS_FILE}"
# This should pass if mock works
assert_pass "${HOOK_SCRIPT}" "${REFS_FILE}"

echo "✅ All tests passed!"
