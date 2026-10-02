CREATE TABLE sys_role_dept (
    tenant_id BIGINT NOT NULL DEFAULT 1,
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    PRIMARY KEY (tenant_id, role_id, dept_id)
);
UPDATE sys_role SET data_scope = CASE WHEN role_key = 'admin' THEN 1 ELSE 5 END WHERE data_scope IS NULL;
ALTER TABLE sys_role ALTER COLUMN data_scope SET DEFAULT 5;
ALTER TABLE sys_role ADD CONSTRAINT sys_role_data_scope_check CHECK (data_scope BETWEEN 1 AND 5);
ALTER TABLE demo_item ADD COLUMN owner_id BIGINT;
