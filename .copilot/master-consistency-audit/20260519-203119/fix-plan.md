## Objectives
- Make login click deterministic.
- Make auth failure visible.
- Cover login path with interaction test.
- Stop stale-client drift after settings change.

## Ordered Fix Tasks
1. Extract login submit path into testable method or callback boundary.
2. Rebuild `ViewContext` and dependent views when `ApiClient` changes in settings.
3. Add TestFX or headless FX test for `LoginView` button fire.
4. Add MockWebServer auth test for `/api/auth/login` + dashboard transition.
5. Remove silent dashboard fallback or convert it to explicit error surfacing.
6. Add CI job step that runs the login interaction test.

## Test Plan
- Unit: `ApiClient.login()` request/response handling.
- UI: `LoginView` click fires auth request and calls dashboard callback on 200.
- UI: 401 shows error state and does not transition.
- Regression: settings save then login uses new base URL.
- Gate: `mvn clean verify` plus the new UI auth test class.

## Focused Regression Shields
- Assert one POST to `/api/auth/login` per click.
- Assert no silent stay-on-login after 200.
- Assert error path leaves a visible failure signal.
- Assert settings update refreshes client reference everywhere.
