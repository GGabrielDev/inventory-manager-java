# Automated Auditing & Validation Pipeline

This project uses a lightweight, local-only pre-push gate for basic validation before CI.

## Local Pre-Push Gate (Blocking)

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

CI remains the primary enforcement layer for merge protection. Configure branch protection to require:
  - `CI / Build and Unit Tests`
