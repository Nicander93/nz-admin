-- 客户端管理 v0：业务表、菜单和按钮权限。
CREATE TABLE IF NOT EXISTS sys_client (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(64) NOT NULL UNIQUE,
    client_name VARCHAR(128) NOT NULL,
    login_type VARCHAR(32) NOT NULL DEFAULT 'account',
    token_timeout INTEGER NOT NULL DEFAULT 7200,
    status SMALLINT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status) VALUES
    (1800, 1000, '客户端管理', 'client', 'system/client/index', 'Connection', 8, 'C', 'system:client:list', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, path = EXCLUDED.path, component = EXCLUDED.component,
    icon = EXCLUDED.icon, sort = EXCLUDED.sort, perm = EXCLUDED.perm;

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status) VALUES
    (1810, 1800, '查询', 1810, 'F', 'system:client:query', 0, 0),
    (1811, 1800, '新增', 1811, 'F', 'system:client:add', 0, 0),
    (1812, 1800, '修改', 1812, 'F', 'system:client:edit', 0, 0),
    (1813, 1800, '删除', 1813, 'F', 'system:client:remove', 0, 0)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, perm = EXCLUDED.perm;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu.id FROM sys_menu menu WHERE menu.id IN (1800, 1810, 1811, 1812, 1813)
ON CONFLICT DO NOTHING;
