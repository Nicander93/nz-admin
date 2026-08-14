-- 补齐在线用户页面与强制退出权限，清理不再使用的查询按钮。
UPDATE sys_menu
SET component = 'system/online/index',
    visible = 0,
    status = 0
WHERE id = 2300 OR perm = 'system:online:list';

UPDATE sys_menu
SET name = '强制退出',
    perm = 'system:online:force',
    visible = 0,
    status = 0
WHERE parent_id IN (SELECT id FROM sys_menu WHERE perm = 'system:online:list')
  AND perm = 'system:online:remove';

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, '强制退出', 2311, 'F', 'system:online:force', 0, 0
FROM sys_menu menu
WHERE menu.perm = 'system:online:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu button
    WHERE button.parent_id = menu.id AND button.perm = 'system:online:force'
  );

DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT id FROM sys_menu
    WHERE parent_id IN (SELECT id FROM sys_menu WHERE perm = 'system:online:list')
      AND perm = 'system:online:query'
);

DELETE FROM sys_menu
WHERE parent_id IN (SELECT id FROM sys_menu WHERE perm = 'system:online:list')
  AND perm = 'system:online:query';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN ('system:online:list', 'system:online:force')
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
