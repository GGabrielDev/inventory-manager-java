# Locations and Branches

## Physical Hierarchy

The system enforces a strict physical hierarchy to ensure every asset is accurately located:

`State` > `Municipality` > `Parish` > `Branch` > `Department`

### 1. Administrative Divisions
- **States**: The highest level geographic division.
- **Municipalities**: Belong to a State.
- **Parishes**: Belong to a Municipality.

### 2. Physical Nodes (Branches)
- **Branches**: Represent physical offices, warehouses, or stores.
- A Branch is linked to a specific Parish and has a unique name and street address.
- **Implicit Knowledge**: Items belong to a Branch. Because a Branch knows its State/Municipality/Parish, every item in the system implicitly knows its full geographic location.

### 3. Logical Divisions (Departments)
- **Departments**: The specific office or room inside a Branch (e.g., "IT Office", "Storage Room B").
- **Required Defaults**: Every branch should have:
    - `Inbound`: Where new cargo lands during an Inter-Branch transfer.
    - `Storage`: General holding area for items not currently assigned to a functional department.

---

## Operations

### Assignments
- **Users**: Operators are assigned to a primary Branch. This filters their view to only show assets and bags they are responsible for.
- **Items**: Every item must be assigned to a Branch and a Department.
- **Bags**: Every bag belongs to a Branch.

### Inter-Branch Logic
When moving items between branches, the source operator only needs to select the target **Branch**. The system handles the internal routing by placing the items in the destination's `Inbound` department. The destination operator is then responsible for moving them to a permanent department (e.g., `Storage`) via an Intra-Branch transfer.
