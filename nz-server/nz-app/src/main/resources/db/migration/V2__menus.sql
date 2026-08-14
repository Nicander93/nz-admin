-- P2：工作台与运行监控菜单（已有库升级时可单独执行；全新库已由 init.sql 包含）
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status) VALUES
(900, 0, '工作台', 'workbench', 'workbench/index', 'House', -10, 'C', 'system:workbench:view', 0, 0),
(2450, 2000, '运行状态', 'state', 'system/monitor/index', 'Cpu', 5, 'C', 'system:monitor:query', 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu_id, button_name, sort, 'F', perm, 0, 0
FROM (
    VALUES
    (2450, '查询', 2451, 'system:monitor:query')
) AS buttons(menu_id, button_name, sort, perm)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.parent_id = buttons.menu_id AND sys_menu.perm = buttons.perm);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (900, 2450)
WHERE r.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;
