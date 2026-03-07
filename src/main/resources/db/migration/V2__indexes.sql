CREATE INDEX IF NOT EXISTS idx_resources_owner_parent_path ON resources(owner_id, parent_path);
CREATE INDEX IF NOT EXISTS idx_resources_owner_name ON resources(owner_id, name);
