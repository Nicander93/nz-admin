CREATE TABLE IF NOT EXISTS sys_file_config (
    id BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(100) NOT NULL,
    storage_type VARCHAR(16) NOT NULL,
    base_path VARCHAR(500),
    endpoint VARCHAR(500),
    access_key_id VARCHAR(200),
    access_key_secret VARCHAR(1000),
    bucket_name VARCHAR(200),
    domain VARCHAR(500),
    path_prefix VARCHAR(200),
    local_access_url_prefix VARCHAR(200),
    max_file_size_bytes BIGINT NOT NULL DEFAULT 104857600,
    status SMALLINT NOT NULL DEFAULT 1,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_file_config_storage_type CHECK (storage_type IN ('local', 'oss')),
    CONSTRAINT chk_file_config_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_file_config_max_size CHECK (max_file_size_bytes > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_file_config_active
    ON sys_file_config (status) WHERE status = 0;

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)
VALUES (1970, 1000, '文件配置', 'file-config', 'system/file-config/index', 'SetUp', 11, 'C',
        'system:fileconfig:list', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, path = EXCLUDED.path, component = EXCLUDED.component,
    icon = EXCLUDED.icon, sort = EXCLUDED.sort, perm = EXCLUDED.perm;

UPDATE sys_menu SET sort = 12 WHERE id = 1960 OR perm = 'system:mail:test';

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status) VALUES
    (1971, 1970, '查询', 1971, 'F', 'system:fileconfig:query', 0, 0),
    (1972, 1970, '新增', 1972, 'F', 'system:fileconfig:add', 0, 0),
    (1973, 1970, '修改', 1973, 'F', 'system:fileconfig:edit', 0, 0),
    (1974, 1970, '删除', 1974, 'F', 'system:fileconfig:remove', 0, 0)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, perm = EXCLUDED.perm;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (1970, 1971, 1972, 1973, 1974)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;