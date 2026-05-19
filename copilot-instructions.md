# Copilot Instructions - inventory-manager-java

## Project goals

- Keep behavioral parity with `~/Projects/inventory-manager` while migrating to Java.
- Backend: Spring Boot + PostgreSQL + JWT + RBAC + JaVers audit trail.
- Frontend: JavaFX desktop app that runs locally and talks to backend over HTTP.

## Engineering guardrails

- Keep API contracts stable and explicit (DTOs, validation, paginated responses).
- Enforce RBAC at service/controller boundary, never only in UI.
- Audit writes must include: actor, operation, entity, timestamp, field diffs.
- Prefer transactional service methods for write operations.
- Avoid hidden defaults and broad exception catches.
- Keep code modular by domain (`auth`, `user`, `role`, etc.).
- Write tests for auth, RBAC checks, and audit logging paths.

## Conventions

- Java 21.
- Constructor injection only.
- Map entities to DTOs explicitly.
- Use `ApiException` + global handler for uniform error payloads.
- Use Flyway migrations only (no schema auto-sync in production profile).
- Always make atomic, focused commits with descriptive Conventional Commits messages for any changes made by the AI.
- Prefer small incremental commits per PR (single concern per commit). Avoid large monolithic commits when changes can be split safely.
- After each local commit, review passive guard output in `.gemini/local-guards/latest-summary.md` and address blocking findings before next commit/push.
- Before pushing, review recursive headless Copilot pre-push summary at `.copilot/recursive-guards/latest-summary.md` when a push is rejected.
- Never bypass local hooks/checks with `--no-verify`.
- For pre-push failures, rerun recursive Copilot guard after fixes until PASS or max attempts reached.
- Do not edit guard scripts or instruction files as a workaround once they are confirmed working; fix project code/tests first.
- Follow the CI/CD pipeline rules: all new work must go into a separate branch, pass tests in `ci.yml`, and be merged via PR to trigger `release.yml` for changelog and artifact generation.
- Do not push directly into `master` unless for extremely minor changes or emergency hotfixes.
- Always reference and adhere to the project's style guidelines (e.g., `docs/STYLE-GUIDE.md`) when planning or programming UI components.
