# Frontend (JavaFX Desktop)

JavaFX desktop client that runs locally and connects to the backend API.

## Run

```bash
mvn javafx:run
```

## Configuration

Set backend API URL with env var:

```bash
INVENTORY_API_URL=http://localhost:4000/api
```

## Features

- Login with JWT token
- Permission-aware module tabs
- CRUD operations for all backend modules via JSON payload editor
- Changelog query per entity/id
- Item request workflow tab for operator-driven inventory operations:
  - create/edit/submit requests
  - admin-only review/execute actions
