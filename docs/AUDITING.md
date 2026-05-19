# Automated Auditing & Validation Pipeline

To support a high-velocity, "vibe-coded" development workflow, this project employs a strict, multi-stage automated auditing pipeline. This system ensures architectural integrity and security without manual code reviews.

## The Three-Stage Pipeline

### Stage 1: The Builder (Implementation)
- **Actor:** The primary AI agent (Gemini CLI) used for feature development.
- **Workflow:** Implement logic and UI components following `copilot-instructions.md`.
- **Constraint:** The Builder focuses on velocity and implementation; it is not trusted to validate its own work rigorously.

### Stage 2: The Test Adversary (PR Hard Guard)
- **Trigger:** Pull Request updates to protected branches.
- **Actor:** Dedicated Gemini "Adversary Agent" in GitHub Actions.
- **Logic:**
  - Reads changed files from PR diff context.
  - Executes backend tests and adversarial scrutiny.
  - Writes a strict `PASS`/`FAIL` verdict artifact consumed by CI.
- **Enforcement:** Merge is blocked by required status check when verdict is `FAIL`.

### Stage 3: The Architectural Auditor (PR Hard Guard)
- **Trigger:** Pull Request updates to protected branches.
- **Actor:** Dedicated Gemini "Architecture Agent" in GitHub Actions.
- **Logic:**
  - **RBAC:** Verifies all new Controller methods have `@PreAuthorize`.
  - **Audit:** Ensures write-path changes preserve JaVers audit integrity.
  - **UI Style:** Validates JavaFX layouts against `STYLE-GUIDE.md`.
  - **Integrity:** Checks the Physical Hierarchy (State > Municipality > Parish > Branch).
  - Writes a strict `PASS`/`FAIL` verdict artifact consumed by CI.
- **Enforcement:** Merge is blocked by required status check when verdict is `FAIL`.

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
- **Behavior:** Push is blocked when any check fails.

## Local Setup

To provision local prerequisites on a new machine:

```bash
./setup-pipeline.sh
```

After setup, each commit starts passive guards automatically.
Each push also runs the local Java pre-push gate.

## Bypassing

- Do not bypass local hooks with `--no-verify`; keep checks active in normal workflow.
- Hard checks run server-side in GitHub Actions and must pass before merge.
- Configure branch protection to require:
  - `CI / Build and Unit Tests`
  - `Gemini Hard Guard / Adversary Agent`
  - `Gemini Hard Guard / Architecture Agent`
