-- 官方设计器坐标、业务绑定与可靠回调。存量 legacy 表不变。
ALTER TABLE flow_definition ADD COLUMN business_type VARCHAR(64);
ALTER TABLE flow_node ALTER COLUMN coordinate TYPE VARCHAR(1000);
ALTER TABLE flow_skip ALTER COLUMN coordinate TYPE VARCHAR(1000);

CREATE TABLE nz_workflow_business (
    tenant_id VARCHAR(40) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    instance_id BIGINT,
    event_sequence BIGINT NOT NULL DEFAULT 0,
    flow_status VARCHAR(20) NOT NULL,
    PRIMARY KEY (tenant_id,business_type,business_id)
);
CREATE UNIQUE INDEX idx_workflow_business_instance ON nz_workflow_business(tenant_id,instance_id);
CREATE TABLE nz_workflow_event (
    id BIGINT PRIMARY KEY,
    tenant_id VARCHAR(40) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    instance_id BIGINT NOT NULL,
    event_sequence BIGINT NOT NULL,
    flow_status VARCHAR(20) NOT NULL,
    node_type SMALLINT NOT NULL,
    actor VARCHAR(80) NOT NULL,
    operation VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt TIMESTAMP NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    last_error VARCHAR(500),
    business_snapshot TEXT NOT NULL,
    UNIQUE(tenant_id,instance_id,event_sequence)
);
CREATE INDEX idx_workflow_event_retry ON nz_workflow_event(delivered,next_attempt);
CREATE TABLE demo_leave (
    id VARCHAR(32) PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    flow_code VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    instance_id VARCHAR(80),
    last_sequence BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL,
    CHECK(end_date>=start_date)
);
CREATE INDEX idx_demo_leave_applicant ON demo_leave(tenant_id,applicant_id,create_time);
UPDATE sys_menu SET name='流程中心' WHERE perm='workflow:engine:query';
INSERT INTO sys_menu(parent_id,name,path,component,icon,sort,type,perm,visible,status)
SELECT id,'请假申请','leave','demo/leave/index','Calendar',10,'C','demo:leave:query',0,0
FROM sys_menu p WHERE p.parent_id=0 AND p.path='/demo'
AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perm='demo:leave:query');
INSERT INTO sys_menu(parent_id,name,sort,type,perm,visible,status)
SELECT m.id,b.name,b.sort,'F',b.perm,0,0 FROM sys_menu m
CROSS JOIN (VALUES('保存申请',1,'demo:leave:save'),('提交申请',2,'demo:leave:submit')) b(name,sort,perm)
WHERE m.perm='demo:leave:query' AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perm=b.perm);
INSERT INTO sys_role_menu(tenant_id,role_id,menu_id)
SELECT r.tenant_id,r.id,m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key='admin' AND m.perm LIKE 'demo:leave:%' ON CONFLICT(role_id,menu_id) DO NOTHING;
INSERT INTO sys_tenant_package_menu(package_id,menu_id)
SELECT p.id,m.id FROM sys_tenant_package p CROSS JOIN sys_menu m
WHERE m.perm LIKE 'demo:leave:%' ON CONFLICT(package_id,menu_id) DO NOTHING;
