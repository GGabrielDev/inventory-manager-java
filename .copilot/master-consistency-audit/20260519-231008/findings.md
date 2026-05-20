## Verdict
Master has real consistency gap in edit flows with lazy relations + audit commit timing. Municipality edit 500 is credible and likely reproducible in prod profile. Global 500 payload too masked for fast triage. Test/build gates miss this path.

## Findings
1. **Focused bug: Municipality edit can hit `LazyInitializationException`**
   - `MunicipalityController.update` does `findById -> setState -> save -> auditService.commitUpdate -> return saved` without transaction boundary (`backend/src/main/java/com/inventorymanager/backend/web/MunicipalityController.java:70-82`).
   - Entity relation is LAZY: `Municipality.state` (`backend/src/main/java/com/inventorymanager/backend/domain/Municipality.java:26-29`).
   - Prod disables Open Session In View: `spring.jpa.open-in-view: false` (`backend/src/main/resources/application.yml:14`).
   - Audit commit inspects entity graph (`javers.commit`) (`backend/src/main/java/com/inventorymanager/backend/audit/AuditService.java:45-47`), so detached lazy state/proxy can blow up during commit/serialization.

2. **Cross-module inconsistency: Branch edit protected, Municipality/Parish/Department not**
   - Branch controller wraps create/update/delete in `@Transactional` (`backend/src/main/java/com/inventorymanager/backend/web/BranchController.java:77,102,113`).
   - Municipality/Parish/Department perform same audit-after-save pattern, but no `@Transactional` (`backend/src/main/java/com/inventorymanager/backend/web/MunicipalityController.java:57-91`, `.../ParishController.java:57-91`, `.../DepartmentController.java:55-85`).
   - Same architectural pattern, different safety level => fragile behavior drift by module.

3. **Global 500 handler gives weak debug signal**
   - Unexpected errors return fixed generic message + only exception class name (`backend/src/main/java/com/inventorymanager/backend/common/GlobalExceptionHandler.java:46-50,23`).
   - Frontend already supports richer `details` field parsing (`frontend/src/main/java/com/inventorymanager/frontend/ui/UIUtils.java:46-51`), but backend never emits `details`. Debug context lost.

4. **Domain invariant gap around municipality/parish hierarchy outside Branch flow**
   - Branch update validates `municipality belongs to state` and `parish belongs to municipality` (`backend/src/main/java/com/inventorymanager/backend/web/BranchController.java:133-141`).
   - Municipality and Parish update paths only check FK existence (`MunicipalityController.java:75-78`, `ParishController.java:75-78`) and rely on DB FK, not explicit hierarchical/business guards in controller/service.

5. **RBAC/test coverage gap on critical edit path**
   - No dedicated Municipality controller tests found (no `*Municipality*Test.java` in backend tests).
   - Existing broad tests only create municipality (`POST`), not edit municipality (`PUT`) (`backend/src/test/java/com/inventorymanager/backend/FunctionalApiTest.java:47-54`, `.../AcceptanceScenarioTest.java:44-47`).
   - No test asserts authority `edit_municipality`; current tests only include `create_municipality` (`FunctionalApiTest.java:36`, `AcceptanceScenarioTest.java:37`).

6. **Why this passed build gates**
   - CI only runs `mvn clean verify` with no targeted lazy-loading regression suite (`.github/workflows/ci.yml:74-75`).
   - Coverage gate is extremely low (`LINE >= 5%`) (`backend/pom.xml:159-162`), so missing municipality update tests do not fail pipeline.
   - Test profile does not set `open-in-view: false` (`backend/src/test/resources/application-test.yml:1-15`), while prod sets false (`backend/src/main/resources/application.yml:14`), so lazy-load behavior can differ between test/prod.
   - Multiple integration tests run with class-level `@Transactional` (`FunctionalApiTest.java:26`, `AcceptanceScenarioTest.java:27`), which can keep persistence context alive and mask detached lazy failures.

## High-Risk Pitfalls
- Same lazy/audit failure pattern likely in `ParishController.update` and `DepartmentController.update` (LAZY relation + audit commit outside explicit transaction).
- Generic 500 payload blocks incident triage: operators see class name only, no root-cause message/correlation/path.
- UI shows “Failed” popup while backend omits actionable details, increasing MTTR and repeat user retries.
- Guardrails depend on `BranchController` only; other modules may violate hierarchical consistency policy by omission.

## Suggested Fixes
1. Add transactional boundary to municipality/parish/department write endpoints (or service-layer transactional upsert) to keep lazy graph attached through audit commit and response mapping.
2. Avoid returning managed entities directly on writes; map to response DTO with explicit scalar fields + relation IDs/names to kill lazy-serialization risk class-wide.
3. Harden global exception payload for 500:
   - include `backendError`, `details` (root message in non-prod or guarded), `path`, `method`, `requestId`, `timestamp`.
   - keep user-safe top `message`; put sensitive stack only behind profile flag.
4. Add focused tests for municipality update:
   - success path with `edit_municipality`;
   - lazy regression with `open-in-view=false`;
   - 500 payload contract includes `backendError` + `details` for unexpected exception mapping.
5. Raise coverage/quality guardrail for controllers handling hierarchical entities; add module-level minimum tests per CRUD verb on geo hierarchy resources.

## Focused Root Cause Path
`PUT /api/municipalities/{id}` (`MunicipalityController.update`) loads entity with LAZY `state`, saves, then calls `auditService.commitUpdate(saved)` outside explicit transaction. With prod `open-in-view=false`, Hibernate session closes before all lazy graph access paths complete. JaVers commit (or later serialization path) touches lazy relation/proxy and throws `LazyInitializationException`; `GlobalExceptionHandler.handleUnexpected` then returns generic 500 with masked message, hiding root cause from caller.
