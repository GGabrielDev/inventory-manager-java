## Objectives
1. Stop Municipality edit 500 (`LazyInitializationException`) in prod config.
2. Make backend 500 payload actionable for debugging without leaking sensitive data.
3. Add regression shields so CI fails on same class of lazy/audit/contract breakage.
4. Align transactional + invariant handling across geo hierarchy modules.

## Ordered Fix Tasks
1. **Critical: stabilize municipality upsert transaction path**
   - Move municipality write logic to service method annotated `@Transactional` (preferred) or add `@Transactional` on controller write endpoints.
   - Keep `save + audit commit + response mapping` in same transaction scope.
   - Mirror same fix for parish and department write flows to remove pattern drift.

2. **Critical: remove lazy entity response fragility**
   - Replace direct entity return from write endpoints with DTO projection (`id`, `name`, `state{id,name}` for municipality; similar for parish/department).
   - Ensure mapper reads needed relation fields while transaction active.

3. **High: enrich global exception response contract**
   - Extend `GlobalExceptionHandler` unexpected branch to emit:
     - `backendError` (class),
     - `details` (safe root-cause message / most-specific cause),
     - `path`, `method`, `requestId`, `timestamp`.
   - Keep stable user-facing `message`; gate detailed internals by profile/env flag.
   - Update frontend expectation docs/tests for `details` usage (already parsed in UI).

4. **High: add cross-module consistency guard**
   - Introduce shared write-pattern helper or architecture test: controllers/services doing `save + audit` must run transactionally.
   - Add code review checklist item: no direct entity return for LAZY-heavy aggregates.

5. **Medium: tighten CI quality gates**
   - Raise JaCoCo minimum from `0.05` to meaningful threshold by module (phase-in if needed).
   - Add targeted test suite execution marker for geo hierarchy controller updates.

## Test Plan
1. **Municipality update integration tests**
   - `PUT /api/municipalities/{id}` with valid payload + `edit_municipality` authority returns 200 and expected JSON shape.
   - Same path with missing authority returns 403.
   - Same path with nonexistent state returns 400 `State not found`.

2. **Lazy regression tests (prod-like config)**
   - Test profile override `spring.jpa.open-in-view=false`.
   - Execute municipality/parish/department update and assert no `LazyInitializationException`.
   - Verify audit commit still recorded.

3. **Global 500 contract tests**
   - Force runtime exception from test controller; assert payload has `status`, `message`, `backendError`, `details`, `path`, `requestId`.
   - Assert user-safe message remains stable; details policy differs by env if configured.

4. **Cross-module contract tests**
   - Parameterized test for geo resources (`state`, `municipality`, `parish`, `department`) CRUD write paths:
     - audit commit called,
     - response serializable under `open-in-view=false`,
     - no detached lazy proxy leakage.

## Focused Regression Shields
- Add dedicated `MunicipalityControllerIntegrationTest` covering `PUT` edit path with real JPA + audit enabled + `open-in-view=false`.
- Add `GlobalExceptionHandlerIntegrationTest` validating enhanced 500 payload fields and frontend-consumable `details`.
- Add static/arch test rule: methods calling `auditService.commit*` must be under transactional boundary.
- Add CI job step that runs focused suite for municipality/parish/department update regressions before full verify.
