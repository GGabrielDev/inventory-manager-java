## Objectives

- Cover real auth, not just mocked controller state.
- Lock backend/frontend contract shape around `/api/auth/me` and login flow.
- Surface UI loader failures instead of swallowing them.
- Add a build-level coverage gate so missing critical-path tests stop merging.

## Ordered Fix Tasks

1. **Add backend auth integration tests.** Use a Spring test with the real security chain for `/api/auth/login`, `/api/auth/me`, and `/api/auth/validate`. Verify bad credentials, missing user, and unauthorized requests fail the right way.

2. **Add frontend contract tests around login/dashboard.** Extend `ApiClientTest` for 4xx/5xx login failure and token reuse. Add `DesktopUi` tests that feed `me()` payloads with missing/null/typed-wrong `permissions` and assert the UI fails loudly, not with a hidden NPE.

3. **Cover the silent loader branches.** Add tests for `FormView` branch/department loading failures and for edit/create bag payload handling when dependent fetches fail or return empty.

4. **Reduce standalone controller-only coverage where it matters.** Keep fast unit tests, but add one full-context test per critical slice so `SecurityConfig`, validation, advice, and Jackson behavior are exercised together.

5. **Add a coverage floor in CI.** Wire JaCoCo into backend and frontend modules and fail on meaningful loss of line/branch coverage for auth, UI login, and controller/service packages.

## Test Plan

- Backend: `AuthController` login success, bad password, missing user row, real JWT `/me`, unauthorized `/me`, and `/validate`.
- Backend: one `@SpringBootTest` or `@WebMvcTest` that proves security chain is active, not bypassed by standalone setup.
- Frontend: `ApiClient.login()` returns exception on 401/500 and still stores token only on 200.
- Frontend: `DesktopUi.showDashboard()` handles absent `permissions` deterministically; admin nav only appears when payload matches.
- Frontend: `LoginView.performLogin()` blank input stays local and failed login re-enables UI state.
- UI form: branch/department fetch failures produce visible error state, not swallowed exceptions.

## Focused Regression Shields

- Guard the auth boundary with a real JWT round-trip test, not a mocked principal-only test.
- Guard the UI contract with payload-shape tests for `/api/auth/me`; null or non-list `permissions` must fail in test, not at runtime.
- Guard async UI loaders with explicit failure assertions so `catch (Exception ignored)` cannot hide regressions.
- Guard CI with coverage thresholds so future missing-path gaps are visible at merge time.
