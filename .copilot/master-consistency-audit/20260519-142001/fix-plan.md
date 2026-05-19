## Objectives

- Keep bag updates from erasing nested expected items unintentionally.
- Enforce branch/department and workflow state invariants at the backend boundary.
- Make the desktop UI follow the same authorization model as the backend.
- Collapse audit access onto one stable route contract.

## Ordered Fix Tasks

1. **Harden bag save/update semantics.** Decide whether `expectedItems` is replace-only or patchable, then make the backend and `FormView` send the same contract. Add a branch-vs-department check in `BagController` before save.

2. **Tighten item-request validation.** Reject empty `entries`, validate review `decision` against `approve|reject`, and make `ItemRequestWorkflowService.reviewRequest` enforce allowed status transitions instead of accepting arbitrary `ItemRequestStatus` values.

3. **Align frontend RBAC with authorities.** Replace the `roles.contains("admin")` gate in `DesktopUi` with a permission-based check from `/auth/me` so UI visibility matches `@PreAuthorize`.

4. **Normalize audit routing.** Choose `/api/audit-logs/...` or `/api/changelogs/...` as canonical, update the other path to a compatibility shim if needed, and make docs/UI/tests point to one route.

5. **Add regression coverage for the boundary cases above.** Keep tests close to the invariants so future UI or API refactors cannot reintroduce the drift.

## Test Plan

- Backend controller test: bag update preserves or clears `expectedItems` exactly as defined by the chosen contract, and rejects a department from a different branch.
- Backend workflow test: empty item-request entries fail, invalid review decisions fail, and illegal status transitions are rejected.
- Frontend test: admin navigation is shown from permission data, not just the literal `admin` role name.
- Audit contract test: the desktop audit view and backend route resolve the same canonical endpoint; keep only one path in the happy path.
