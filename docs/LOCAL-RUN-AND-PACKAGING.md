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

## Running the Released JARs

Once the project is built via the CI pipeline (or locally via `mvn clean package`), you can run the resulting executable `.jar` files directly.

### 1. Running the Backend Server

The backend requires Java 21 and a running PostgreSQL instance. You can configure the backend in three ways:

#### Option A: Command-Line Arguments (Recommended for one-offs)
Pass any configuration variable using the `--KEY=VALUE` syntax:
```bash
java -jar inventory-manager-backend.jar \
  --DB_HOST=localhost \
  --DB_PORT=5432 \
  --DB_NAME=${DB_NAME:-inventory_manager_java} \
  --DB_USER=postgres \
  --DB_PASSWORD=your_password
```

#### Option B: Environment Variables
Standard practice for servers and containers:
```bash
DB_PASSWORD=your_password DB_NAME=my_custom_db java -jar inventory-manager-backend.jar
```

#### Option C: External configuration file
If you place a file named `application.yml` in the **same directory** as the `.jar`, Spring Boot will automatically detect and use it. This is the cleanest way to manage a large number of settings.

If the database is not reachable, the application will print a helpful error message (referring to the `DB_NAME` and connection URL used) and exit cleanly.

### 2. Running the Frontend Desktop App

The frontend is a "fat" JAR containing JavaFX dependencies for Windows, macOS, and Linux. It only requires a standard Java 21 runtime.

**Double-click execution:**
On most systems with Java 21 installed, you can simply double-click `inventory-manager-frontend.jar` to launch the application.

**Command-line execution:**
```bash
java -jar inventory-manager-frontend.jar
```

*Note: The frontend will remember the last backend URL it connected to. If it fails to connect, a settings popup will appear allowing you to point it to the correct backend IP address.*

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
