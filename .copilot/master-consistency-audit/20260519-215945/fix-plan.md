## Objectives
1. Catch JavaFX login→dashboard transition failures in CI before merge.
2. Enforce scene-root ownership invariant in desktop UI.
3. Reduce silent failure paths (UI + auth filter) that mask regressions.
4. Add critical-path tests for RBAC/nav consistency and null-safe payload handling.

## Ordered Fix Tasks
1. **Create PR: frontend JavaFX regression harness**
   - Add headless JavaFX test setup (Monocle/TestFX) for CI.
   - Wire Surefire/system properties for headless run.

2. **Create PR: focused transition tests**
   - Add `DesktopUiSceneTransitionTest` (new) that:
     - boots JavaFX `Stage`,
     - creates `DesktopUi(stage, fakeApi, config)`,
     - triggers successful login callback path,
     - asserts no `IllegalArgumentException` and dashboard UI visible.
   - Add explicit test for “same root not rebound to second scene”.

3. **Create PR: scene management correction**
   - Update `DesktopUi.showDashboard()` to avoid creating new `Scene` with existing root.
   - Keep single `Scene` lifecycle; mutate layout regions only.

4. **Create PR: guardrails for weak null/shape handling**
   - Introduce shared safe extractors for id/string conversions in frontend views.
   - Replace highest-risk cast/toString callsites first (`DesktopUi`, `UIUtils`, `FormView` hot paths).

5. **Create PR: observability hardening**
   - Replace ignored catches with logging in:
     - `JwtAuthenticationFilter`
     - `ResourceView` preload thread
     - `UIUtils.parseErrorReport`
   - Keep behavior, but stop silent suppression.

6. **Create PR: RBAC/UI contract protection**
   - Add test mapping backend authority sets to frontend nav visibility contract.
   - Decide policy: “full admin bundle only” vs “per-feature visibility by authority”; encode in tests.

## Focused Regression Shields
- **Shield A (must-have):** UI transition integration test exercising real `Stage` + login success + dashboard render.
- **Shield B (must-have):** Scene root ownership invariant test fails on any attempt to bind same root to new scene.
- **Shield C (must-have):** CI profile running JavaFX headless tests on every PR.
- **Shield D (should-have):** test asserting dashboard callback completion only counts as success when dashboard scene/regions applied, not just runnable invocation.

## Test Plan
1. Run focused frontend regression suite:
   - `mvn -pl frontend -Dtest=DesktopUiSceneTransitionTest,LoginInteractionTest,FrontendStartupSmokeTest test`
2. Run frontend full tests:
   - `mvn -pl frontend test`
3. Run full repo verify:
   - `mvn clean verify`
4. For RBAC consistency PR, add/execute contract tests covering representative authority subsets and expected nav visibility.
