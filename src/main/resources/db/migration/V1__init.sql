CREATE TABLE IF NOT EXISTS users (
id SERIAL PRIMARY KEY,
name VARCHAR (20) UNIQUE NOT NULL,
password VARCHAR (256) NOT NULL,
role VARCHAR (20) NOT NULL
);

CREATE TABLE IF NOT EXISTS resources(
id SERIAL PRIMARY KEY,
name VARCHAR (40) NOT NULL,
size INT,
type VARCHAR (20) NOT NULL,
owner_id INT NOT NULL,
parent_id INT,


CONSTRAINT fk_resources_owner
FOREIGN KEY (owner_id) REFERENCES users(id)
ON DELETE CASCADE,

CONSTRAINT fk_resources_parent
FOREIGN KEY (parent_id) REFERENCES resources(id)
ON DELETE CASCADE,

CONSTRAINT chk_resources_size_by_type
        CHECK (
            (type = 'FILE' AND size IS NOT NULL AND size >= 0)
            OR
            (type = 'DIRECTORY' AND size IS NULL)
        )
);

);

