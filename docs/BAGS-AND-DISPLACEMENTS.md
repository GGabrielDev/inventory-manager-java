# Bags and Displacements

## Overview
The "Bag" system allows for the creation of standardized kits (logical groupings of items). The "Displacement" system allows for the temporary removal of items from their assigned location without the formal paperwork of a stock transfer.

---

## Bag Management

### Entities
- **Bag**: A physical container with a unique `barcode`.
- **BagItem**: A record of an item that is **intended** to be inside a specific bag.

### Live Audit Workflow
Personnel can perform a real-time check of a bag's contents:
1. Scan the Bag's barcode.
2. The system retrieves the list of `BagItems` (Expected).
3. The system subtracts any `Displacements` (Items known to be missing for a valid reason).
4. The personnel verifies the remaining items.
5. Any discrepancies are flagged as "Anomalies".

---

## Universal Displacements (Borrowing)

A **Displacement** is a record of a temporary relocation of **any item** in the inventory.

### Use Cases
- **Borrowed Tool**: Someone takes a drill from Storage for 3 days.
- **Temporary Assignment**: A laptop is assigned to a visitor for a week.
- **Missing from Bag**: An item is removed from a bag for maintenance.

### Lifecycle
1. **Creation**: Record `borrowerName`, `reason`, and `expectedReturnDate`.
2. **Active**: The item is considered "Displaced" in auditing screens.
3. **Resolution**: When the item is returned, the Displacement is marked as `RESOLVED`, and it no longer affects audits.

---

## Benefits
- **Zero Paperwork**: No formal `ItemRequest` needed for short-term moves.
- **Audit Accuracy**: Personnel know exactly *why* an item is missing during a scan.
- **Accountability**: Provides a trail of who currently has company property.
