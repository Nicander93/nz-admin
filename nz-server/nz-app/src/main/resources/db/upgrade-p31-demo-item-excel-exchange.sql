-- 示例条目 Excel 导入与导出权限。
INSERT INTO sys_menu(parent_id,name,sort,type,perm,visible,status)
SELECT m.id,b.name,b.sort,'F',b.perm,0,0 FROM sys_menu m
CROSS JOIN (VALUES('导入',5,'demo:item:import'),('导出',6,'demo:item:export')) b(name,sort,perm)
WHERE m.perm='demo:item:list'
AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perm=b.perm);
INSERT INTO sys_role_menu(tenant_id,role_id,menu_id)
SELECT r.tenant_id,r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key='admin' AND m.perm IN ('demo:item:import','demo:item:export')
ON CONFLICT(role_id,menu_id) DO NOTHING;
INSERT INTO sys_tenant_package_menu(package_id,menu_id)
SELECT p.id,m.id FROM sys_tenant_package p CROSS JOIN sys_menu m
WHERE m.perm IN ('demo:item:import','demo:item:export')
ON CONFLICT(package_id,menu_id) DO NOTHING;
