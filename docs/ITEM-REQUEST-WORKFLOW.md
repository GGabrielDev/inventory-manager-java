# Item Request Workflow (Operator-driven inventory changes)

## Goal

Only callers with the matching item permissions can directly mutate `/api/items` (create/edit/delete); the seeded admin role carries those permissions by default.
**Operators** should request inventory operations through `/api/item-requests` forms.

## Coverage model

Supported request types:

- `INBOUND`: New incoming stock. `targetBranchId` is optional; if omitted, execution falls back to the requester'"'"'s branch. Resulting items are assigned to the target branch'"'"'s "Inbound" department unless `targetDepartmentId` is explicitly provided.
- `MODIFICATION`: Updates metadata or quantity of existing items.
- `TRANSFER`: Movement of items.
    - **Inter-Branch**: Moving items to a different branch. Targets a `Branch`. Items land in the "Inbound" department of the destination.
    - **Intra-Branch**: Moving items between departments in the *same* branch. Targets a `Department` (e.g., Storage -> IT).
- `DISINCORPORATION`: Decommission/removal of items from inventory.
- `ADJUSTMENT`: Signed quantity correction for auditing.

Each request can include **multiple entries**, allowing bulk operations in one workflow.

## Lifecycle

`DRAFT -> SUBMITTED -> (APPROVED | REJECTED) -> EXECUTED`

- **DRAFT**: operator creates/edits
- **SUBMITTED**: ready for admin review
- **APPROVED**: accepted by admin
- **REJECTED**: returned with reason
- **EXECUTED**: approved changes applied to real inventory
- **CANCELLED**: reserved for future cancellation flow

## Authorization split

### Direct item handling

- `/api/items` create/update/delete require:
  - corresponding `create_item` / `edit_item` / `delete_item` permissions

### Request workflow permissions

- `create_item_request`
- `get_item_request`
- `edit_item_request`
- `delete_item_request`
- `submit_item_request`
- `review_item_request`
- `execute_item_request`

Default seeded roles:

- **admin**: all permissions, can review/execute
- **operator**: can create/edit/submit requests and read supporting entities

## API endpoints

- `GET /api/item-requests`
- `GET /api/item-requests/{id}`
- `POST /api/item-requests`
- `PUT /api/item-requests/{id}`
- `POST /api/item-requests/{id}/submit`
- `POST /api/item-requests/{id}/review` (`decision`: `approve` maps to `APPROVED`; any other value is treated as `REJECTED`)
- `POST /api/item-requests/{id}/execute`

## Execution semantics by type

- **INBOUND**: creates new `Item` records from entry data.
- **MODIFICATION**: updates existing items.
- **TRANSFER**: reassigns item department, logs link/unlink relation events.
- **DISINCORPORATION**: subtracts quantity; deletes item if remaining quantity is 0 or below.
- **ADJUSTMENT**: applies signed delta to quantity; deletes item if resulting quantity is 0 or below.

## Auditability

- Request records are audited through JaVers commits.
- Executed inventory changes are also audited (create/update/delete/link/unlink).
- Changelog query remains available via:
  - `/api/audit-logs/{entityName}/{id}`

## Example request payload

```json
{
  "requestType": "INBOUND",
  "title": "Q2 replenishment",
  "justification": "Supplier shipment arrived",
  "entries": [
    {
      "itemId": null,
      "requestedItemName": "Lab gloves",
      "requestedQuantity": 120,
      "requestedUnit": "UND",
      "requestedCategoryId": 1,
      "sourceDepartmentId": null,
      "targetDepartmentId": 2,
      "observations": "Batch L-2026-04",
      "characteristicsJson": "{\"size\":\"M\"}"
    }
  ]
}
```
