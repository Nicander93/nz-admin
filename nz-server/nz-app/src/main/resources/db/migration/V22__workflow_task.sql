-- 流程任务中心：当前待办、历史已办、逐用户抄送和任务菜单权限。
CREATE TABLE IF NOT EXISTS flow_task (
    task_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    definition_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    assignee VARCHAR(100) NOT NULL,
    assignee_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_task_tenant_instance UNIQUE (tenant_id, instance_id)
);

CREATE INDEX IF NOT EXISTS idx_flow_task_tenant_assignee_created
    ON flow_task (tenant_id, assignee, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_flow_task_tenant_user_created
    ON flow_task (tenant_id, assignee_user_id, create_time DESC);

CREATE TABLE IF NOT EXISTS flow_history_task (
    history_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    task_id BIGINT NOT NULL,
    definition_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    node_id VARCHAR(100) NOT NULL,
    node_name VARCHAR(100) NOT NULL,
    assignee VARCHAR(100) NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(100),
    action VARCHAR(20) NOT NULL,
    target_node_id VARCHAR(100),
    target_node_name VARCHAR(100),
    target_assignee VARCHAR(100),
    comment VARCHAR(500),
    task_create_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_flow_history_task_action CHECK (
        action IN ('APPROVE', 'REJECT', 'TRANSFER', 'CANCEL', 'TERMINATE')
    )
);

CREATE INDEX IF NOT EXISTS idx_flow_history_task_tenant_operator_created
    ON flow_history_task (tenant_id, operator_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_flow_history_task_tenant_instance_created
    ON flow_history_task (tenant_id, instance_id, create_time, history_id);

CREATE TABLE IF NOT EXISTS flow_task_copy (
    copy_id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    task_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_name VARCHAR(100),
    comment VARCHAR(500),
    read_status SMALLINT NOT NULL DEFAULT 0,
    read_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_flow_task_copy_tenant_task_receiver UNIQUE (tenant_id, task_id, receiver_id),
    CONSTRAINT chk_flow_task_copy_read_status CHECK (read_status IN (0, 1))
);

CREATE INDEX IF NOT EXISTS idx_flow_task_copy_tenant_receiver_read
    ON flow_task_copy (tenant_id, receiver_id, read_status, create_time DESC);

INSERT INTO flow_task (
    tenant_id, definition_id, instance_id, node_id, node_name,
    assignee, assignee_user_id, create_time, update_time
)
SELECT instance.tenant_id, instance.definition_id, instance.instance_id,
       instance.current_node_id, instance.current_node_name, instance.current_assignee,
       CASE
           WHEN instance.current_assignee = 'initiator' THEN instance.initiator_id
           WHEN instance.current_assignee ~ '^user:[0-9]+$'
               THEN SUBSTRING(instance.current_assignee FROM 6)::BIGINT
           ELSE NULL
       END,
       instance.create_time, instance.update_time
FROM flow_instance instance
WHERE instance.status = 'RUNNING'
  AND instance.current_assignee IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM flow_task task
      WHERE task.tenant_id = instance.tenant_id
        AND task.instance_id = instance.instance_id
  );

INSERT INTO sys_menu (parent_id, name, path, component, icon, sort, type, perm, visible, status)
SELECT parent.id, '流程任务', 'task', 'workflow/task/index', 'Finished', 4,
       'C', 'workflow:task:list', 0, 0
FROM sys_menu parent
WHERE parent.parent_id = 0 AND parent.path = '/workflow'
  AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE perm = 'workflow:task:list');

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, button.name, button.sort, 'F', button.perm, 0, 0
FROM sys_menu menu
CROSS JOIN (
    VALUES
        ('查询', 1, 'workflow:task:query'),
        ('办理', 2, 'workflow:task:action'),
        ('转办', 3, 'workflow:task:transfer'),
        ('抄送', 4, 'workflow:task:copy'),
        ('已读', 5, 'workflow:task:read')
) AS button(name, sort, perm)
WHERE menu.perm = 'workflow:task:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu child
    WHERE child.parent_id = menu.id AND child.perm = button.perm
  );

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT role.tenant_id, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm IN (
    'workflow:task:list', 'workflow:task:query', 'workflow:task:action',
    'workflow:task:transfer', 'workflow:task:copy', 'workflow:task:read'
)
WHERE role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT package.id, menu.id
FROM sys_tenant_package package
CROSS JOIN sys_menu menu
WHERE menu.perm IN (
    'workflow:task:list', 'workflow:task:query', 'workflow:task:action',
    'workflow:task:transfer', 'workflow:task:copy', 'workflow:task:read'
)
ON CONFLICT (package_id, menu_id) DO NOTHING;
