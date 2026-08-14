INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)
VALUES (4000, 0, '开发工具', '/tool', NULL, 'Tools', 80, 'M', NULL, 0, 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, path = EXCLUDED.path, component = EXCLUDED.component,
    icon = EXCLUDED.icon, sort = EXCLUDED.sort, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)
VALUES (4010, 4000, '代码生成', 'generator', 'generator/index', 'DocumentCopy', 1, 'C',
        'generator:table:list', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, path = EXCLUDED.path,
    component = EXCLUDED.component, icon = EXCLUDED.icon, sort = EXCLUDED.sort,
    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status) VALUES
    (4011, 4010, '查询字段', 1, 'F', 'generator:table:query', 0, 0),
    (4012, 4010, '预览代码', 2, 'F', 'generator:table:preview', 0, 0),
    (4013, 4010, '下载代码', 3, 'F', 'generator:table:download', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, sort = EXCLUDED.sort,
    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (4000, 4010, 4011, 4012, 4013)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
