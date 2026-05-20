## Verdict
High risk. Login path has no interaction coverage, no visible failure contract, and one downstream exception sink that can make click look inert while CI still stays green.

## Findings
- `LoginView` wires button click to `apiClient.login(...)` then `dashboardShower().run()`, but any exception is caught and only sent to `UIUtils.showErrorPopup(...)` (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/LoginView.java:49-56`).
- `DesktopUi` hides `showDashboard()` failures by catching `Exception` and falling back to `showLogin()` with no surfaced error (`frontend/src/main/java/com/inventorymanager/frontend/ui/DesktopUi.java:60-67,94-103`).
- That means success/failure is not externally observable unless the dashboard render fully succeeds. A bad token, bad auth response, or layout/runtime fault can leave user on login screen with no durable signal.
- Existing tests miss the path. `ApiClientTest` only covers `list()` and JSON parsing in `me()` (`frontend/src/test/java/com/inventorymanager/frontend/api/ApiClientTest.java:32-77`). `DesktopUiAuthorizationTest` only checks `computeIsAdmin()` (`frontend/src/test/java/com/inventorymanager/frontend/ui/DesktopUiAuthorizationTest.java:35-51`). `FrontendStartupSmokeTest` only checks constructor NPEs and field init, not button click or scene transition (`frontend/src/test/java/com/inventorymanager/frontend/ui/FrontendStartupSmokeTest.java:33-52`).
- CI gate is just `mvn clean verify` (`.github/workflows/ci.yml:71-72`). No TestFX/UI event test, no MockWebServer login flow, no assertion that clicking `Sign In` sends `/api/auth/login`.

## Focused Root Cause Path
`LoginView` click handler is the only auth entry point (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/LoginView.java:41-56`). It depends on `ApiClient.login()` for network I/O (`frontend/src/main/java/com/inventorymanager/frontend/api/ApiClient.java:29-46`) and on `DesktopUi.dashboardShower()` for visible state change (`frontend/src/main/java/com/inventorymanager/frontend/ui/DesktopUi.java:60-67`). `DesktopUi` swallows dashboard errors and returns to login, so the button can fail with no obvious UI state change. No test ever fires that button, so this can ship green.

## High-Risk Pitfalls
- No test for button fire, auth POST, or dashboard transition.
- Generic catch in login path hides root cause.
- Async popup via `Platform.runLater` can vanish in headless or broken FX state.
- `showSettingsPopup()` swaps `this.apiClient` but does not rebuild `viewContext`, so already-built views can keep stale client refs.
- `DesktopUi` hard-casts `permissions` payload and `computeIsAdmin()` is rigid; future auth schema drift can break post-login navigation with no compile signal.

## Suggested Fixes
- Add a real login interaction test: fire `Sign In`, assert POST to `/api/auth/login`, assert dashboard callback runs once on 200.
- Add negative-path test: 401 returns visible error state, not silent return.
- Remove silent `showDashboard()` fallback or at least log and surface it.
- Rebuild `ViewContext` after settings changes, or make it read current `ApiClient` dynamically.
- Add a UI smoke gate in CI that exercises login click under headless FX.
