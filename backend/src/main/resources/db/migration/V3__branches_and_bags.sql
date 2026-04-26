-- V3: Branches, Bags, and Displacements

-- 1. Create Branches Table
CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    state_id BIGINT NOT NULL REFERENCES states(id) ON DELETE RESTRICT,
    municipality_id BIGINT NOT NULL REFERENCES municipalities(id) ON DELETE RESTRICT,
    parish_id BIGINT NOT NULL REFERENCES parishes(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. Setup Default Branch for existing data
-- Note: This assumes at least one state/municipality/parish exists from seeding.
-- If not, these columns will be temporarily nullable and fixed in Phase 1 seed.
INSERT INTO branches (name, address, state_id, municipality_id, parish_id)
SELECT 'Main Branch', 'Default Address', s.id, m.id, p.id
FROM states s, municipalities m, parishes p
WHERE m.state_id = s.id AND p.municipality_id = m.id
LIMIT 1;

-- 3. Update Departments
ALTER TABLE departments ADD COLUMN branch_id BIGINT REFERENCES branches(id) ON DELETE CASCADE;
UPDATE departments SET branch_id = (SELECT id FROM branches LIMIT 1);
ALTER TABLE departments ALTER COLUMN branch_id SET NOT NULL;
ALTER TABLE departments DROP CONSTRAINT departments_name_key;
ALTER TABLE departments ADD CONSTRAINT departments_name_branch_unique UNIQUE (name, branch_id);

-- 4. Update Users
ALTER TABLE users ADD COLUMN branch_id BIGINT REFERENCES branches(id) ON DELETE SET NULL;
UPDATE users SET branch_id = (SELECT id FROM branches LIMIT 1);

-- 5. Update Items
ALTER TABLE items ADD COLUMN branch_id BIGINT REFERENCES branches(id) ON DELETE RESTRICT;
UPDATE items SET branch_id = (SELECT id FROM branches LIMIT 1);
ALTER TABLE items ALTER COLUMN branch_id SET NOT NULL;

-- 6. Update Item Requests
ALTER TABLE item_requests ADD COLUMN target_branch_id BIGINT REFERENCES branches(id) ON DELETE RESTRICT;
-- By default, existing requests targeted the main branch
UPDATE item_requests SET target_branch_id = (SELECT id FROM branches LIMIT 1);

-- 7. Create Bags Table
CREATE TABLE bags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    barcode VARCHAR(255) NOT NULL UNIQUE,
    branch_id BIGINT NOT NULL REFERENCES branches(id) ON DELETE CASCADE,
    assigned_department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 8. Create Bag Items Table (Expected contents)
CREATE TABLE bag_items (
    id BIGSERIAL PRIMARY KEY,
    bag_id BIGINT NOT NULL REFERENCES bags(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    expected_quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (bag_id, item_id)
);

-- 9. Create Displacements Table (Universal temporary borrowing)
CREATE TABLE displacements (
    id BIGSERIAL PRIMARY KEY,
    bag_id BIGINT REFERENCES bags(id) ON DELETE SET NULL, -- Optional: if item came from a specific bag
    item_id BIGINT NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    reason TEXT NOT NULL,
    borrower_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, RESOLVED
    removed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expected_return_date TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
