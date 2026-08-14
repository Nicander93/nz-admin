-- 工作流分类：租户数据、默认分类、菜单与权限。
CREATE TABLE IF NOT EXISTS flow_category (
    category_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    parent_id BIGINT NOT NULL DEFAULT 0,
    ancestors VARCHAR(500) NOT NULL DEFAULT '0',
    category_name VARCHAR(30) NOT NULL,
    order_num INTEGER NOT NULL DEFAULT 0,
    built_in SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_flow_category_parent CHECK (parent_id >= 0),
    CONSTRAINT chk_flow_category_built_in CHECK (built_in IN (0, 1)),
    CONSTRAINT uk_flow_category_tenant_parent_name UNIQUE (tenant_id, parent_id, category_name)
);

CREATE INDEX IF NOT EXISTS idx_flow_category_tenant_parent_order
    ON flow_category (tenant_id, parent_id, order_num, category_id);

INSERT INTO flow_category (
    tenant_id, parent_id, ancestors, category_name, order_num, built_in
)
SELECT 1, 0, '0', 'OA审批', 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM flow_category
    WHERE tenant_id = 1 AND parent_id = 0 AND category_name = 'OA审批'
);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, visible, status)
SELECT 0, '工作流程', '/workflow', NULL, 'Guide', 30, 'M', 0, 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = '/workflow'
);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '流程分类', 'category', 'workflow/category/index', 'FolderOpened', 1,
       'C', 'workflow:category:list', 0, 0
FROM sys_menu parent
WHERE parent.parent_id = 0 AND parent.path = '/workflow'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'workflow:category:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查询', 1, 'workflow:category:query'),
        ('新增', 2, 'workflow:category:add'),
        ('修改', 3, 'workflow:category:edit'),
        ('删除', 4, 'workflow:category:remove'),
        ('导出', 5, 'workflow:category:export')
) AS button(name, sort, perm)
WHERE menu.perm = 'workflow:category:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT role.tenant_id, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON (
    menu.path = '/workflow'
    OR menu.perm IN (
        'workflow:category:list', 'workflow:category:query', 'workflow:category:add',
        'workflow:category:edit', 'workflow:category:remove', 'workflow:category:export'
    )
)
WHERE role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT package.id, menu.id
FROM sys_tenant_package package
CROSS JOIN sys_menu menu
WHERE menu.path = '/workflow'
   OR menu.perm IN (
       'workflow:category:list', 'workflow:category:query', 'workflow:category:add',
       'workflow:category:edit', 'workflow:category:remove', 'workflow:category:export'
   )
ON CONFLICT (package_id, menu_id) DO NOTHING;

