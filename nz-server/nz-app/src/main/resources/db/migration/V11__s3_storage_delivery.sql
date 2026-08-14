-- 增加 S3/MinIO 文件存储配置，并注册连接测试权限。
ALTER TABLE sys_file_config
    ADD COLUMN IF NOT EXISTS region VARCHAR(100);

ALTER TABLE sys_file_config
    DROP CONSTRAINT IF EXISTS chk_file_config_storage_type;

ALTER TABLE sys_file_config
    ADD CONSTRAINT chk_file_config_storage_type
        CHECK (storage_type IN ('local', 'oss', 's3'));

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status)
VALUES (1975, 1970, '测试存储连接', 1975, 'F', 'system:fileconfig:test', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    sort = EXCLUDED.sort,
    type = EXCLUDED.type,
    perm = EXCLUDED.perm,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, 1975
FROM sys_role role
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
