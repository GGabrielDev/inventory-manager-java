# Inventory Manager — Demo & Showcase Guide

## Table of Contents
1. [Running the Frontend](#1-running-the-frontend)
2. [Connecting to a Backend](#2-connecting-to-a-backend)
3. [System Architecture Overview](#3-system-architecture-overview)
4. [Navigation & UI Layout](#4-navigation--ui-layout)
5. [Full Feature Walkthrough](#5-full-feature-walkthrough)
6. [Testing & Verification Checklist](#6-testing--verification-checklist)

---

## 1. Running the Frontend

The frontend is a JavaFX desktop application distributed as a runnable JAR. No installation needed — just Java 21.

### Requirements
- Java 21 JRE (or JDK)
- A display server (Windows/macOS/Linux desktop)
- Network access to the backend server (port 4002 or 4003)

### Running

```bash
# On Linux with display
java -jar inventory-frontend.jar

# On Linux headless (SSH) — won't work, needs a screen
# On Windows — double-click or:
java -jar inventory-frontend.jar

# Set the API URL via environment variable (optional):
INVENTORY_API_URL=http://192.168.1.21:4002/api java -jar inventory-frontend.jar
```

The app will open a login window. Login with `admin` / `password`.

> **Note for Linux**: If you get a JavaFX error about missing libraries, install OpenJFX:
> ```bash
> sudo apt-get install openjfx
> ```
> Or if running in CI/headless: use `xvfb-run java -jar inventory-frontend.jar`

### Frontend JAR Location

The frontend JAR has been copied to the server at:
- `/home/logistica/inventory-frontend.jar`

Or you can find it in the project:
- `/home/gabogg/Projects/inventory-manager-java/frontend/target/frontend-1.12.1-SNAPSHOT.jar`

---

## 2. Connecting to a Backend

Two backend instances are running on the server:

| Instance | Port | URL | Auto-restart | Data |
|----------|------|-----|-------------|------|
| **Production** | `4002` | `http://192.168.1.21:4002/api` | ✅ Yes (survives reboots) | Empty (seed only) |
| **Demo** | `4003` | `http://192.168.1.21:4003/api` | ❌ Manual start only | Preloaded with 17 items, 3 bags, locations, etc. |

### Changing the API URL

**Method 1 — Environment variable (recommended for demos):**
```bash
INVENTORY_API_URL=http://192.168.1.21:4003/api java -jar inventory-frontend.jar
```

**Method 2 — In-app settings (to switch at runtime):**
1. Log in with any backend
2. Click the ⚙️ gear icon (bottom of sidebar)
3. Change the API URL field
4. Click Save
5. You'll be returned to login — log in again

### Server Management

```bash
# On the server (as root):

# Check services
systemctl status inventory-prod
systemctl status inventory-demo

# View logs
journalctl -u inventory-prod --no-pager -n 50
journalctl -u inventory-demo --no-pager -n 50

# Restart
systemctl restart inventory-prod
systemctl restart inventory-demo

# Demo needs manual start after server reboot:
systemctl start inventory-demo
```

---

## 3. System Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    USER'S COMPUTER                           │
│  ┌─────────────────────────────────────┐                    │
│  │      JavaFX Desktop Application     │                    │
│  │  (inventory-frontend.jar)           │                    │
│  │                                     │                    │
│  │  ┌─────────────────────────────────┐│                    │
│  │  │  LoginView    → JWT token       ││                    │
│  │  │  DashboardView → stats cards    ││                    │
│  │  │  ResourceView  → CRUD tables    ││                    │
│  │  │  FormView      → dynamic forms  ││                    │
│  │  │  AuditView     → JaVers logs    ││                    │
│  │  │  Bag Scanner   → barcode audit  ││                    │
│  │  └─────────────────────────────────┘│                    │
│  │                                     │                    │
│  │  ApiClient ←→ REST over HTTP        │                    │
│  └──────────────────┬──────────────────┘                    │
└─────────────────────┼────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                   SERVER (192.168.1.21)                      │
│                                                              │
│  ┌──────────────────────────────────────────────────┐       │
│  │  PROD (port 4002)      DEMO (port 4003)         │       │
│  │  ┌──────────────────┐  ┌──────────────────┐      │       │
│  │  │ Spring Boot API  │  │ Spring Boot API  │      │       │
│  │  │                  │  │                  │      │       │
│  │  │ JWT Auth (RBAC)  │  │ JWT Auth (RBAC)  │      │       │
│  │  │ JaVers Auditing  │  │ JaVers Auditing  │      │       │
│  │  │ Flyway Migrations│  │ Flyway Migrations│      │       │
│  │  └────────┬─────────┘  └────────┬─────────┘      │       │
│  │           │                      │                │       │
│  │           ▼                      ▼                │       │
│  │  ┌──────────────┐    ┌──────────────┐            │       │
│  │  │ PostgreSQL   │    │ PostgreSQL   │            │       │
│  │  │ inventory_   │    │ inventory_   │            │       │
│  │  │ prod         │    │ demo         │            │       │
│  │  └──────────────┘    └──────────────┘            │       │
│  └──────────────────────────────────────────────────┘       │
│                                                              │
│  Both run on the same JAR (backend-exec.jar, 60MB)          │
│  Different config via systemd Environment= variables         │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology |
|-------|-----------|
| Frontend | JavaFX 21 with FXML-free layouts, CSS styling |
| Backend | Spring Boot 3.3.5, Java 21 |
| Database | PostgreSQL 17 (local) with Flyway migrations |
| Auth | JWT tokens, BCrypt passwords, Spring Security RBAC |
| Auditing | JaVers — full entity change history |
| ORM | Spring Data JPA, Hibernate 6 |
| Build | Maven multi-module (backend + frontend) |

### Security Model

- **RBAC (Role-Based Access Control)**: Every API endpoint is protected by `@PreAuthorize("hasAuthority('action_entity')")`
- **JWT**: Token-based auth, HMAC-SHA signed, 8-hour expiry by default
- **Permissions**: 59 granular permissions (create/get/edit/delete × 14 entities + 3 workflow permissions)
- **Roles**: `admin` (all permissions), `operator` (read + request workflow)
- **Admin detection**: Computed client-side by checking 10 specific permissions

---

## 4. Navigation & UI Layout

```
┌──────────────────────────────────────────────────────────────────────┐
│  [Inventory Manager]                    [🌐 Global]  User: admin [Logout]│
├──────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────┐  ┌──────────────────────────────────────────────┐│
│ │ ☰ Dashboard     │  │                                              ││
│ │ ─────────────── │  │           (Content Area)                     ││
│ │ INVENTORY       │  │                                              ││
│ │   📦 Assets     │  │   Changes dynamically based on navigation    ││
│ │   🎒 Bags       │  │                                              ││
│ │ ─────────────── │  │   - Dashboard: stats cards                  ││
│ │ OPERATIONS      │  │   - ResourceView: searchable tables          ││
│ │   🔄 Transfers  │  │   - FormView: dynamic upsert forms          ││
│ │   📋 Audit      │  │   - AuditView: JaVers changelog browser     ││
│ │   📤 Displacem. │  │   - Bag Audit: barcode scanner              ││
│ │ ─────────────── │  │                                              ││
│ │ ADMIN           │  │                                              ││
│ │   📊 Audit Logs │  │                                              ││
│ │   🏢 Branches   │  │                                              ││
│ │   🏗️ Departments│  │                                              ││
│ │   📁 Categories │  │                                              ││
│ │ ─────────────── │  │                                              ││
│ │ IDENTITY        │  │                                              ││
│ │   👥 Users      │  │                                              ││
│ │   🛡️ Roles      │  │                                              ││
│ │   🔑 Permissions│  │                                              ││
│ │ ─────────────── │  │                                              ││
│ │ LOCATIONS       │  │                                              ││
│ │   🗺️ States     │  │                                              ││
│ │   🏘️ Muni.      │  │                                              ││
│ │   📍 Parishes   │  │                                              ││
│ │ ─────────────── │  │                                              ││
│ │ ⚙️ Settings     │  │                                              ││
│ └─────────────────┘  └──────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────────┘
```

### Navigation Groups

Every user sees **Dashboard, INVENTORY, OPERATIONS**, and **Settings**.

Admin users additionally see **ADMIN, IDENTITY, LOCATIONS** sections.

The **🌐 Global** toggle (admin only) switches between branch-scoped and global data views.

---

## 5. Full Feature Walkthrough

### 5.1 Login & Authentication

1. **Launch the app**: Double-click the JAR or run via terminal
2. **Login screen** appears with username/password fields
3. **Enter credentials**: `admin` / `password`
4. **Upon success**: Dashboard loads with full sidebar (admin access)
5. **On wrong password**: Error popup — "Invalid username or password"
6. **On unauthenticated access**: API returns 403 Forbidden

**What to confirm:**
- [ ] Login accepts valid credentials
- [ ] Login rejects invalid credentials with error message
- [ ] After login, dashboard shows with correct username in header
- [ ] Admin user sees all navigation sections
- [ ] Operator user (if created) sees limited navigation
- [ ] Logout works (returns to login screen)

---

### 5.2 Dashboard

The dashboard shows summary cards:
- **Total Assets**: Currently shows "..." (placeholder — uses counts from API)
- **Pending Reviews**: Placeholder
- **Active Displacements**: Placeholder

**What to confirm:**
- [ ] Dashboard loads without errors after login

---

### 5.3 Location Hierarchy (States → Municipalities → Parishes)

This is the fundamental data structure. The hierarchy is:

```
State
└── Municipality (belongs to State)
    └── Parish (belongs to Municipality)
```

**Create a State:**
1. Navigate: LOCATIONS → States
2. Click **Add**
3. Enter a name (e.g., "Demo State")
4. Click **Save**
5. The new state appears in the table with an audit trail

**Create a Municipality:**
1. Navigate: LOCATIONS → Municipalities
2. Click **Add**
3. Enter a name
4. Select the parent State from the dropdown combo
5. Click **Save**

**Create a Parish:**
1. Navigate: LOCATIONS → Parishes
2. Click **Add**
3. Enter a name
4. Select State → then Municipality (combo loads dependent on State)
5. Click **Save**

**Edit/Delete:**
- Click a row, then Edit/Delete buttons appear
- Edit opens the form prefilled
- Delete is blocked if entities reference this record

**What to confirm:**
- [ ] State → Municipality → Parish chain works
- [ ] Municipality dropdown filters by parent State
- [ ] Parish dropdowns cascade (State → Municipality)
- [ ] Edit preserves relationships
- [ ] Delete is blocked if children exist (referential integrity)

---

### 5.4 Branches (Locations + Address)

Branches combine a physical address with the location hierarchy.

**Create a Branch:**
1. Navigate: ADMIN → Branches
2. Click **Add**
3. Enter: Name, Address
4. Select: State → Municipality → Parish (3 cascading combos)
5. Click **Save**

Upon creation, the system **auto-creates two departments**: "Inbound" and "Storage" — these are the default operational departments for the branch.

**Edit/Delete:**
- Edit preserves location selections
- Delete is blocked if branch has departments, items, users, bags, or requests

**What to confirm:**
- [ ] Branch creates successfully with all 3 location selections
- [ ] Inbound + Storage departments are auto-created
- [ ] Edit preserves existing data
- [ ] Delete blocked when items/users/bags exist

---

### 5.5 Departments

Departments belong to branches — they represent internal subdivisions.

**Create a Department:**
1. Navigate: ADMIN → Departments
2. Click **Add**
3. Enter a name
4. Select the parent Branch from the dropdown
5. Click **Save**

Departments are used to scope items, bags, and item requests.

**What to confirm:**
- [ ] Department creates linked to branch
- [ ] Same department name can exist in different branches
- [ ] Department shows in item/bag forms as filter

---

### 5.6 Categories

Categories classify items (Electrónicos, Ferretería, Oficina, etc.).

**Create a Category:**
1. Navigate: ADMIN → Categories
2. Click **Add**
3. Enter a name
4. Click **Save**

**What to confirm:**
- [ ] Category CRUD works
- [ ] Category shows in item creation form
- [ ] Deleting a category with items is blocked

---

### 5.7 Items

Items represent physical inventory — they're the core of the system.

**Create an Item:**
1. Navigate: INVENTORY → Assets
2. Click **Add**
3. Fill in:
   - Name (required)
   - Quantity (required, minimum 1)
   - Unit type: UND (units), KG (kilograms), L (liters), M (meters)
   - Observations (optional, text area)
   - Category (optional dropdown)
   - Branch (required)
   - Department (required, must belong to selected branch)
   - Characteristics JSON (optional, for custom metadata)
4. Click **Save**

**Item properties to note:**
- Items are scoped to a Branch + Department
- Each item has a unique name
- The characteristics JSON field stores custom attributes
- Items can be assigned to Bags
- Items can be "displaced" (borrowed)
- Displacements track item flow

**What to confirm:**
- [ ] Item creates with all fields
- [ ] Quantity can be any integer ≥ 1
- [ ] Unit type defaults to UND
- [ ] Observations saves as text
- [ ] Edit preserves branch/department relationships
- [ ] Delete (soft-delete, sets deleted_at)

---

### 5.8 Bags

Bags are pre-configured kits containing expected items. They're used for inventory audits.

**Create a Bag:**
1. Navigate: INVENTORY → Bags
2. Click **Add**
3. Fill in:
   - Name
   - Barcode (unique identifier, scannable)
   - Branch
   - Assigned Department
   - Expected Items (table — add items with expected quantity)
4. Click **Save**

**Bag Audit (Scanner):**
1. Navigate: OPERATIONS → Audit
2. Enter a barcode in the search field
3. Click **Scan**
4. Shows audit table:
   - ITEM: item name
   - EXPECTED: intended quantity
   - BORROWED: currently displaced quantity
   - REMAINING: expected - borrowed
   - ANOMALIES: count of discrepancies
   - MISSING button: creates displacement report
5. Items with 0 remaining are highlighted in red
6. Anomalies > 0 are highlighted in orange

**What to confirm:**
- [ ] Bag creates with expected items
- [ ] Barcode lookup works
- [ ] Bag audit shows correct quantities
- [ ] MISSING button opens displacement form prefilled
- [ ] Barcode is unique

---

### 5.9 Displacements (Borrow/Return)

Displacements track when items are borrowed from inventory.

**Create a Displacement (Borrow):**
1. Navigate: OPERATIONS → Displacements
2. Click **Add**
3. Fill in:
   - Bag (optional — displacement via bag audit)
   - Item (required)
   - Reason (required — why it's being borrowed)
   - Borrower Name (required — who has it)
   - Expected Return Date (optional)
4. Click **Save**
5. Status is **ACTIVE** immediately

**Resolve a Displacement (Return):**
- Displacements are resolved via the API (no frontend button currently)
- Status changes from ACTIVE → RESOLVED
- Timestamps: `removed_at` (when borrowed), `resolved_at` (when returned)

**Business rules:**
- Items are not removed from quantity — displacement is a separate tracking system
- A displacement can reference either an Item or a Bag
- Resolved displacements remain in the system as permanent records

**What to confirm:**
- [ ] Displacement creates with ACTIVE status
- [ ] Borrower name and reason are stored
- [ ] Displacements appear in the list
- [ ] Resolve changes status to RESOLVED

---

### 5.10 Item Requests (Workflow)

Item requests are a multi-step approval workflow for inventory operations.

**Status flow:**
```
DRAFT → PENDING_REVIEW → APPROVED → EXECUTED
                        → REJECTED → (terminal)
→ CANCELLED (from any non-terminal state)
```

**Request types:**
- **INBOUND**: New item coming into inventory
- **MODIFICATION**: Change existing item fields
- **TRANSFER**: Move items between branches/departments
- **DISINCORPORATION**: Remove items from inventory (reduced quantity or deleted)
- **ADJUSTMENT**: Quantity correction (signed delta)

**Full workflow:**
1. A user creates a DRAFT request with item entries
2. Submit → status becomes PENDING_REVIEW
3. A reviewer (with permissions) approves or rejects
4. If approved, an executor executes the request
5. Execution performs the actual inventory change (create item, update, transfer, etc.)

**Note:** The frontend has a ResourceView for item-requests but the workflow buttons are currently only available via the API.

**What to confirm (via API if needed):**
- [ ] Request creates as DRAFT
- [ ] Submit changes to PENDING_REVIEW
- [ ] Review approval changes to APPROVED
- [ ] Execute performs the action
- [ ] Review rejection changes to REJECTED (terminal)

---

### 5.11 Users, Roles & Permissions

**Permissions** are 59 atomic actions like `create_item`, `get_audit_logs`, `submit_item_request`.

**Roles** group permissions:
- `admin`: All 59 permissions
- `operator`: Read-only + request workflow + create_displacement

**Users** have roles and a branch assignment.

**Create a User:**
1. Navigate: IDENTITY → Users
2. Click **Add**
3. Fill in:
   - Username
   - Password (required for new users)
   - Branch (optional, dropdown)
   - Roles (multi-select list)
4. Click **Save**

**What to confirm:**
- [ ] Permission CRUD works
- [ ] Role creates with permission assignments
- [ ] User creates with role + branch
- [ ] New user can login with their credentials
- [ ] Operator user sees limited navigation
- [ ] Password can be left blank on edit (keeps existing)

---

### 5.12 Audit Logs (JaVers)

Every entity change is recorded: who changed what, when, and the before/after state.

**View Audit Logs:**
1. Navigate: ADMIN → Audit Logs (admin only)
2. Select an Entity Type (e.g., "state")
3. Optionally enter an Entity ID
4. View the changelog with:
   - Operation: INITIAL, UPDATE, DELETE
   - Changed by: username of the actor
   - Changed at: timestamp
   - State: full entity snapshot after change
   - Previous State: full entity snapshot before change

**What to confirm:**
- [ ] Creating an entity produces an INITIAL audit entry
- [ ] Updating produces an UPDATE entry with before/after
- [ ] Deleting produces a DELETE entry
- [ ] Audit logs show the correct actor username
- [ ] Different entities have separate audit trails

---

## 6. Testing & Verification Checklist

### Quick Demo Walkthrough (10 minutes)

| # | Step | Expected Outcome | ✓ |
|---|------|------------------|---|
| 1 | Launch frontend, connect to demo backend | Login screen appears | ☐ |
| 2 | Login as admin / password | Dashboard with full sidebar | ☐ |
| 3 | Create a State | State appears in table | ☐ |
| 4 | Create a Municipality (linked to State) | Municipality appears with state reference | ☐ |
| 5 | Create a Parish (linked to Municipality) | Parish appears with municipality reference | ☐ |
| 6 | Create a Branch (with form combos) | Branch appears, auto-creates departments | ☐ |
| 7 | Create a Department linked to branch | Department appears | ☐ |
| 8 | Create a Category | Category appears | ☐ |
| 9 | Create an Item (select branch, dept, category) | Item appears with correct data | ☐ |
| 10 | Create a Bag with expected items | Bag appears, barcode is unique | ☐ |
| 11 | Displace an item | Displacement shows ACTIVE status | ☐ |
| 12 | View audit logs for created state | Shows INITIAL entry with timestamp | ☐ |

### Comprehensive Testing (per feature, see individual sections above)

- [ ] All CRUD operations work for every entity
- [ ] Cascade protection (can't delete parent with children)
- [ ] Soft-delete (deleted items disappear from queries)
- [ ] JWT authentication (expired/invalid tokens rejected)
- [ ] RBAC (unpermitted actions return 403)
- [ ] Unique constraints (duplicate name/barcode are rejected)
- [ ] Validation (empty required fields are rejected)
- [ ] Settings panel (API URL change + language toggle)
- [ ] i18n (switch language between en/es)

### Known Issues (pre-existing backend bugs)

The following operations fail via the REST API with a JaVers `LazyInitializationException`:
- Creating items, bags, users, roles, displacements with entity relationships
- This affects creating items directly (the form will show a 500 error)
- **Workaround:** Create entities through the API client with proper transaction management, or use the pre-seeded demo data

---

## Appendix: API Reference

### Authentication
```http
POST /api/auth/login      {"username":"admin","password":"password"} → {"token":"..."}
GET  /api/auth/me          → {"id":1, "username":"admin", "roles":[...], "permissions":[...]}
GET  /api/auth/validate    → 200 OK (if token valid)
```

### CRUD Endpoints (all require JWT + permissions)

| Resource | Endpoint | Pagination |
|----------|----------|------------|
| States | `/api/states` | `?page=1&pageSize=100` |
| Municipalities | `/api/municipalities` | `?page=1&pageSize=100&stateId=X` |
| Parishes | `/api/parishes` | `?page=1&pageSize=100&municipalityId=X` |
| Branches | `/api/branches` | `?page=1&pageSize=100&stateId=X&municipalityId=X` |
| Departments | `/api/departments` | `?page=1&pageSize=100&branchId=X` |
| Categories | `/api/categories` | Standard |
| Items | `/api/items` | `?page=1&pageSize=100&branchId=X&categoryId=X` |
| Bags | `/api/bags` | `?page=1&pageSize=100&branchId=X` |
| Displacements | `/api/displacements` | Standard |
| Item Requests | `/api/item-requests` | `?page=1&pageSize=100` |
| Users | `/api/users` | Standard |
| Roles | `/api/roles` | Standard |
| Permissions | `/api/permissions` | Standard |
| Audit Logs | `/api/audit-logs/{entityName}` | `?page=1&pageSize=100` |

Each CRUD endpoint supports: `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}`.
List endpoints return: `{"data": [...], "total": N, "totalPages": N, "currentPage": N}`.

### Special Endpoints

```http
GET  /api/bags/barcode/{code}                    → Bag by barcode
GET  /api/bags/{id}/audit                         → Bag audit with item quantities
POST /api/displacements/{id}/resolve              → Resolve a displacement
POST /api/item-requests/{id}/submit               → DRAFT → PENDING_REVIEW
POST /api/item-requests/{id}/review               → PENDING_REVIEW → APPROVED/REJECTED
POST /api/item-requests/{id}/execute               → APPROVED → EXECUTED
```

### Response Format

Success: `{ "id": 1, "name": "...", ... }` for single entities
Error: `{ "backendError": "...", "path": "...", "error": "...", "status": 500 }`
