-- Warm-Flow 1.8.9 coexistence foundation.
-- Keep the current runtime tables and data intact while freeing Warm-Flow's canonical names.
ALTER TABLE flow_definition RENAME TO nz_flow_definition_legacy;
ALTER TABLE nz_flow_definition_legacy
    RENAME CONSTRAINT flow_definition_pkey TO nz_flow_definition_legacy_pkey;

ALTER TABLE flow_instance RENAME TO nz_flow_instance_legacy;
ALTER TABLE nz_flow_instance_legacy
    RENAME CONSTRAINT flow_instance_pkey TO nz_flow_instance_legacy_pkey;

ALTER TABLE flow_task RENAME TO nz_flow_task_legacy;
ALTER TABLE nz_flow_task_legacy
    RENAME CONSTRAINT flow_task_pkey TO nz_flow_task_legacy_pkey;

CREATE TABLE flow_definition (
    id BIGINT PRIMARY KEY,
    flow_code VARCHAR(40) NOT NULL,
    flow_name VARCHAR(100) NOT NULL,
    category VARCHAR(100),
    version VARCHAR(20) NOT NULL,
    is_publish SMALLINT NOT NULL DEFAULT 0,
    form_custom CHAR(1) DEFAULT 'N',
    form_path VARCHAR(100),
    activity_status SMALLINT NOT NULL DEFAULT 1,
    listener_type VARCHAR(100),
    listener_path VARCHAR(400),
    ext VARCHAR(500),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE flow_node (
    id BIGINT PRIMARY KEY,
    node_type SMALLINT NOT NULL,
    definition_id BIGINT NOT NULL,
    node_code VARCHAR(100) NOT NULL,
    node_name VARCHAR(100),
    permission_flag VARCHAR(200),
    node_ratio NUMERIC(6, 3),
    coordinate VARCHAR(100),
    any_node_skip VARCHAR(100),
    listener_type VARCHAR(100),
    listener_path VARCHAR(400),
    handler_type VARCHAR(100),
    handler_path VARCHAR(400),
    form_custom CHAR(1) DEFAULT 'N',
    form_path VARCHAR(100),
    version VARCHAR(20) NOT NULL,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    ext TEXT,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE flow_skip (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    now_node_code VARCHAR(100) NOT NULL,
    now_node_type SMALLINT,
    next_node_code VARCHAR(100) NOT NULL,
    next_node_type SMALLINT,
    skip_name VARCHAR(100),
    skip_type VARCHAR(40),
    skip_condition VARCHAR(200),
    coordinate VARCHAR(100),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE flow_instance (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    business_id VARCHAR(40) NOT NULL,
    node_type SMALLINT NOT NULL,
    node_code VARCHAR(40) NOT NULL,
    node_name VARCHAR(100),
    variable TEXT,
    flow_status VARCHAR(20) NOT NULL,
    activity_status SMALLINT NOT NULL DEFAULT 1,
    def_json TEXT,
    create_by VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    ext VARCHAR(500),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE flow_task (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    node_code VARCHAR(100) NOT NULL,
    node_name VARCHAR(100),
    node_type SMALLINT NOT NULL,
    form_custom CHAR(1) DEFAULT 'N',
    form_path VARCHAR(100),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE flow_his_task (
    id BIGINT PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    node_code VARCHAR(100),
    node_name VARCHAR(100),
    node_type SMALLINT,
    target_node_code VARCHAR(200),
    target_node_name VARCHAR(200),
    approver VARCHAR(40),
    cooperate_type SMALLINT NOT NULL DEFAULT 0,
    collaborator VARCHAR(40),
    skip_type VARCHAR(10),
    flow_status VARCHAR(20) NOT NULL,
    form_custom CHAR(1) DEFAULT 'N',
    form_path VARCHAR(100),
    ext VARCHAR(500),
    message VARCHAR(500),
    variable TEXT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE TABLE flow_user (
    id BIGINT PRIMARY KEY,
    type CHAR(1) NOT NULL,
    processed_by VARCHAR(80),
    associated BIGINT NOT NULL,
    create_time TIMESTAMP,
    create_by VARCHAR(80),
    update_time TIMESTAMP,
    del_flag CHAR(1) DEFAULT '0',
    tenant_id VARCHAR(40)
);

CREATE INDEX idx_warm_flow_definition_tenant_code
    ON flow_definition (tenant_id, flow_code, is_publish);
CREATE INDEX idx_warm_flow_node_definition ON flow_node (definition_id);
CREATE INDEX idx_warm_flow_skip_definition_node ON flow_skip (definition_id, now_node_code);
CREATE INDEX idx_warm_flow_instance_tenant_business ON flow_instance (tenant_id, business_id);
CREATE INDEX idx_warm_flow_task_instance ON flow_task (instance_id);
CREATE INDEX idx_warm_flow_his_task_instance_created ON flow_his_task (instance_id, create_time);
CREATE INDEX idx_warm_flow_user_processed_type ON flow_user (processed_by, type);
CREATE INDEX idx_warm_flow_user_associated ON flow_user (associated);
