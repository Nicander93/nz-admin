-- 租户内站内消息、逐用户已读状态和菜单权限。
CREATE TABLE IF NOT EXISTS sys_message (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    sender_id BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    category VARCHAR(32) NOT NULL,
    type VARCHAR(32) NOT NULL DEFAULT 'message',
    source VARCHAR(64) NOT NULL DEFAULT 'backend',
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    content TEXT NOT NULL,
    data_json TEXT,
    path VARCHAR(500),
    read_status SMALLINT NOT NULL DEFAULT 0,
    read_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_sys_message_category
        CHECK (category IN ('system', 'notice', 'workflow')),
    CONSTRAINT chk_sys_message_read_status CHECK (read_status IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_sys_message_tenant_user_read_time
    ON sys_message (tenant_id, user_id, read_status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_sys_message_tenant_sender_time
    ON sys_message (tenant_id, sender_id, create_time DESC);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '消息中心', 'message', 'system/message/index', 'Bell', 17,
       'C', 'system:message:list', 0, 0
FROM sys_menu parent
WHERE parent.path = '/system'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'system:message:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查看详情', 1, 'system:message:query'),
        ('标记已读', 2, 'system:message:read'),
        ('删除消息', 3, 'system:message:remove'),
        ('发送消息', 4, 'system:message:send')
) AS button(name, sort, perm)
WHERE menu.perm = 'system:message:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    'system:message:list', 'system:message:query', 'system:message:read',
    'system:message:remove', 'system:message:send'
)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
