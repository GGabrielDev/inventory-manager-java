# Automated Auditing & Validation Pipeline

To support a high-velocity, "vibe-coded" development workflow, this project employs a strict, multi-stage automated auditing pipeline. This system ensures architectural integrity and security without manual code reviews.

## The Three-Stage Pipeline

### Stage 1: The Builder (Implementation)
- **Actor:** The primary AI agent (Gemini CLI) used for feature development.
- **Workflow:** Implement logic and UI components following `copilot-instructions.md`.
- **Constraint:** The Builder focuses on velocity and implementation; it is not trusted to validate its own work rigorously.

### Stage 2: The Test Adversary (Pre-Commit)
- **Trigger:** `git commit`
- **Actor:** Isolated "Adversary" persona with no context of the Builder's thoughts.
- **Logic:**
  - Analyzes the git diff.
  - Generates hostile JUnit/Mockito tests (null pointers, privilege escalation, malformed data).
  - Executes `mvn test`.
- **Enforcement:** Commit is blocked if adversarial tests fail or if tautological (useless) tests are detected.

### Stage 3: The Architectural Auditor (Pre-Push)
- **Trigger:** `git push`
- **Actor:** Isolated "Principal Auditor" persona.
- **Tools:** Custom TypeScript MCP Server (`tools/mcp-server`).
- **Logic:**
  - **RBAC:** Verifies all new Controller methods have `@PreAuthorize`.
  - **Audit:** Ensures new Entities are registered in `EntityRegistry` for JaVers.
  - **UI Style:** Validates JavaFX layouts against `STYLE-GUIDE.md`.
  - **Integrity:** Checks the Physical Hierarchy (State > Municipality > Parish > Branch).
- **Enforcement:** Push is blocked if any systemic invariants are violated.

## Local Setup

To activate the pipeline on a new machine:

```bash
./setup-pipeline.sh
```

## Bypassing
In emergencies, hooks can be bypassed using standard git flags (`--no-verify`), but this should be logged in the commit message.
