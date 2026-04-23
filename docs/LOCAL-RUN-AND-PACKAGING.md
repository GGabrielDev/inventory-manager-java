# Local Run and Packaging

## Prerequisites

- Java 21
- Maven 3.9+
- PostgreSQL 14+

## Backend local run

```bash
cd backend
mvn spring-boot:run
```

Environment variables (defaults exist in `application.yml`):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRE_MINUTES`
- `PORT`, `CORS_ORIGIN`

## Frontend local run

```bash
cd frontend
INVENTORY_API_URL=http://localhost:4000/api mvn javafx:run
```

## Build artifacts

```bash
cd ..
mvn -q -pl backend,frontend -am package
```

- Backend jar: `backend/target/backend-1.0.0-SNAPSHOT.jar`

## Frontend native packaging baseline (jpackage)

Use JavaFX jmods from your JDK and package per-OS:

```bash
cd frontend
mvn -q package
jpackage \
  --name InventoryManagerDesktop \
  --input target \
  --main-jar frontend-1.0.0-SNAPSHOT.jar \
  --main-class com.inventorymanager.frontend.InventoryManagerDesktopApplication \
  --type app-image
```

`jpackage` output is OS-specific and should be generated on each target OS.
