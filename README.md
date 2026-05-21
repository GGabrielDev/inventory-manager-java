# Inventory Manager Java

Java fullstack rebuild of the existing `inventory-manager` project.

## Modules

- `backend/` - Spring Boot API with JWT, RBAC, PostgreSQL, Flyway, and JaVers audit logging
- `frontend/` - JavaFX desktop app that connects to the backend API
- `docs/` - architecture and API notes

## Quick start

1. Start PostgreSQL and create a database:
   - Default name: `inventory_manager_java` (or as set in `DB_NAME`)
2. Configure backend env:
   - copy `backend/.env.example` values into your environment
3. Run backend:
   - `cd backend && mvn spring-boot:run`
   - *Or run the compiled jar: `java -jar inventory-manager-backend.jar --DB_PASSWORD=your_password`*

### Backend Configuration Methods
When running the `.jar` file, you can provide configuration in three ways:
- **Command-line flags**: `java -jar backend.jar --DB_PASSWORD=secret --PORT=4001`
- **Environment variables**: `DB_PASSWORD=secret java -jar backend.jar`
- **External config file**: Place an `application.yml` file in the same directory as the `.jar`. Spring Boot will automatically detect and use it.

4. Run frontend:
   - `cd frontend && mvn javafx:run`
   - *Or run the compiled jar (double-click or via command line): `java -jar inventory-manager-frontend.jar`*

Backend default URL: `http://localhost:4000/api`  
Frontend API base URL default: `http://localhost:4000/api`

All non-login API routes require a JWT. Most mutating backend routes are also permission-gated with `@PreAuthorize`; the small diagnostics surface under `/api/test/*` stays auth-gated but not permission-gated.

## PostgreSQL Quickstart

The backend requires a PostgreSQL database. You can set it up quickly using Docker or manually via `psql`.

### Option A: Using Docker (Recommended)
Run the following command to start a PostgreSQL container with the correct database and credentials:
```bash
docker run --name inventory-db \
  -e POSTGRES_DB=inventory_manager_java \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  -d postgres:16
```

### Option B: Manual Setup (via psql)
If you have PostgreSQL installed locally, run these commands:
```sql
-- Connect to postgres as superuser
psql -U postgres

-- Create the database
CREATE DATABASE inventory_manager_java;

-- (Optional) Create a dedicated user
CREATE USER inventory_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE inventory_manager_java TO inventory_user;
```

If you use a custom user/password, make sure to update your `backend/.env` file.

## Development

- Build all: `mvn -q -DskipTests package`
- Test backend: `cd backend && mvn test`
- Test full suite (Java 21 + headless UI): `xvfb-run --auto-servernum mvn clean verify`

## Quality gates

- Remote required checks are the source of truth for merge enforcement.
- Optional local passive post-commit checks write to `.gemini/local-guards/latest-summary.md`.
- Local pre-push hook runs:
  - `mvn -q -DskipTests clean compile`
  - `mvn -q -pl backend test`
  - recursive headless Copilot adversary/auditor checks against `master` (auto-fix + retry, bounded attempts)
- Pre-push Copilot summary: `.copilot/recursive-guards/latest-summary.md`.
- Merge protection should require these GitHub checks:
  - `CI / Build and Unit Tests`
- `Gemini Hard Guard` is optional/manual (`workflow_dispatch`) when extra remote audit is needed.

## Documentation

Detailed guides for specific systems:
- [Physical Hierarchy & Locations](docs/LOCATIONS-AND-BRANCHES.md)
- [Bags & Displacements](docs/BAGS-AND-DISPLACEMENTS.md)
- [Item Request Workflows](docs/ITEM-REQUEST-WORKFLOW.md)
- [Testing Strategy](docs/TESTING.md)
- [Local Run & Packaging](docs/LOCAL-RUN-AND-PACKAGING.md)
- [Migration Notes](docs/MIGRATION-NOTES.md)

## GitHub repository setup

After authenticating with your GitHub account/token:

```bash
git branch -M main
git remote add origin <YOUR_GITHUB_REPO_URL>
git push -u origin main
```
