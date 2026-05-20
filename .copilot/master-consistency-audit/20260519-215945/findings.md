## Verdict
Pipeline green, but frontend transition path not exercised. `DesktopUi.showDashboard()` can throw JavaFX runtime `IllegalArgumentException` and still pass CI because current tests avoid real `Stage`/`Scene` lifecycle and do not assert post-login scene transition behavior.

## Findings
1. **Focused escape path: scene-root ownership violation not tested**
   - `DesktopUi` sets `mainLayout` as root in constructor (`frontend/src/main/java/com/inventorymanager/frontend/ui/DesktopUi.java:56`).
   - `showDashboard()` sets **new** `Scene(mainLayout, ...)` again (`DesktopUi.java:197`), which triggers `IllegalArgumentException` when same root already bound to prior scene.
   - No test executes this exact transition with non-null `Stage`.

2. **Current tests validate stubs, not runtime transition**
   - `LoginInteractionTest` only checks `dashboardShower` callback fired (`frontend/src/test/java/com/inventorymanager/frontend/ui/views/LoginInteractionTest.java:51-71`), but uses mocked `ViewContext` runnable (`:56-62`), never instantiates `DesktopUi`, never calls `showDashboard()`, never touches `Stage`.
   - `FrontendStartupSmokeTest` explicitly avoids JavaFX platform startup (`frontend/src/test/java/com/inventorymanager/frontend/ui/FrontendStartupSmokeTest.java:17-19`) and passes `null` stage (`:39-40`), so all scene operations are skipped.

3. **CI gate has no JavaFX transition guardrail**
   - CI runs `mvn clean verify` only (`.github/workflows/ci.yml:71-72`).
   - TestFX deps exist (`frontend/pom.xml:77-87`) but no TestFX/App lifecycle tests in repo; no Monocle/headless JavaFX config in workflow or Surefire args.

4. **RBAC cross-module consistency risk**
   - Frontend admin UI gate requires hardcoded **all-permission set** in `computeIsAdmin()` (`DesktopUi.java:91-99`), while backend access is endpoint-authority driven (`backend/src/main/java/com/inventorymanager/backend/web/*Controller.java`, `@PreAuthorize("hasAuthority(...)")`).
   - Effect: user can be authorized for many admin endpoints server-side but lose admin navigation client-side if one listed permission missing.

5. **Fragile null/shape handling in frontend data maps**
   - Repeated casts like `((Number) map.get("id")).longValue()` and `.toString()` on nullable values (`DesktopUi.java:252,268-291`, `UIUtils.java:138`, `FormView.java` multiple sites) can crash on partial backend payloads.

6. **Silent exception handling weakens diagnosability**
   - `JwtAuthenticationFilter` swallows all token parse/load failures (`backend/src/main/java/com/inventorymanager/backend/auth/JwtAuthenticationFilter.java:45-47`).
   - Frontend also has ignored exceptions in data-preload/error parsing (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/ResourceView.java:101`, `frontend/src/main/java/com/inventorymanager/frontend/ui/UIUtils.java:52`).

## Focused Root Cause Path
1. App starts with `DesktopUi(stage, ...)` and constructor binds `mainLayout` into scene A (`DesktopUi.java:56`).
2. Login succeeds in `LoginView.performLogin()` and dispatches `dashboardShower` runnable (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/LoginView.java:93-95`).
3. Runnable in `DesktopUi.initViewContext()` calls `showDashboard()` (`DesktopUi.java:64-67`).
4. `showDashboard()` attempts `stage.setScene(new Scene(mainLayout, ...))` (`DesktopUi.java:197`) while `mainLayout` still root of scene A.
5. JavaFX throws `IllegalArgumentException: BorderPane is already set as root of another scene`.
6. Existing tests miss this because neither `LoginInteractionTest` nor `FrontendStartupSmokeTest` executes steps 1+4 with real stage lifecycle.

## High-Risk Pitfalls
- **Transition correctness blind spot**: no assertion around login→dashboard scene mutation on real JavaFX thread.
- **UI/backend authority drift**: client “admin” is hardcoded permission bundle, not backend canonical role/authority contract.
- **Map payload fragility**: many unchecked casts can convert minor payload drift into UI hard crash.
- **Silent catches**: ignored exceptions hide security/diagnostic signals, making production issues harder to detect and triage.

## Suggested Fixes
1. Add JavaFX lifecycle regression test that starts real `Stage`, performs login success flow, and asserts dashboard transition does not throw.
2. Add scene-root ownership invariant test: no path should construct a second `Scene` with a root already attached.
3. Refactor `DesktopUi` to reuse existing scene and only replace regions (`setLeft/setTop/center view`), or clear old root ownership before reassignment.
4. Add guard helper for frontend map extraction (`safeLong`, `safeString`) and cover with unit tests for null/missing keys.
5. Add explicit logging for JWT parse failures and frontend ignored-catch sites.
6. Add contract test aligning backend authorities and frontend navigation visibility logic.
