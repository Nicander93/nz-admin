-- 流程任务委派：保留原办理人，受托人完成后归还，并记录委派历史。
ALTER TABLE flow_task
    ADD COLUMN IF NOT EXISTS owner_assignee VARCHAR(100),
    ADD COLUMN IF NOT EXISTS owner_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS delegation_status SMALLINT NOT NULL DEFAULT 0;

UPDATE flow_task
SET delegation_status = 0
WHERE delegation_status IS NULL;

ALTER TABLE flow_task DROP CONSTRAINT IF EXISTS chk_flow_task_delegation_status;
ALTER TABLE flow_task
    ADD CONSTRAINT chk_flow_task_delegation_status CHECK (delegation_status IN (0, 1));

ALTER TABLE flow_history_task DROP CONSTRAINT IF EXISTS chk_flow_history_task_action;
ALTER TABLE flow_history_task
    ADD CONSTRAINT chk_flow_history_task_action CHECK (
        action IN ('APPROVE', 'REJECT', 'TRANSFER', 'DELEGATE', 'RESOLVE', 'CANCEL', 'TERMINATE')
    );

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu.id, '委派', 6, 'F', 'workflow:task:delegate', 0, 0
FROM sys_menu menu
WHERE menu.perm = 'workflow:task:list'
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu child
      WHERE child.parent_id = menu.id AND child.perm = 'workflow:task:delegate'
  );

INSERT INTO sys_role_menu (tenant_id, role_id, menu_id)
SELECT role.tenant_id, role.id, menu.id
FROM sys_role role
JOIN sys_menu menu ON menu.perm = 'workflow:task:delegate'
WHERE role.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_tenant_package_menu (package_id, menu_id)
SELECT package.id, menu.id
FROM sys_tenant_package package
CROSS JOIN sys_menu menu
WHERE menu.perm = 'workflow:task:delegate'
ON CONFLICT (package_id, menu_id) DO NOTHING;
