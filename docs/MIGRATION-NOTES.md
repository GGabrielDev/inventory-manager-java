# Migration Notes (Node/React -> Java/JavaFX)

## Functional parity implemented

- Auth endpoints: `/api/auth/login`, `/api/auth/validate`, `/api/auth/me`
- RBAC permission convention preserved: `<action>_<entity>`
- CRUD modules:
  - users, roles, permissions
  - departments, categories, items
  - states, municipalities, parishes
- Changelog endpoint: `/api/changelogs/{entity}/{id}`

## Key stack changes

- Backend migrated from Express + Sequelize to Spring Boot + JPA + Flyway.
- Audit pipeline migrated from model-hook custom logger to JaVers SQL commits.
- Frontend migrated from React SPA to JavaFX desktop app using backend REST.

## Default admin seed

- username: `admin`
- password: `<see_env_example>`

## Behavioral differences

- JavaFX UI currently uses JSON payload editors for fast CRUD parity and debugging.
- JaVers changelog payload shape differs internally from Sequelize ChangeLog schema, but operation intent (`create/update/delete/link/unlink`) and actor/timestamp context are preserved.

## Quality Assurance & Auditing

- **Automated Pipeline:** The project uses a multi-stage AI auditing pipeline (Builder -> Adversary -> Auditor).
- **Enforcement:** Validation is enforced server-side via required GitHub Actions checks on pull requests.
- **Details:** See [AUDITING.md](AUDITING.md) for full architecture and setup instructions.
