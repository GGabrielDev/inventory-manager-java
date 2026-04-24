# Inventory Manager Java

Java fullstack rebuild of the existing `inventory-manager` project.

## Modules

- `backend/` - Spring Boot API with JWT, RBAC, PostgreSQL, Flyway, and JaVers audit logging
- `frontend/` - JavaFX desktop app that connects to the backend API
- `docs/` - architecture and API notes

## Quick start

1. Start PostgreSQL and create a database:
   - `inventory_manager_java`
2. Configure backend env:
   - copy `backend/.env.example` values into your environment
3. Run backend:
   - `cd backend && mvn spring-boot:run`
4. Run frontend:
   - `cd frontend && mvn javafx:run`

Backend default URL: `http://localhost:4000/api`  
Frontend API base URL default: `http://localhost:4000/api`

## Development

- Build all: `mvn -q -DskipTests package`
- Test backend: `cd backend && mvn test`

## Packaging and migration docs

- `docs/LOCAL-RUN-AND-PACKAGING.md`
- `docs/MIGRATION-NOTES.md`
- `docs/ITEM-REQUEST-WORKFLOW.md`

## GitHub repository setup

After authenticating with your GitHub account/token:

```bash
git branch -M main
git remote add origin <YOUR_GITHUB_REPO_URL>
git push -u origin main
```
