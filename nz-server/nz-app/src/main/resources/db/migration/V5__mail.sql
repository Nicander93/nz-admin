-- Optional mail capability: menu and permission for existing installations.
INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)
VALUES (1960, 1000, '邮件测试', 'mail', 'system/mail/index', 'Message', 11, 'C', 'system:mail:test', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, path = EXCLUDED.path, component = EXCLUDED.component,
    icon = EXCLUDED.icon, sort = EXCLUDED.sort, perm = EXCLUDED.perm;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT id, 1960 FROM sys_role WHERE role_key = 'admin'
ON CONFLICT DO NOTHING;