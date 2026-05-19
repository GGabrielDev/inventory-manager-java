## Verdict

Master is not consistency-safe yet. The biggest risks are nested-bag updates silently deleting data, a missing branch/department invariant, and workflow/RBAC contracts that differ between UI, controllers, and service logic.

## Findings

1. **Bag edits wipe configured expected items.** `frontend/src/main/java/com/inventorymanager/frontend/ui/views/FormView.java:692-698` always sends `"expectedItems", List.of()` on bag save, and `backend/src/main/java/com/inventorymanager/backend/web/BagController.java:170-186` treats any non-null list as a full replacement. Editing an existing bag through the UI therefore clears all expected items even when the operator only changes name/barcode metadata.

2. **Bag branch/department relationships are not validated together.** `backend/src/main/java/com/inventorymanager/backend/web/BagController.java:165-168` accepts any `assignedDepartmentId` for any `branchId`. The frontend bag form filters departments by branch (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/FormView.java:651-669, 688-697`), so the API can still create cross-branch bag assignments that the UI assumes cannot exist. That breaks the branch-scoped inventory invariant used by audit and browsing screens.

3. **Item-request review status handling is too permissive and too lossy.** `backend/src/main/java/com/inventorymanager/backend/web/ItemRequestController.java:95-103` turns every decision other than `"approve"` into `REJECTED`, and `backend/src/main/java/com/inventorymanager/backend/service/ItemRequestWorkflowService.java:57-71` accepts any `nextStatus` without whitelisting allowed transitions. A typoed review body becomes a silent rejection, and internal callers can set impossible statuses.

4. **Frontend RBAC is keyed off role name, not the permission model.** `frontend/src/main/java/com/inventorymanager/frontend/ui/DesktopUi.java:66-73, 102-120` decides admin access with `roles.contains("admin")`, while backend enforcement is permission-based (`backend/src/main/java/com/inventorymanager/backend/config/SecurityConfig.java:47-52` and controller `@PreAuthorize` checks). That means a permission-equivalent non-`admin` role loses admin UI, and an `admin`-named role without the right authorities still shows admin navigation.

5. **Audit route naming is split across modules.** Backend exposes both `/api/changelogs/...` and `/api/audit-logs/...` (`backend/src/main/java/com/inventorymanager/backend/audit/AuditController.java:11-30`, `backend/src/main/java/com/inventorymanager/backend/audit/AuditLogController.java:7-27`), the desktop UI calls `/api/audit-logs/...` (`frontend/src/main/java/com/inventorymanager/frontend/ui/views/AuditView.java:48-63`), and repo docs still advertise `/api/changelogs/...` (`backend/README.md:26-34`, `docs/ITEM-REQUEST-WORKFLOW.md:56-80`, `docs/MIGRATION-NOTES.md:8-12`). The contract is functionally duplicated but not canonical.

## High-Risk Pitfalls

- `backend/src/main/java/com/inventorymanager/backend/web/CrudRequest.java:86-92` only requires `entries` to be non-null, so an empty item-request payload can still be approved/executed with zero inventory effect.
- There is no regression test proving bag updates preserve nested state when expected items are intentionally edited, or that the bag API rejects branch/department mismatches.
- There is no end-to-end contract test tying the desktop audit screen to a single backend audit route.

## Suggested Fixes

- Make bag update semantics explicit: either preserve `expectedItems` unless the client sends a sentinel clear action, or require full replacement and make the UI send the intended current list.
- Enforce `assignedDepartment.branch_id == branchId` in `BagController` before save.
- Validate item-request review decisions against an allowed set, and make the service reject illegal `nextStatus` values instead of trusting callers.
- Switch frontend admin visibility to `permissions`/authorities, not role name.
- Pick one canonical audit route and make the other a temporary compatibility alias with tests, or remove the alias after updating UI/docs.
