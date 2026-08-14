CREATE TABLE IF NOT EXISTS demo_item (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    sort INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_demo_item_status CHECK (status IN (0, 1)),
    CONSTRAINT chk_demo_item_sort CHECK (sort BETWEEN 0 AND 999)
);

CREATE INDEX IF NOT EXISTS idx_demo_item_category ON demo_item (category);
CREATE INDEX IF NOT EXISTS idx_demo_item_status_sort ON demo_item (status, sort);

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)
VALUES (3000, 0, '示例模块', '/demo', NULL, 'Opportunity', 90, 'M', NULL, 0, 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name, path = EXCLUDED.path, component = EXCLUDED.component,
    icon = EXCLUDED.icon, sort = EXCLUDED.sort, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status)
VALUES (3010, 3000, '示例条目', 'item', 'demo/item/index', 'List', 1, 'C', 'demo:item:list', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, path = EXCLUDED.path,
    component = EXCLUDED.component, icon = EXCLUDED.icon, sort = EXCLUDED.sort,
    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_menu (id, parent_id, name, sort, type, perm, visible, status) VALUES
    (3011, 3010, '查询', 3011, 'F', 'demo:item:query', 0, 0),
    (3012, 3010, '新增', 3012, 'F', 'demo:item:add', 0, 0),
    (3013, 3010, '修改', 3013, 'F', 'demo:item:edit', 0, 0),
    (3014, 3010, '删除', 3014, 'F', 'demo:item:remove', 0, 0)
ON CONFLICT (id) DO UPDATE SET
    parent_id = EXCLUDED.parent_id, name = EXCLUDED.name, sort = EXCLUDED.sort,
    perm = EXCLUDED.perm, visible = EXCLUDED.visible, status = EXCLUDED.status;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.id IN (3000, 3010, 3011, 3012, 3013, 3014)
WHERE role.role_key = 'admin'
ON CONFLICT DO NOTHING;
