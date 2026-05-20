# Automated Auditing & Validation Pipeline

To support a high-velocity, "vibe-coded" development workflow, this project employs a strict, multi-stage automated auditing pipeline. This system ensures architectural integrity and security without manual code reviews.

## The Three-Stage Pipeline

### Stage 1: The Builder (Implementation)
- **Actor:** The primary AI agent (Gemini CLI) used for feature development.
- **Workflow:** Implement logic and UI components following `copilot-instructions.md`.
- **Constraint:** The Builder focuses on velocity and implementation; it is not trusted to validate its own work rigorously.

### Stage 2: The Test Adversary (Local Pre-Push Copilot Guard)
- **Trigger:** Local `git push`.
- **Actor:** Headless Copilot adversary runner.
- **Logic:**
  - Reads branch-scope diff from merge-base with `master`.
  - Fails closed on missing tests for changed logic paths, null/malformed payload handling, RBAC bypass risk, and audit/hierarchy regressions.
  - Writes strict `PASS`/`FAIL` verdict artifact locally.
- **Enforcement:** Push blocked locally when verdict is `FAIL`.

### Stage 3: The Architectural Auditor (Local Pre-Push Copilot Guard)
- **Trigger:** Local `git push`.
- **Actor:** Headless Copilot auditor runner.
- **Logic:**
  - **RBAC:** Verifies controller methods keep explicit auth gating or stay inside the approved exception list (`/api/auth/login`, `/api/auth/validate`, `/api/auth/me`, `/api/test/*`, and docs endpoints).
  - **Audit:** Ensures every write-path change preserves JaVers audit integrity and the matching commit call.
  - **UI Style:** Validates JavaFX layouts against `STYLE-GUIDE.md` when frontend files change.
  - **Integrity:** Checks the physical hierarchy `State > Municipality > Parish > Branch > Department`.
  - Writes strict `PASS`/`FAIL` verdict artifact locally.
- **Enforcement:** Push blocked locally when verdict is `FAIL`.

## Local Passive Guardrail (Post-Commit, Non-Blocking)

- **Trigger:** Every local `git commit`.
- **Actor:** Local Gemini passive runners (`tools/hooks/post-commit` -> `tools/hooks/run-passive-guards.sh`).
- **Behavior:**
  - Commit succeeds immediately (no blocking).
  - Runner analyzes only the new commit and generates:
    - Adversary report
    - Auditor report
    - Summary report
- **Output:** `.gemini/local-guards/latest-summary.md` (and per-run artifacts).
- **Purpose:** Catch issues earlier with minimal commit-sized diffs before pushing.

## Local Pre-Push Java Gate (Blocking)

- **Trigger:** Every local `git push`.
- **Actor:** Git hook `tools/hooks/pre-push`.
- **Checks:**
  - `mvn -q -DskipTests clean compile`
  - `mvn -q -pl backend test`
  - Recursive headless Copilot adversary + auditor checks (`tools/hooks/run-copilot-recursive-guards.sh`)
    - Uses merge-base vs `master` as branch context.
- **Behavior:** Push is blocked when any check fails.
  - On failure, recursive runner can auto-apply targeted code/test fixes and retry (bounded by max attempts).

## Local Setup

To provision local prerequisites on a new machine:

```bash
./setup-pipeline.sh
```

After setup, each commit starts passive guards automatically.
Each push also runs the local Java pre-push gate and recursive Copilot loop against `master`.

## Bypassing

- Do not bypass local hooks with `--no-verify`; keep checks active in normal workflow.
- CI remains required for merge.
- Configure branch protection to require:
  - `CI / Build and Unit Tests`
- `Gemini Hard Guard` can be run manually (`workflow_dispatch`) for optional remote audit.
