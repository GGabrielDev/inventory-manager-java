CREATE TABLE item_requests (
    id BIGSERIAL PRIMARY KEY,
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    justification TEXT NOT NULL,
    requested_by_user_id BIGINT NOT NULL REFERENCES users(id),
    reviewed_by_user_id BIGINT REFERENCES users(id),
    executed_by_user_id BIGINT REFERENCES users(id),
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    executed_at TIMESTAMPTZ,
    review_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE item_request_entries (
    id BIGSERIAL PRIMARY KEY,
    item_request_id BIGINT NOT NULL REFERENCES item_requests(id) ON DELETE CASCADE,
    item_id BIGINT REFERENCES items(id),
    requested_item_name VARCHAR(255),
    requested_quantity INTEGER,
    requested_unit VARCHAR(30),
    requested_category_id BIGINT REFERENCES categories(id),
    source_department_id BIGINT REFERENCES departments(id),
    target_department_id BIGINT REFERENCES departments(id),
    observations TEXT,
    characteristics_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
