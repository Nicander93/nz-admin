-- 用户联系方式字段加密：扩大密文列长度并注册最小权限点。
ALTER TABLE sys_user
    ALTER COLUMN email TYPE VARCHAR(512),
    ALTER COLUMN phone TYPE VARCHAR(512);

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status) VALUES
    (1115, 1100, '查看明文联系方式', 1115, 'F', 'system:user:contact:plain', 0, 0),
    (1116, 1100, '联系方式重加密', 1116, 'F', 'system:user:contact:encrypt', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    name = EXCLUDED.name,
    sort = EXCLUDED.sort,
    type = EXCLUDED.type,
    perm = EXCLUDED.perm,
    visible = EXCLUDED.visible,
    status = EXCLUDED.status;

-- 明文查看权限涉及敏感数据，升级时不自动授予任何角色或租户套餐。
