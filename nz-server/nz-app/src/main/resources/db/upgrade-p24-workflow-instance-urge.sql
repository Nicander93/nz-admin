-- 流程实例催办：记录催办轨迹并授予发起人/管理员催办权限。
INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, '催办', 8, 'F', 'workflow:instance:urge', 0, 0
FROM sys_menu menu
WHERE menu.perm = 'workflow:instance:list'
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu child
      WHERE child.parent_id = menu.id AND child.perm = 'workflow:instance:urge'
  );

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT role.tenant_id, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm = 'workflow:instance:urge'
WHERE role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT package.id, menu.id
FROM sys_tenant_package package
CROSS JOIN sys_menu menu
WHERE menu.perm = 'workflow:instance:urge'
ON CONFLICT (package_id, menu_id) DO NOTHING;
