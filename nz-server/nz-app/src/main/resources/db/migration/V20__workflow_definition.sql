-- 流程定义与发布：版本链、模型 JSON、运行状态、菜单与权限。
CREATE TABLE IF NOT EXISTS flow_definition (
    definition_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    flow_code VARCHAR(40) NOT NULL,
    flow_name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    publish_status SMALLINT NOT NULL DEFAULT 0,
    activity_status SMALLINT NOT NULL DEFAULT 1,
    form_path VARCHAR(200),
    model_json TEXT NOT NULL,
    remark VARCHAR(500),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_flow_definition_version CHECK (version_no > 0),
    CONSTRAINT chk_flow_definition_publish_status CHECK (publish_status IN (0, 1, 9)),
    CONSTRAINT chk_flow_definition_activity_status CHECK (activity_status IN (0, 1)),
    CONSTRAINT uk_flow_definition_tenant_code_version UNIQUE (tenant_id, flow_code, version_no)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_flow_definition_tenant_code_draft
    ON flow_definition (tenant_id, flow_code) WHERE publish_status = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_flow_definition_tenant_code_published
    ON flow_definition (tenant_id, flow_code) WHERE publish_status = 1;
CREATE INDEX IF NOT EXISTS idx_flow_definition_tenant_category_status
    ON flow_definition (tenant_id, category_id, publish_status, create_time DESC);

INSERT INTO flow_definition (
    tenant_id, flow_code, flow_name, category_id, version_no,
    publish_status, activity_status, model_json, remark
)
SELECT category.tenant_id, 'leave_apply', '请假审批', category.category_id, 1,
       0, 1,
       '{"nodes":[{"id":"start","type":"start","name":"开始"},{"id":"approve","type":"task","name":"部门负责人审批","assignee":"role:manager"},{"id":"end","type":"end","name":"结束"}],"edges":[{"source":"start","target":"approve"},{"source":"approve","target":"end"}]}',
       '内置草稿，可在发布前修改办理人'
FROM flow_category category
WHERE category.tenant_id = 1
  AND category.parent_id = 0
  AND category.category_name = 'OA审批'
  AND NOT EXISTS (
      SELECT 1 FROM flow_definition definition
      WHERE definition.tenant_id = category.tenant_id
        AND definition.flow_code = 'leave_apply'
  );

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '流程定义', 'definition', 'workflow/definition/index', 'DocumentChecked', 2,
       'C', 'workflow:definition:list', 0, 0
FROM sys_menu parent
WHERE parent.parent_id = 0 AND parent.path = '/workflow'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'workflow:definition:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查询', 1, 'workflow:definition:query'),
        ('新增', 2, 'workflow:definition:add'),
        ('修改', 3, 'workflow:definition:edit'),
        ('删除', 4, 'workflow:definition:remove'),
        ('发布', 5, 'workflow:definition:publish'),
        ('启停', 6, 'workflow:definition:active'),
        ('复制', 7, 'workflow:definition:copy'),
        ('导入', 8, 'workflow:definition:import'),
        ('导出', 9, 'workflow:definition:export')
) AS button(name, sort, perm)
WHERE menu.perm = 'workflow:definition:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT role.tenant_id, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    'workflow:definition:list', 'workflow:definition:query', 'workflow:definition:add',
    'workflow:definition:edit', 'workflow:definition:remove', 'workflow:definition:publish',
    'workflow:definition:active', 'workflow:definition:copy', 'workflow:definition:import',
    'workflow:definition:export'
)
WHERE role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT package.id, menu.id
FROM sys_tenant_package package
CROSS JOIN sys_menu menu
WHERE menu.perm IN (
    'workflow:definition:list', 'workflow:definition:query', 'workflow:definition:add',
    'workflow:definition:edit', 'workflow:definition:remove', 'workflow:definition:publish',
    'workflow:definition:active', 'workflow:definition:copy', 'workflow:definition:import',
    'workflow:definition:export'
)
ON CONFLICT (package_id, menu_id) DO NOTHING;

