-- V4: Paranoid / Soft-Delete
-- Adds deleted_at column to every entity table so that rows are never
-- physically removed; application logic sets deleted_at instead of issuing
-- a DELETE statement.  Queries automatically filter WHERE deleted_at IS NULL.

ALTER TABLE permissions          ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE roles                ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE users                ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE states               ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE municipalities       ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE parishes             ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE branches             ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE departments          ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE categories           ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE items                ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE bags                 ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE bag_items            ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE displacements        ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE item_requests        ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
ALTER TABLE item_request_entries ADD COLUMN deleted_at TIMESTAMPTZ DEFAULT NULL;
