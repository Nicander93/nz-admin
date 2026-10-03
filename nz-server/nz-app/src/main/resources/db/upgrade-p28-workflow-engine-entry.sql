-- 新引擎使用独立权限与入口，旧流程菜单和在途实例不变。
INSERT INTO sys_menu (parent_id,name,path,component,icon,sort,type,perm,visible,status)
SELECT id,'新引擎工作台','engine','workflow/engine/index','Connection',5,'C','workflow:engine:query',0,0
FROM sys_menu parent WHERE parent.parent_id=0 AND parent.path='/workflow'
AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perm='workflow:engine:query');
INSERT INTO sys_menu (parent_id,name,sort,type,perm,visible,status)
SELECT menu.id,button.name,button.sort,'F',button.perm,0,0 FROM sys_menu menu
CROSS JOIN (VALUES ('设计发布',1,'workflow:engine:design'),('发起',2,'workflow:engine:start'),('办理',3,'workflow:engine:action')) button(name,sort,perm)
WHERE menu.perm='workflow:engine:query' AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perm=button.perm);
INSERT INTO sys_role_menu(tenant_id,role_id,menu_id)
SELECT role.tenant_id,role.id,menu.id FROM sys_role role CROSS JOIN sys_menu menu
WHERE role.role_key='admin' AND menu.perm LIKE 'workflow:engine:%' ON CONFLICT(role_id,menu_id) DO NOTHING;
INSERT INTO sys_tenant_package_menu(package_id,menu_id)
SELECT package.id,menu.id FROM sys_tenant_package package CROSS JOIN sys_menu menu
WHERE menu.perm LIKE 'workflow:engine:%' ON CONFLICT(package_id,menu_id) DO NOTHING;
