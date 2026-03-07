CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(20) UNIQUE NOT NULL,
    password VARCHAR(256) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS resources (
    id SERIAL PRIMARY KEY,
    name VARCHAR(256) NOT NULL,
    size BIGINT,
    type VARCHAR(20) NOT NULL,
    owner_id INT NOT NULL,
    full_path VARCHAR(1024) NOT NULL,
    parent_path VARCHAR(1024) NOT NULL,
    CONSTRAINT fk_resources_owner
        FOREIGN KEY (owner_id) REFERENCES users(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_resources_size_by_type
        CHECK (
            (type = 'FILE' AND size IS NOT NULL AND size >= 0)
            OR
            (type = 'DIRECTORY' AND size IS NULL)
        ),
    CONSTRAINT uk_resources_owner_full_path UNIQUE (owner_id, full_path)
);

