-- 流程实例与运行轨迹：定义快照、变量、当前节点、完整事件历史和菜单权限。
CREATE TABLE IF NOT EXISTS flow_instance (
    instance_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    definition_id BIGINT NOT NULL,
    business_key VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    flow_code VARCHAR(40) NOT NULL,
    flow_name VARCHAR(100) NOT NULL,
    version_no INTEGER NOT NULL,
    initiator_id BIGINT NOT NULL,
    current_node_id VARCHAR(100) NOT NULL,
    current_node_name VARCHAR(100) NOT NULL,
    current_node_type VARCHAR(20) NOT NULL,
    current_assignee VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    activity_status SMALLINT NOT NULL DEFAULT 1,
    variables_json TEXT NOT NULL DEFAULT '{}',
    model_json TEXT NOT NULL,
    end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_instance_tenant_business UNIQUE (tenant_id, business_key),
    CONSTRAINT chk_flow_instance_status CHECK (
        status IN ('RUNNING', 'COMPLETED', 'REJECTED', 'CANCELED', 'TERMINATED')
    ),
    CONSTRAINT chk_flow_instance_activity_status CHECK (activity_status IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_flow_instance_tenant_status_created
    ON flow_instance (tenant_id, status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_flow_instance_tenant_initiator_created
    ON flow_instance (tenant_id, initiator_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_flow_instance_definition
    ON flow_instance (tenant_id, definition_id);

CREATE TABLE IF NOT EXISTS flow_instance_event (
    event_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    instance_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    from_node_id VARCHAR(100),
    from_node_name VARCHAR(100),
    to_node_id VARCHAR(100),
    to_node_name VARCHAR(100),
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(100),
    comment VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_flow_instance_event_instance_created
    ON flow_instance_event (tenant_id, instance_id, create_time, event_id);

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '流程实例', 'instance', 'workflow/instance/index', 'Connection', 3,
       'C', 'workflow:instance:list', 0, 0
FROM sys_menu parent
WHERE parent.parent_id = 0 AND parent.path = '/workflow'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'workflow:instance:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查询', 1, 'workflow:instance:query'),
        ('发起', 2, 'workflow:instance:start'),
        ('办理', 3, 'workflow:instance:action'),
        ('撤回', 4, 'workflow:instance:cancel'),
        ('终止', 5, 'workflow:instance:terminate'),
        ('启停', 6, 'workflow:instance:active'),
        ('删除', 7, 'workflow:instance:remove')
) AS button(name, sort, perm)
WHERE menu.perm = 'workflow:instance:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT role.tenant_id, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    'workflow:instance:list', 'workflow:instance:query', 'workflow:instance:start',
    'workflow:instance:action', 'workflow:instance:cancel', 'workflow:instance:terminate',
    'workflow:instance:active', 'workflow:instance:remove'
)
WHERE role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT package.id, menu.id
FROM sys_tenant_package package
CROSS JOIN sys_menu menu
WHERE menu.perm IN (
    'workflow:instance:list', 'workflow:instance:query', 'workflow:instance:start',
    'workflow:instance:action', 'workflow:instance:cancel', 'workflow:instance:terminate',
    'workflow:instance:active', 'workflow:instance:remove'
)
ON CONFLICT (package_id, menu_id) DO NOTHING;
