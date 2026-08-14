-- 多租户：管理表、默认租户、行级隔离字段与菜单权限。
CREATE TABLE IF NOT EXISTS sys_tenant_package (
    id BIGSERIAL PRIMARY KEY,
    package_name VARCHAR(128) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_tenant_package_menu (
    package_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (package_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(32) NOT NULL UNIQUE,
    tenant_name VARCHAR(128) NOT NULL,
    contact_user VARCHAR(64),
    contact_phone VARCHAR(20),
    package_id BIGINT NOT NULL,
    expire_time TIMESTAMP,
    account_count INTEGER NOT NULL DEFAULT 100,
    status SMALLINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_tenant_package (id, package_name, status, remark)
VALUES (1, '默认套餐', 0, '系统内置套餐，默认包含全部菜单')
ON CONFLICT (id) DO UPDATE SET package_name = EXCLUDED.package_name;

INSERT INTO sys_tenant (
    id, tenant_code, tenant_name, contact_user, package_id, account_count, status, remark
)
VALUES (1, 'default', '默认租户', '管理员', 1, 1000, 0, '承接升级前的全部数据')
ON CONFLICT (id) DO UPDATE SET tenant_code = EXCLUDED.tenant_code;

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_dict_type ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_dict_data ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_user_role ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_role_menu ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_login_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_post ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_user_post ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_notice ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_job ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_job_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_file ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE sys_file_config ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE demo_item ADD COLUMN IF NOT EXISTS tenant_id BIGINT NOT NULL DEFAULT 1;

ALTER TABLE sys_user DROP CONSTRAINT IF EXISTS sys_user_username_key;
ALTER TABLE sys_role DROP CONSTRAINT IF EXISTS sys_role_role_key_key;
ALTER TABLE sys_dict_type DROP CONSTRAINT IF EXISTS sys_dict_type_type_key;
ALTER TABLE sys_post DROP CONSTRAINT IF EXISTS sys_post_post_code_key;
ALTER TABLE sys_config DROP CONSTRAINT IF EXISTS sys_config_config_key_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_tenant_username ON sys_user (tenant_id, username);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_tenant_role_key ON sys_role (tenant_id, role_key);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_type_tenant_type ON sys_dict_type (tenant_id, type);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_post_tenant_code ON sys_post (tenant_id, post_code);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_config_tenant_key ON sys_config (tenant_id, config_key);

DROP INDEX IF EXISTS uk_sys_file_config_active;
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_config_tenant_active
    ON sys_file_config (tenant_id) WHERE status = 0;

CREATE INDEX IF NOT EXISTS idx_sys_user_tenant_id ON sys_user (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_role_tenant_id ON sys_role (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_dept_tenant_id ON sys_dept (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_tenant_id ON sys_oper_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_login_log_tenant_id ON sys_login_log (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_job_tenant_id ON sys_job (tenant_id);
CREATE INDEX IF NOT EXISTS idx_sys_file_tenant_id ON sys_file (tenant_id);
CREATE INDEX IF NOT EXISTS idx_demo_item_tenant_id ON demo_item (tenant_id);

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status) VALUES
    (1980, 1000, '租户套餐', 'tenant-package', 'system/tenant-package/index', 'Tickets', 13, 'C', 'system:tenantpackage:list', 0, 0),
    (1990, 1000, '租户管理', 'tenant', 'system/tenant/index', 'OfficeBuilding', 14, 'C', 'system:tenant:list', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, path = EXCLUDED.path,
    component = EXCLUDED.component, icon = EXCLUDED.icon, sort = EXCLUDED.sort,
    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status) VALUES
    (1981, 1980, '查询', 1981, 'F', 'system:tenantpackage:query', 0, 0),
    (1982, 1980, '新增', 1982, 'F', 'system:tenantpackage:add', 0, 0),
    (1983, 1980, '修改', 1983, 'F', 'system:tenantpackage:edit', 0, 0),
    (1984, 1980, '删除', 1984, 'F', 'system:tenantpackage:remove', 0, 0),
    (1991, 1990, '查询', 1991, 'F', 'system:tenant:query', 0, 0),
    (1992, 1990, '新增', 1992, 'F', 'system:tenant:add', 0, 0),
    (1993, 1990, '修改', 1993, 'F', 'system:tenant:edit', 0, 0),
    (1994, 1990, '停用', 1994, 'F', 'system:tenant:remove', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, sort = EXCLUDED.sort,
    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT 1, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id BETWEEN 1980 AND 1994
WHERE role.tenant_id = 1 AND role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT 1, id FROM sys_menu
ON CONFLICT (package_id, menu_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('sys_tenant_package', 'id'),
              COALESCE((SELECT MAX(id) FROM sys_tenant_package), 1), true);
SELECT setval(pg_get_serial_sequence('sys_tenant', 'id'),
              COALESCE((SELECT MAX(id) FROM sys_tenant), 1), true);
