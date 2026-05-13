CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    dept_id     BIGINT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL,
    nickname    VARCHAR(64),
    email       VARCHAR(128),
    phone       VARCHAR(20),
    status      SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role (
     id          BIGSERIAL PRIMARY KEY,
     name        VARCHAR(64)  NOT NULL,
     role_key    VARCHAR(64)  NOT NULL UNIQUE,
     sort        INT DEFAULT 0,
     status      SMALLINT DEFAULT 0,
     remark      VARCHAR(500),
     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS data_scope INTEGER NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS sys_dept (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT DEFAULT 0,
    name        VARCHAR(64)  NOT NULL,
    path        VARCHAR(200),
    component   VARCHAR(255),
    icon        VARCHAR(64),
    sort        INT DEFAULT 0,
    type        CHAR(1),          -- M=目录 C=菜单 F=按钮
    perm        VARCHAR(128),     -- 权限标识，如 system:user:list
    visible     SMALLINT DEFAULT 0, -- 0=显示 1=隐藏
    status      SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    type        VARCHAR(128) NOT NULL UNIQUE,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id          BIGSERIAL PRIMARY KEY,
    dict_type   VARCHAR(128) NOT NULL,
    label       VARCHAR(128) NOT NULL,
    value       VARCHAR(128) NOT NULL,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id     BIGINT NOT NULL,
    role_id     BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id     BIGINT NOT NULL,
    menu_id     BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(128) NOT NULL,
    business_type  SMALLINT,
    oper_content   VARCHAR(255),
    method         VARCHAR(255),
    request_method VARCHAR(16),
    oper_name      VARCHAR(64),
    oper_url       VARCHAR(255),
    oper_ip        VARCHAR(128),
    user_agent     VARCHAR(500),
    oper_param     TEXT,
    json_result    TEXT,
    status         SMALLINT DEFAULT 0,
    error_msg      TEXT,
    oper_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_login_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(64),
    ip          VARCHAR(128),
    status      SMALLINT DEFAULT 0,
    msg         VARCHAR(255),
    login_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_post (
    id          BIGSERIAL PRIMARY KEY,
    post_code   VARCHAR(64)  NOT NULL UNIQUE,
    post_name   VARCHAR(64)  NOT NULL,
    sort        INT DEFAULT 0,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user_post (
    user_id     BIGINT NOT NULL,
    post_id     BIGINT NOT NULL,
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE IF NOT EXISTS sys_config (
    id          BIGSERIAL PRIMARY KEY,
    config_name VARCHAR(128) NOT NULL,
    config_key  VARCHAR(128) NOT NULL UNIQUE,
    config_value VARCHAR(500),
    config_type SMALLINT DEFAULT 2,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_notice (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(128) NOT NULL,
    content     TEXT,
    type        SMALLINT DEFAULT 1,
    status      SMALLINT DEFAULT 0,
    remark      VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_job (
    id              BIGSERIAL PRIMARY KEY,
    job_name        VARCHAR(64)  NOT NULL,
    job_group       VARCHAR(64)  DEFAULT 'DEFAULT',
    invoke_target   VARCHAR(500) NOT NULL,
    cron_expression VARCHAR(64)  NOT NULL,
    misfire_policy  SMALLINT DEFAULT 1,
    concurrent      SMALLINT DEFAULT 1,
    status          SMALLINT DEFAULT 0,
    remark          VARCHAR(500),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_job_log (
    id          BIGSERIAL PRIMARY KEY,
    job_id      BIGINT NOT NULL,
    job_name    VARCHAR(64),
    job_group   VARCHAR(64),
    invoke_target VARCHAR(500),
    job_message VARCHAR(500),
    status      SMALLINT DEFAULT 0,
    exception_info TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_file (
    id          BIGSERIAL PRIMARY KEY,
    file_name   VARCHAR(128) NOT NULL,
    original_name VARCHAR(128),
    file_suffix VARCHAR(32),
    file_size   BIGINT,
    file_path   VARCHAR(255),
    url         VARCHAR(255),
    storage_type SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- P0 seed data. Safe to rerun on PostgreSQL.
INSERT INTO sys_dept (id, parent_id, name, sort, status)
SELECT 1, 0, '总公司', 0, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE parent_id = 0 AND name = '总公司');

INSERT INTO sys_user (id, dept_id, username, password, nickname, status)
VALUES (1, 1, 'admin', '$2a$10$9U.FFaB9V1A4mjaD01fFAuE/Dle0Q3fvrrRqb9vQaFdggBNjL/LCS', '管理员', 0)
ON CONFLICT (username) DO NOTHING;

INSERT INTO sys_role (id, name, role_key, sort, status, remark, data_scope)
VALUES (1, '超级管理员', 'admin', 0, 0, '系统内置角色，拥有全部权限', 1)
ON CONFLICT (role_key) DO NOTHING;

INSERT INTO sys_post (id, post_code, post_name, sort, status, remark) VALUES
(1, 'ceo', '总经理', 1, 0, '系统内置岗位'),
(2, 'dev', '开发工程师', 2, 0, '系统内置岗位')
ON CONFLICT (post_code) DO NOTHING;

INSERT INTO sys_menu (id, parent_id, name, path, component, icon, sort, type, perm, visible, status) VALUES
(900, 0, '工作台', 'workbench', 'workbench/index', 'House', -10, 'C', 'system:workbench:view', 0, 0),
(1000, 0, '系统管理', '/system', NULL, 'Setting', 0, 'M', NULL, 0, 0),
(1100, 1000, '用户管理', 'user', 'system/user/index', 'User', 1, 'C', 'system:user:list', 0, 0),
(1200, 1000, '角色管理', 'role', 'system/role/index', 'UserFilled', 2, 'C', 'system:role:list', 0, 0),
(1300, 1000, '菜单管理', 'menu', 'system/menu/index', 'Menu', 3, 'C', 'system:menu:list', 0, 0),
(1400, 1000, '部门管理', 'dept', 'system/dept/index', 'OfficeBuilding', 4, 'C', 'system:dept:list', 0, 0),
(1500, 1000, '岗位管理', 'post', 'system/post/index', 'User', 5, 'C', 'system:post:list', 0, 0),
(1600, 1000, '字典管理', 'dict', 'system/dict/index', 'Collection', 6, 'C', 'system:dict:list', 0, 0),
(1700, 1000, '参数管理', 'config', 'system/config/index', 'Tools', 7, 'C', 'system:config:list', 0, 0),
(1800, 1000, '通知公告', 'notice', 'system/notice/index', 'Bell', 8, 'C', 'system:notice:list', 0, 0),
(1900, 1000, '文件管理', 'file', 'system/file/index', 'Folder', 9, 'C', 'system:file:list', 0, 0),
(2000, 0, '系统监控', '/monitor', NULL, 'Monitor', 20, 'M', NULL, 1, 0),
(2100, 2000, '操作日志', 'oper-log', NULL, 'Document', 1, 'C', 'system:operlog:list', 1, 0),
(2200, 2000, '登录日志', 'login-log', NULL, 'Tickets', 2, 'C', 'system:loginlog:list', 1, 0),
(2300, 2000, '在线用户', 'online', NULL, 'Connection', 3, 'C', 'system:online:list', 1, 0),
(2400, 2000, '定时任务', 'job', NULL, 'Timer', 4, 'C', 'system:job:list', 1, 0),
(2450, 2000, '运行状态', 'state', 'system/monitor/index', 'Cpu', 5, 'C', 'system:monitor:query', 0, 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_menu (parent_id, name, sort, type, perm, visible, status)
SELECT menu_id, button_name, sort, 'F', perm, 0, 0
FROM (
    VALUES
    (1100, '查询', 1110, 'system:user:query'), (1100, '新增', 1111, 'system:user:add'), (1100, '修改', 1112, 'system:user:edit'), (1100, '删除', 1113, 'system:user:remove'), (1100, '重置密码', 1114, 'system:user:resetPwd'),
    (1200, '查询', 1210, 'system:role:query'), (1200, '新增', 1211, 'system:role:add'), (1200, '修改', 1212, 'system:role:edit'), (1200, '删除', 1213, 'system:role:remove'),
    (1300, '查询', 1310, 'system:menu:query'), (1300, '新增', 1311, 'system:menu:add'), (1300, '修改', 1312, 'system:menu:edit'), (1300, '删除', 1313, 'system:menu:remove'),
    (1400, '查询', 1410, 'system:dept:query'), (1400, '新增', 1411, 'system:dept:add'), (1400, '修改', 1412, 'system:dept:edit'), (1400, '删除', 1413, 'system:dept:remove'),
    (1500, '查询', 1510, 'system:post:query'), (1500, '新增', 1511, 'system:post:add'), (1500, '修改', 1512, 'system:post:edit'), (1500, '删除', 1513, 'system:post:remove'),
    (1600, '查询', 1610, 'system:dict:query'), (1600, '新增', 1611, 'system:dict:add'), (1600, '修改', 1612, 'system:dict:edit'), (1600, '删除', 1613, 'system:dict:remove'),
    (1700, '查询', 1710, 'system:config:query'), (1700, '新增', 1711, 'system:config:add'), (1700, '修改', 1712, 'system:config:edit'), (1700, '删除', 1713, 'system:config:remove'),
    (1800, '查询', 1810, 'system:notice:query'), (1800, '新增', 1811, 'system:notice:add'), (1800, '修改', 1812, 'system:notice:edit'), (1800, '删除', 1813, 'system:notice:remove'),
    (1900, '查询', 1910, 'system:file:query'), (1900, '删除', 1911, 'system:file:remove'),
    (2100, '查询', 2110, 'system:operlog:query'), (2100, '删除', 2111, 'system:operlog:remove'),
    (2200, '查询', 2210, 'system:loginlog:query'), (2200, '删除', 2211, 'system:loginlog:remove'),
    (2300, '查询', 2310, 'system:online:query'), (2300, '删除', 2311, 'system:online:remove'),
    (2400, '查询', 2410, 'system:job:query'), (2400, '新增', 2411, 'system:job:add'), (2400, '修改', 2412, 'system:job:edit'), (2400, '删除', 2413, 'system:job:remove'),
    (2450, '查询', 2451, 'system:monitor:query')
) AS buttons(menu_id, button_name, sort, perm)
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE sys_menu.parent_id = buttons.menu_id AND sys_menu.perm = buttons.perm);

INSERT INTO sys_dict_type (id, name, type, status, remark) VALUES
(1, '系统状态', 'sys_normal_disable', 0, '系统通用状态'),
(2, '是否状态', 'sys_yes_no', 0, '系统通用是否'),
(3, '通知类型', 'sys_notice_type', 0, '通知公告类型')
ON CONFLICT (type) DO NOTHING;

INSERT INTO sys_dict_data (dict_type, label, value, sort, status)
SELECT dict_type, label, value, sort, 0
FROM (
    VALUES
    ('sys_normal_disable', '正常', '0', 1),
    ('sys_normal_disable', '停用', '1', 2),
    ('sys_yes_no', '是', 'Y', 1),
    ('sys_yes_no', '否', 'N', 2),
    ('sys_notice_type', '通知', '1', 1),
    ('sys_notice_type', '公告', '2', 2)
) AS data(dict_type, label, value, sort)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_dict_data
    WHERE sys_dict_data.dict_type = data.dict_type AND sys_dict_data.value = data.value
);

INSERT INTO sys_config (id, config_name, config_key, config_value, config_type, status, remark) VALUES
(1, '主框架页签', 'sys.index.tabs', 'true', 1, 0, '是否开启页签风格'),
(2, '默认密码', 'sys.user.initPassword', '123456', 1, 0, '新用户默认密码')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO sys_notice (title, content, type, status, remark)
SELECT '欢迎使用 nz-admin', '系统初始化完成，可使用 admin / admin123 登录。', 2, 0, '系统内置公告'
WHERE NOT EXISTS (SELECT 1 FROM sys_notice WHERE title = '欢迎使用 nz-admin');

INSERT INTO sys_job (job_name, job_group, invoke_target, cron_expression, concurrent, status, remark)
SELECT '系统演示任务', 'SYSTEM', 'demoTask.run', '0 0/30 * * * ?', 1, 1, '初始化占位任务，默认暂停'
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE job_name = '系统演示任务');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_key = 'admin'
ON CONFLICT (role_id, menu_id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u CROSS JOIN sys_role r
WHERE u.username = 'admin' AND r.role_key = 'admin'
ON CONFLICT (user_id, role_id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('sys_dept', 'id'), COALESCE((SELECT MAX(id) FROM sys_dept), 1), true);
SELECT setval(pg_get_serial_sequence('sys_user', 'id'), COALESCE((SELECT MAX(id) FROM sys_user), 1), true);
SELECT setval(pg_get_serial_sequence('sys_role', 'id'), COALESCE((SELECT MAX(id) FROM sys_role), 1), true);
SELECT setval(pg_get_serial_sequence('sys_post', 'id'), COALESCE((SELECT MAX(id) FROM sys_post), 1), true);
SELECT setval(pg_get_serial_sequence('sys_menu', 'id'), COALESCE((SELECT MAX(id) FROM sys_menu), 1), true);
SELECT setval(pg_get_serial_sequence('sys_dict_type', 'id'), COALESCE((SELECT MAX(id) FROM sys_dict_type), 1), true);
SELECT setval(pg_get_serial_sequence('sys_dict_data', 'id'), COALESCE((SELECT MAX(id) FROM sys_dict_data), 1), true);
SELECT setval(pg_get_serial_sequence('sys_config', 'id'), COALESCE((SELECT MAX(id) FROM sys_config), 1), true);
