## Objectives

- Eliminate the frontend startup NPE and guarantee JAR launch safety.
- Align frontend RBAC visibility with backend authority enforcement.
- Enforce domain invariants at backend boundaries (location hierarchy and transfer consistency).
- Add guardrails so release/build gates fail on these regressions.

## Ordered Fix Tasks

1. **Stabilize DesktopUi initialization lifecycle (P0).**  
   - Ensure a root `Scene`/`contentArea` exists before any call to `setView`.  
   - Add an explicit initialization path for login-first flow (no dependence on `showDashboard` side effects).  
   - Add a defensive assertion/guard in `setView` with actionable message.

2. **Add frontend launch regression tests (P0).**  
   - Add JavaFX startup smoke test that executes `InventoryManagerDesktopApplication.start` and validates no exception during `showLogin`.  
   - Refactor `DesktopUiAuthorizationTest` to stop swallowing unexpected exceptions and to assert deterministic UI state behavior.

3. **Harden CI/release guardrails for frontend runtime (P0).**  
   - Add a CI job step for frontend launch smoke test in headless mode (Xvfb/Monocle as needed).  
   - Release workflow must run at least the frontend smoke test before packaging/upload.

4. **Enforce location hierarchy invariants in backend branch upsert (P1).**  
   - In `BranchController.mapRequestToEntity`, validate:
     - selected municipality belongs to selected state,
     - selected parish belongs to selected municipality.  
   - Return `400` with precise invariant error messages.

5. **Enforce transfer branch-department coherence (P1).**  
   - In `ItemRequestWorkflowService.executeTransfer`, validate that final department belongs to final branch.  
   - Reject inconsistent transfer payloads instead of persisting split branch/department references.

6. **Align UI RBAC to backend authorities (P1).**  
   - Replace one-shot admin bundle gate with per-feature permission checks tied to each nav entry.  
   - Keep visibility logic in one helper to avoid future drift.

7. **Add security-aware diagnostics endpoint tests (P2).**  
   - Replace standalone-only tests for `TestController` with security-enabled tests covering unauthenticated and authenticated access.

## Focused Regression Shields

1. **Startup path shield:** automated test for `Application.start -> DesktopUi.showLogin -> LoginView.show -> setView` to assert no NPE and non-null scene root.
2. **Lifecycle contract shield:** unit test that `setView` before initialization fails with explicit invariant error (or auto-initializes by design), never raw NPE.
3. **Build gate shield:** CI required check named for frontend launch smoke; release workflow must depend on it (no `-DskipTests` bypass for runtime-critical smoke).
4. **Test-quality shield:** prohibit broad exception swallowing in UI tests; enforce fail-on-unexpected-exception pattern.

## Test Plan

1. **Frontend critical-path tests**
   - Startup smoke test for `InventoryManagerDesktopApplication`.
   - `DesktopUi` lifecycle tests for login-first rendering and dashboard-after-login rendering.
   - RBAC nav visibility matrix test (permission combinations).

2. **Backend invariant tests**
   - `BranchController` tests for invalid state/municipality/parish chains (expect 400).  
   - `ItemRequestWorkflowService` transfer tests for cross-branch department mismatch rejection.

3. **Security tests**
   - `TestController` auth matrix:
     - unauthenticated request denied,
     - authenticated request allowed.

4. **End-to-end consistency checks**
   - API + UI contract test that users with precise authorities can see and use matching UI routes without requiring unrelated permissions.
