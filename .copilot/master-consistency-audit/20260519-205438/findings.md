## Verdict

High risk. Tests are green, but they are narrow and uneven: backend auth/security paths are mostly bypassed, frontend contract paths are only smoke-tested, and CI has no coverage floor. Green build does not mean critical paths are covered.

## Findings

- `backend/src/test/java/com/inventorymanager/backend/auth/AuthControllerTest.java:29-76` only checks `/api/auth/me` with a custom principal resolver. It never exercises `/api/auth/login` or `/api/auth/validate`, and it builds the controller with `null` auth manager, so the real `SecurityConfig` / `JwtAuthenticationFilter` path is not under test (`backend/src/main/java/com/inventorymanager/backend/config/SecurityConfig.java:42-53`, `backend/src/main/java/com/inventorymanager/backend/auth/JwtAuthenticationFilter.java:25-48`). A broken auth manager, bad JWT claim, or filter bug can still pass because the test skips the wiring that uses it.

- Frontend auth/state mapping is fragile and under-tested. `frontend/src/main/java/com/inventorymanager/frontend/ui/DesktopUi.java:101-110` hard-casts `currentUser.get("permissions")` to `List<String>` and assumes the backend payload shape is stable. `frontend/src/main/java/com/inventorymanager/frontend/ui/views/LoginView.java:84-103` only covers blank input and then hides async failures behind UI callbacks. Existing tests only probe `performLogin()` and `computeIsAdmin()` with idealized data (`frontend/src/test/java/com/inventorymanager/frontend/ui/views/LoginInteractionTest.java:50-101`, `frontend/src/test/java/com/inventorymanager/frontend/ui/DesktopUiAuthorizationTest.java:35-51`), so null permissions, schema drift, or dashboard-load failures can ship green.

- `frontend/src/main/java/com/inventorymanager/frontend/ui/views/FormView.java:651-693` swallows loader exceptions for branch/department selection with `catch (Exception ignored) {}`. No test covers that branch, so a backend 404/500 or payload mismatch can make the form look inert instead of failing loudly.

- CI only runs `mvn clean verify` (`.github/workflows/ci.yml:71-72`), and the POMs only configure compile/test packaging plugins (`backend/pom.xml:111-125`, `frontend/pom.xml:90-142`). There is no JaCoCo, no minimum coverage threshold, and no cross-module contract gate. That is why these gaps can stay red in reality and still pass build.

## High-Risk Pitfalls

- Most controller tests use `MockMvcBuilders.standaloneSetup(...)`; security, validation, exception handlers, and full Jackson wiring stay out of scope.
- No test asserts `/api/auth/login` success/fail, `/api/auth/me` with a real JWT, or unauthorized access through the filter chain.
- No frontend test asserts `showDashboard()` with missing/null `permissions`, or `ApiClient` 4xx/5xx handling.
- Silent catches in UI loaders hide backend regressions instead of surfacing them.
- No coverage floor means untouched branches can merge indefinitely.

## Focused Root Cause Path

`AuthController.login()` depends on the real auth manager, repo lookup, and JWT creation (`backend/src/main/java/com/inventorymanager/backend/auth/AuthController.java:41-52`). The only auth controller test replaces that with a mock principal and never hits login wiring (`backend/src/test/java/com/inventorymanager/backend/auth/AuthControllerTest.java:29-76`). On the frontend, `LoginView.performLogin()` calls `ApiClient.login()` and then `DesktopUi.dashboardShower()` (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/LoginView.java:84-103`), while `DesktopUi.showDashboard()` assumes a stable `permissions` payload (`frontend/src/main/java/com/inventorymanager/frontend/ui/DesktopUi.java:101-110`). If the backend payload, filter chain, or JWT path breaks, current tests still pass because they stop before the real integration boundary.

## Suggested Fixes

- Add real auth coverage: `/api/auth/login` success/fail, `/api/auth/me` with a valid JWT, `/api/auth/validate`, and unauthorized `/me`.
- Add frontend contract coverage: `ApiClient.login()` error handling, `DesktopUi.showDashboard()` with null/missing/extra `permissions`, and login blank-input behavior.
- Replace at least one controller slice per critical area with a full Spring test that loads security and advice.
- Fail the build on coverage loss with JaCoCo floor for backend and frontend modules.
