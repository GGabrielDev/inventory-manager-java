# Testing Strategy and Workflow

The Inventory Manager project follows a multi-tier testing strategy to ensure reliability across the full stack.

## Testing Tiers

| Tier | Name | Technology | Focus |
| :--- | :--- | :--- | :--- |
| **L1** | **Unit** | JUnit 5, Mockito | Pure logic, DTO mapping, and service method isolation. |
| **L2** | **Integration** | H2 (Postgres Mode) | Data persistence, Repository query logic, and Database constraints. |
| **L3** | **Functional** | MockMvc | Controller endpoints, JSON serialization, and RBAC security rules. |
| **L4** | **Acceptance** | Scenario Tests | Business process verification (e.g., the "Zero Paperwork" borrowing flow). |
| **L5** | **Frontend** | MockWebServer | API client robust parsing and error handling. |

## Running Tests

### Root (Full Suite)
Runs every test in both backend and frontend:
```bash
mvn clean verify
```

### Backend Only
```bash
mvn test -pl backend
```

### Frontend Only
```bash
mvn test -pl frontend
```

## CI/CD Integration
The `ci.yml` workflow enforces these tests on every Pull Request.
- **PR Title Linter**: Ensures Conventional Commits.
- **Version Guard**: Ensures module versions match and are higher than the latest release.
- **Build and Unit Tests**: Executes the full suite.

## Development Workflow
1. Create a feature branch.
2. Implement code and matching tests (L1/L2).
3. Verify locally with `mvn test`.
4. Open PR and wait for CI green light.
