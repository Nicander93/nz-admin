-- 注册 SSE/WebSocket 实时通信控制台及按钮权限。
UPDATE sys_menu
SET component = 'system/realtime/index',
    icon = 'Promotion',
    visible = 0,
    status = 0
WHERE perm = 'system:realtime:view';

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '实时通信', 'realtime', 'system/realtime/index',
       'Promotion', 4, 'C', 'system:realtime:view', 0, 0
FROM sys_menu parent
WHERE parent.path = '/monitor'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu menu WHERE menu.perm = 'system:realtime:view'
  );

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, '发送测试消息', 1, 'F', 'system:realtime:send', 0, 0
FROM sys_menu menu
WHERE menu.perm = 'system:realtime:view'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu button
    WHERE button.parent_id = menu.id AND button.perm = 'system:realtime:send'
  );

UPDATE sys_menu
SET sort = 5
WHERE path = 'job'
  AND parent_id IN (SELECT id FROM sys_menu WHERE path = '/monitor');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN ('system:realtime:view', 'system:realtime:send')
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
