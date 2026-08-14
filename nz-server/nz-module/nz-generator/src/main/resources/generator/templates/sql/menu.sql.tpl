-- @@FEATURE_DOC@@菜单与按钮权限。执行前确认 parent_id 指向正确的目录菜单。
INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT @@PARENT_MENU_ID@@, '@@SQL_FEATURE@@', '@@BUSINESS@@', '@@MODULE@@/@@BUSINESS@@/index',
       'List', 1, 'C', '@@PERMISSION_PREFIX@@:list', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = '@@PERMISSION_PREFIX@@:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查询', 1, '@@PERMISSION_PREFIX@@:query'),
        ('新增', 2, '@@PERMISSION_PREFIX@@:add'),
        ('修改', 3, '@@PERMISSION_PREFIX@@:edit'),
        ('删除', 4, '@@PERMISSION_PREFIX@@:remove')
) AS button(name, sort, perm)
WHERE menu.perm = '@@PERMISSION_PREFIX@@:list'
  AND NOT EXISTS (SELECT 1 FROM sys_menu existing WHERE existing.perm = button.perm);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    '@@PERMISSION_PREFIX@@:list',
    '@@PERMISSION_PREFIX@@:query',
    '@@PERMISSION_PREFIX@@:add',
    '@@PERMISSION_PREFIX@@:edit',
    '@@PERMISSION_PREFIX@@:remove'
)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
